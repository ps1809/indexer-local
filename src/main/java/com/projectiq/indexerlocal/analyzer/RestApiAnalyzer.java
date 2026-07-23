package com.projectiq.indexerlocal.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.projectiq.indexerlocal.model.RestApiEndpoint;
import com.projectiq.indexerlocal.model.RestApiParameter;
import com.projectiq.indexerlocal.model.RestApiStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Analyzes Java source files to discover and analyze REST API endpoints in Spring applications.
 * Detects @RestController, @Controller, HTTP mapping annotations, request/response metadata,
 * validation annotations, and security annotations.
 */
@Component
public class RestApiAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(RestApiAnalyzer.class);

    // Controller annotations
    private static final String ANNOTATION_REST_CONTROLLER = "RestController";
    private static final String ANNOTATION_CONTROLLER = "Controller";

    // HTTP method annotations
    private static final String ANNOTATION_REQUEST_MAPPING = "RequestMapping";
    private static final String ANNOTATION_GET_MAPPING = "GetMapping";
    private static final String ANNOTATION_POST_MAPPING = "PostMapping";
    private static final String ANNOTATION_PUT_MAPPING = "PutMapping";
    private static final String ANNOTATION_DELETE_MAPPING = "DeleteMapping";
    private static final String ANNOTATION_PATCH_MAPPING = "PatchMapping";

    // Request parameter annotations
    private static final String ANNOTATION_PATH_VARIABLE = "PathVariable";
    private static final String ANNOTATION_REQUEST_PARAM = "RequestParam";
    private static final String ANNOTATION_REQUEST_BODY = "RequestBody";
    private static final String ANNOTATION_REQUEST_HEADER = "RequestHeader";
    private static final String ANNOTATION_COOKIE_VALUE = "CookieValue";

    // Response annotations
    private static final String ANNOTATION_RESPONSE_BODY = "ResponseBody";
    private static final String ANNOTATION_RESPONSE_ENTITY = "ResponseEntity";

    // Validation annotations
    private static final String ANNOTATION_VALID = "Valid";
    private static final String ANNOTATION_VALIDATED = "Validated";

    // Security annotations
    private static final String ANNOTATION_PRE_AUTHORIZE = "PreAuthorize";
    private static final String ANNOTATION_POST_AUTHORIZE = "PostAuthorize";
    private static final String ANNOTATION_ROLES_ALLOWED = "RolesAllowed";
    private static final String ANNOTATION_SECURED = "Secured";

    private final JavaParser javaParser = new JavaParser();

    /**
     * Analyze all Java files in a project directory for REST APIs.
     */
    public List<RestApiEndpoint> analyze(String projectPath, String repositoryId) {
        logger.info("Starting REST API analysis for repository: {}", repositoryId);

        Path path = Path.of(projectPath);
        List<RestApiEndpoint> endpoints = new ArrayList<>();
        int totalFilesScanned = 0;

        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();

            logger.info("Found {} Java files to scan in {}", javaFiles.size(), projectPath);

            for (Path javaFile : javaFiles) {
                totalFilesScanned++;
                try {
                    List<RestApiEndpoint> fileEndpoints = analyzeJavaFile(javaFile, repositoryId);
                    endpoints.addAll(fileEndpoints);
                } catch (Exception e) {
                    logger.warn("Failed to analyze REST APIs in file {}: {}", javaFile.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to walk directory {}: {}", projectPath, e.getMessage());
            throw new RuntimeException("Failed to analyze REST APIs in " + projectPath, e);
        }

        logger.info("REST API analysis complete for repository {}. Found {} endpoints from {} files.",
                repositoryId, endpoints.size(), totalFilesScanned);

        return endpoints;
    }

    /**
     * Analyze a single Java file for REST API endpoints.
     */
    private List<RestApiEndpoint> analyzeJavaFile(Path filePath, String repositoryId) throws IOException {
        String content = Files.readString(filePath);

        CompilationUnit cu = javaParser.parse(content).getResult()
                .orElseThrow(() -> new IOException("Failed to parse: " + filePath));

        String packageName = cu.getPackageDeclaration()
                .map(pkg -> pkg.getName().asString())
                .orElse("");

        List<RestApiEndpoint> endpoints = new ArrayList<>();

        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration classDecl : classes) {
            List<String> classAnnotations = classDecl.getAnnotations()
                    .stream()
                    .map(AnnotationExpr::getNameAsString)
                    .toList();

            // Check if this is a REST controller
            if (isRestController(classAnnotations)) {
                String basePath = extractBasePath(classDecl);
                String controllerName = classDecl.getNameAsString();

                List<MethodDeclaration> methods = classDecl.findAll(MethodDeclaration.class);
                for (MethodDeclaration method : methods) {
                    List<String> methodAnnotations = method.getAnnotations()
                            .stream()
                            .map(AnnotationExpr::getNameAsString)
                            .toList();

                    // Check if this method has an HTTP mapping annotation
                    Optional<RestApiEndpoint> endpointOpt = extractEndpoint(method, classDecl, controllerName,
                            packageName, basePath, repositoryId, methodAnnotations);
                    endpointOpt.ifPresent(endpoints::add);
                }
            }
        }

        return endpoints;
    }

    /**
     * Check if a class is a REST controller.
     */
    private boolean isRestController(List<String> annotations) {
        for (String annotation : annotations) {
            if (ANNOTATION_REST_CONTROLLER.equalsIgnoreCase(annotation)) {
                return true;
            }
        }
        // Also detect @Controller with @ResponseBody or mapping annotations on methods
        boolean hasController = false;
        for (String annotation : annotations) {
            if (ANNOTATION_CONTROLLER.equalsIgnoreCase(annotation)) {
                hasController = true;
            }
        }
        return hasController;
    }

    /**
     * Extract the base path from @RequestMapping on the class.
     */
    private String extractBasePath(ClassOrInterfaceDeclaration classDecl) {
        Optional<AnnotationExpr> requestMapping = classDecl.getAnnotations()
                .stream()
                .filter(a -> ANNOTATION_REQUEST_MAPPING.equalsIgnoreCase(a.getNameAsString()))
                .findFirst();

        if (requestMapping.isPresent()) {
            return extractPathValue(requestMapping.get());
        }

        return "";
    }

    /**
     * Extract endpoint information from a method with HTTP mapping annotation.
     */
    private Optional<RestApiEndpoint> extractEndpoint(MethodDeclaration method, ClassOrInterfaceDeclaration classDecl,
                                                        String controllerName, String packageName, String basePath,
                                                        String repositoryId, List<String> methodAnnotations) {
        // Find the HTTP mapping annotation
        Optional<AnnotationExpr> httpMapping = method.getAnnotations()
                .stream()
                .filter(a -> isHttpMethodAnnotation(a.getNameAsString()))
                .findFirst();

        if (!httpMapping.isPresent()) {
            return Optional.empty();
        }

        RestApiEndpoint endpoint = new RestApiEndpoint();
        endpoint.setRepositoryId(repositoryId);
        endpoint.setControllerName(controllerName);
        endpoint.setControllerPackageName(packageName);
        endpoint.setBasepath(basePath);
        endpoint.setClassName(classDecl.getNameAsString());
        endpoint.setMethodName(method.getNameAsString());

        // Extract HTTP method and path
        String httpMethod = extractHttpMethod(httpMapping.get());
        String endpointPath = extractPathValue(httpMapping.get());
        endpoint.setHttpMethod(httpMethod.toUpperCase());
        endpoint.setEndpointPath(endpointPath);

        // Extract request metadata
        extractRequestMetadata(method, endpoint);

        // Extract response metadata
        extractResponseMetadata(method, endpoint);

        // Extract validation annotations
        extractValidation(method, endpoint);

        // Extract security annotations
        extractSecurityAnnotations(classDecl, method, endpoint);

        return Optional.of(endpoint);
    }

    /**
     * Check if an annotation is an HTTP method annotation.
     */
    private boolean isHttpMethodAnnotation(String annotationName) {
        return ANNOTATION_REQUEST_MAPPING.equalsIgnoreCase(annotationName) ||
               ANNOTATION_GET_MAPPING.equalsIgnoreCase(annotationName) ||
               ANNOTATION_POST_MAPPING.equalsIgnoreCase(annotationName) ||
               ANNOTATION_PUT_MAPPING.equalsIgnoreCase(annotationName) ||
               ANNOTATION_DELETE_MAPPING.equalsIgnoreCase(annotationName) ||
               ANNOTATION_PATCH_MAPPING.equalsIgnoreCase(annotationName);
    }

    /**
     * Extract the HTTP method type from a mapping annotation.
     */
    private String extractHttpMethod(AnnotationExpr annotation) {
        String name = annotation.getNameAsString();
        switch (name.toUpperCase()) {
            case "GETMAPPING":
                return "GET";
            case "POSTMAPPING":
                return "POST";
            case "PUTMAPPING":
                return "PUT";
            case "DELET_MAPPING":
                return "DELETE";
            case "PATCHMAPPING":
                return "PATCH";
            case "REQUESTMAPPING":
                // For @RequestMapping, check for the value attribute to determine method
                if (annotation instanceof SingleMemberAnnotationExpr) {
                    return "GET"; // Default to GET for @RequestMapping without method
                }
                return "GET";
            default:
                return "GET";
        }
    }

    /**
     * Extract path value from an annotation.
     */
    private String extractPathValue(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr) {
            Expression expr = ((SingleMemberAnnotationExpr) annotation).getMemberValue();
            return extractStringFromExpression(expr);
        } else if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normal = (NormalAnnotationExpr) annotation;
            for (com.github.javaparser.ast.expr.MemberValuePair pair : normal.getPairs()) {
                String key = pair.getName().asString();
                if ("value".equals(key) || "path".equals(key)) {
                    return extractStringFromExpression(pair.getValue());
                }
            }
        } else if (annotation instanceof MarkerAnnotationExpr) {
            // Simple marker annotation like @GetMapping without path
            return "";
        }
        return "";
    }

    /**
     * Extract a string value from an expression.
     */
    private String extractStringFromExpression(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        }
        return "";
    }

    /**
     * Extract request parameter metadata from method parameters.
     */
    private void extractRequestMetadata(MethodDeclaration method, RestApiEndpoint endpoint) {
        for (var param : method.getParameters()) {
            String paramName = param.getName().asString();
            String paramType = param.getType().asString();

            List<String> annotations = param.getAnnotations()
                    .stream()
                    .map(a -> a.getNameAsString())
                    .toList();

            for (String annotationName : annotations) {
                boolean required = true; // Default is required
                String defaultValue = null;

                // Check for default value
                if (param.toString().contains("default=")) {
                    // Extract default value from annotation string
                    int start = param.toString().indexOf("default=");
                    if (start > 0) {
                        String afterDefault = param.toString().substring(start + 8);
                        defaultValue = afterDefault.replaceAll("[\"']", "").replaceAll("[,)]", "").trim();
                    }
                }

                // Check for required=false
                for (var anno : param.getAnnotations()) {
                    if (anno instanceof NormalAnnotationExpr) {
                        NormalAnnotationExpr normal = (NormalAnnotationExpr) anno;
                for (var pair : normal.getPairs()) {
                            String annotationKeyName = pair.getName().asString();
                            if ("required".equals(annotationKeyName)) {
                                if (pair.getValue() instanceof BooleanLiteralExpr) {
                                    required = ((BooleanLiteralExpr) pair.getValue()).getValue();
                                }
                            }
                        }
                    }
                }

                RestApiParameter parameter = new RestApiParameter(paramName, paramType, required);
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    parameter.setDefaultValue(defaultValue);
                }

                switch (annotationName.toUpperCase()) {
                    case "PATHVARIABLE":
                        endpoint.getPathVariables().add(parameter);
                        break;
                    case "REQUESTPARAM":
                        endpoint.getRequestParams().add(parameter);
                        break;
                    case "REQUESTBODY":
                        endpoint.setRequestBody(parameter);
                        break;
                    case "REQUESTHEADER":
                        endpoint.getRequestHeaders().add(parameter);
                        break;
                    case "COOKIEVALUE":
                        endpoint.setCookieValue(parameter);
                        break;
                }
            }
        }
    }

    /**
     * Extract response metadata from the method.
     */
    private void extractResponseMetadata(MethodDeclaration method, RestApiEndpoint endpoint) {
        // Extract return type - use getType() for JavaParser 3.x
        Type returnType = method.getType();
        if (returnType != null) {
            String returnTypeName = returnType.asString();
            endpoint.setReturnType(returnTypeName);

            // Check for ResponseEntity usage
            if (returnTypeName.contains("ResponseEntity")) {
                endpoint.setResponseEntityUsed(true);
            }
        }

        // Check method and class annotations for consumes/produces
        List<AnnotationExpr> methodAnnotations = method.getAnnotations();
        List<String> methodAnnotationNames = new ArrayList<>();

        // Extract produces from @RequestMapping or specific mapping annotation
        for (AnnotationExpr anno : methodAnnotations) {
            String annoName = anno.getNameAsString();
            methodAnnotationNames.add(annoName);
            if (isHttpMethodAnnotation(annoName)) {
                extractProducesConsumes(anno, endpoint);
            }
        }

        // Also check class-level annotations via getParentNode()
        var parentNode = method.getParentNode();
        if (parentNode.isPresent() && parentNode.get() instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) parentNode.get();
            for (AnnotationExpr anno : classDecl.getAnnotations()) {
                String annoName = anno.getNameAsString();
                if (ANNOTATION_REQUEST_MAPPING.equalsIgnoreCase(annoName)) {
                    extractProducesConsumes(anno, endpoint);
                }
            }
        }

        // Check for @ResponseBody on the method
        if (methodAnnotationNames.contains(ANNOTATION_RESPONSE_BODY)) {
            endpoint.setResponseEntityUsed(true);
        }
    }

    /**
     * Extract consumes and produces from a mapping annotation.
     */
    private void extractProducesConsumes(AnnotationExpr annotation, RestApiEndpoint endpoint) {
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normal = (NormalAnnotationExpr) annotation;
            for (com.github.javaparser.ast.expr.MemberValuePair pair : normal.getPairs()) {
                String key = pair.getName().asString();
                Expression value = pair.getValue();

                if ("produces".equals(key)) {
                    endpoint.setProducesMediaType(extractFirstStringValue(value));
                } else if ("consumes".equals(key)) {
                    endpoint.setConsumesMediaType(extractFirstStringValue(value));
                }
            }
        }
    }

    /**
     * Extract the first string value from an expression.
     */
    private String extractFirstStringValue(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        } else if (expr instanceof ArrayInitializerExpr) {
            ArrayInitializerExpr array = (ArrayInitializerExpr) expr;
            if (!array.getValues().isEmpty()) {
                Expression first = array.getValues().get(0);
                if (first instanceof StringLiteralExpr) {
                    return ((StringLiteralExpr) first).getValue();
                }
            }
        }
        return "";
    }

    /**
     * Extract validation annotations from the method.
     */
    private void extractValidation(MethodDeclaration method, RestApiEndpoint endpoint) {
        for (var param : method.getParameters()) {
            for (var anno : param.getAnnotations()) {
                String annoName = anno.getNameAsString();
                if (ANNOTATION_VALID.equalsIgnoreCase(annoName)) {
                    endpoint.setValidAnnotation(true);
                }
            }
        }

        // Check for @Validated on the class
        var parent = method.getParentNode();
        if (parent.isPresent() && parent.get() instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) parent.get();
            for (var anno : classDecl.getAnnotations()) {
                if (ANNOTATION_VALIDATED.equalsIgnoreCase(anno.getNameAsString())) {
                    endpoint.setValidatedAnnotation(true);
                }
            }
        }
    }

    /**
     * Extract security annotations from the class and method.
     */
    private void extractSecurityAnnotations(ClassOrInterfaceDeclaration classDecl, MethodDeclaration method,
                                             RestApiEndpoint endpoint) {
        // Check method annotations
        for (var anno : method.getAnnotations()) {
            String annoName = anno.getNameAsString();
            switch (annoName.toUpperCase()) {
                case "PRE_AUTHORIZE":
                    endpoint.setPreAuthorize(true);
                    break;
                case "POST_AUTHORIZE":
                    endpoint.setPostAuthorize(true);
                    break;
                case "ROLES_ALLOWED":
                    endpoint.setRolesAllowed(true);
                    break;
                case "SECURED":
                    endpoint.setSecured(true);
                    break;
            }
        }

        // Check class annotations
        for (var anno : classDecl.getAnnotations()) {
            String annoName = anno.getNameAsString();
            switch (annoName.toUpperCase()) {
                case "PRE_AUTHORIZE":
                    endpoint.setPreAuthorize(true);
                    break;
                case "POST_AUTHORIZE":
                    endpoint.setPostAuthorize(true);
                    break;
                case "ROLES_ALLOWED":
                    endpoint.setRolesAllowed(true);
                    break;
                case "SECURED":
                    endpoint.setSecured(true);
                    break;
            }
        }
    }

    /**
     * Generate statistics from a list of REST API endpoints.
     */
    public RestApiStatistics generateStatistics(List<RestApiEndpoint> endpoints, String repositoryId) {
        RestApiStatistics stats = new RestApiStatistics();
        stats.setRepositoryId(repositoryId);

        // Count unique controllers
        Set<String> controllers = new HashSet<>();
        for (RestApiEndpoint endpoint : endpoints) {
            controllers.add(endpoint.getControllerName());
        }
        stats.setTotalRestControllers(controllers.size());

        // Total endpoints
        stats.setTotalEndpoints(endpoints.size());

        // Secure vs public endpoints
        int secureCount = 0;
        for (RestApiEndpoint endpoint : endpoints) {
            if (endpoint.isSecure()) {
                secureCount++;
            }
        }
        stats.setSecureEndpoints(secureCount);
        stats.setPublicEndpoints(endpoints.size() - secureCount);

        // Endpoints by HTTP method
        for (RestApiEndpoint endpoint : endpoints) {
            String method = endpoint.getHttpMethod();
            switch (method.toUpperCase()) {
                case "GET":
                    stats.setEndpointsByGetMapping(stats.getEndpointsByGetMapping() + 1);
                    break;
                case "POST":
                    stats.setEndpointsByPostMapping(stats.getEndpointsByPostMapping() + 1);
                    break;
                case "PUT":
                    stats.setEndpointsByPutMapping(stats.getEndpointsByPutMapping() + 1);
                    break;
                case "DELETE":
                    stats.setEndpointsByDeleteMapping(stats.getEndpointsByDeleteMapping() + 1);
                    break;
                case "PATCH":
                    stats.setEndpointsByPatchMapping(stats.getEndpointsByPatchMapping() + 1);
                    break;
                default:
                    stats.setEndpointsByRequestMapping(stats.getEndpointsByRequestMapping() + 1);
                    break;
            }

            // Endpoints by controller
            String controller = endpoint.getControllerName();
            stats.getEndpointsByController().merge(controller, 1, Integer::sum);
        }

        logger.info("Generated REST API statistics for repository {}: {} controllers, {} endpoints",
                repositoryId, stats.getTotalRestControllers(), stats.getTotalEndpoints());

        return stats;
    }
}