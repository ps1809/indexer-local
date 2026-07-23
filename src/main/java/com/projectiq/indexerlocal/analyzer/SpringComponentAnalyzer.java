package com.projectiq.indexerlocal.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.projectiq.indexerlocal.model.AnnotationInfo;
import com.projectiq.indexerlocal.model.SpringComponent;
import com.projectiq.indexerlocal.model.SpringComponentStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Analyzes Java source files to discover and classify Spring Framework components.
 * Detects stereotypes, configuration classes, beans, scheduling, messaging, caching,
 * transactions, security annotations, and dependency injection patterns.
 */
@Component
public class SpringComponentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SpringComponentAnalyzer.class);

    // Spring stereotype annotations
    private static final String ANNOTATION_COMPONENT = "Component";
    private static final String ANNOTATION_SERVICE = "Service";
    private static final String ANNOTATION_REPOSITORY = "Repository";
    private static final String ANNOTATION_CONTROLLER = "Controller";
    private static final String ANNOTATION_REST_CONTROLLER = "RestController";

    // Configuration annotations
    private static final String ANNOTATION_CONFIGURATION = "Configuration";
    private static final String ANNOTATION_BEAN = "Bean";
    private static final String ANNOTATION_CONFIGURATION_PROPERTIES = "ConfigurationProperties";
    private static final String ANNOTATION_PROPERTY_SOURCE = "PropertySource";
    private static final String ANNOTATION_IMPORT = "Import";

    // Dependency injection annotations
    private static final String ANNOTATION_AUTOWIRED = "Autowired";
    private static final String ANNOTATION_INJECT = "Inject";
    private static final String ANNOTATION_RESOURCE = "Resource";

    // Web annotations
    private static final String ANNOTATION_CONTROLLER_ADVICE = "ControllerAdvice";
    private static final String ANNOTATION_REST_CONTROLLER_ADVICE = "RestControllerAdvice";
    private static final String ANNOTATION_CROSS_ORIGIN = "CrossOrigin";
    private static final String ANNOTATION_RESPONSE_BODY = "ResponseBody";

    // Transaction annotations
    private static final String ANNOTATION_TRANSACTIONAL = "Transactional";

    // Scheduling annotations
    private static final String ANNOTATION_ENABLE_SCHEDULING = "EnableScheduling";
    private static final String ANNOTATION_SCHEDULED = "Scheduled";

    // Async annotations
    private static final String ANNOTATION_ENABLE_ASYNC = "EnableAsync";
    private static final String ANNOTATION_ASYNC = "Async";

    // Caching annotations
    private static final String ANNOTATION_ENABLE_CACHING = "EnableCaching";
    private static final String ANNOTATION_CACHEABLE = "Cacheable";
    private static final String ANNOTATION_CACHE_PUT = "CachePut";
    private static final String ANNOTATION_CACHE_EVICT = "CacheEvict";

    // Security annotations
    private static final String ANNOTATION_ENABLE_WEB_SECURITY = "EnableWebSecurity";
    private static final String ANNOTATION_ENABLE_METHOD_SECURITY = "EnableMethodSecurity";
    private static final String ANNOTATION_PRE_AUTHORIZE = "PreAuthorize";
    private static final String ANNOTATION_POST_AUTHORIZE = "PostAuthorize";
    private static final String ANNOTATION_ROLES_ALLOWED = "RolesAllowed";
    private static final String ANNOTATION_SECURED = "Secured";

    // Event and messaging annotations
    private static final String ANNOTATION_EVENT_LISTENER = "EventListener";
    private static final String ANNOTATION_KAFKA_LISTENER = "KafkaListener";
    private static final String ANNOTATION_RABBIT_LISTENER = "RabbitListener";
    private static final String ANNOTATION_JMS_LISTENER = "JmsListener";

    private final JavaParser javaParser = new JavaParser();

    /**
     * Analyze all Java files in a project directory for Spring components.
     */
    public List<SpringComponent> analyze(String projectPath, String repositoryId) {
        logger.info("Starting Spring component analysis for repository: {}", repositoryId);
        
        Path path = Path.of(projectPath);
        List<SpringComponent> components = new ArrayList<>();
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
                    SpringComponent component = analyzeJavaFile(javaFile, repositoryId);
                    if (component != null) {
                        components.add(component);
                        logger.info("Discovered Spring component: {} ({}) in {}", 
                                component.getClassName(), component.getComponentType(), javaFile.getFileName());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to analyze file {}: {}", javaFile.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to walk directory {}: {}", projectPath, e.getMessage());
            throw new RuntimeException("Failed to analyze Spring components in " + projectPath, e);
        }

        logger.info("Spring component analysis complete for repository {}. Found {} components from {} files.", 
                repositoryId, components.size(), totalFilesScanned);

        return components;
    }

    /**
     * Analyze a single Java file for Spring annotations.
     */
    private SpringComponent analyzeJavaFile(Path filePath, String repositoryId) throws IOException {
        String content = Files.readString(filePath);
        
        CompilationUnit cu = javaParser.parse(content).getResult()
                .orElseThrow(() -> new IOException("Failed to parse: " + filePath));

        String packageName = cu.getPackageDeclaration()
                .map(pkg -> pkg.getName().asString())
                .orElse("");
        
        SpringComponent resultComponent = null;

        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration classDecl : classes) {
            List<String> annotations = classDecl.getAnnotations()
                    .stream()
                    .map(AnnotationExpr::getNameAsString)
                    .toList();

            if (isSpringClassLevelAnnotation(annotations)) {
                if (resultComponent == null) {
                    resultComponent = extractSpringComponent(filePath, classDecl, packageName, repositoryId);
                } else {
                    // Also check methods for @Bean and other annotations in additional classes
                    analyzeMethods(classDecl, resultComponent);
                }
                // Analyze methods of this class too
                analyzeMethods(classDecl, resultComponent);
            }
        }

        return resultComponent;
    }

    /**
     * Check if a class has any Spring-relevant annotation.
     */
    private boolean isSpringClassLevelAnnotation(List<String> annotations) {
        for (String annotation : annotations) {
            if (ANNOTATION_COMPONENT.equalsIgnoreCase(annotation) ||
                ANNOTATION_SERVICE.equalsIgnoreCase(annotation) ||
                ANNOTATION_REPOSITORY.equalsIgnoreCase(annotation) ||
                ANNOTATION_CONTROLLER.equalsIgnoreCase(annotation) ||
                ANNOTATION_REST_CONTROLLER.equalsIgnoreCase(annotation) ||
                ANNOTATION_CONFIGURATION.equalsIgnoreCase(annotation) ||
                ANNOTATION_CONTROLLER_ADVICE.equalsIgnoreCase(annotation) ||
                ANNOTATION_REST_CONTROLLER_ADVICE.equalsIgnoreCase(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract Spring component information from a class declaration.
     */
    private SpringComponent extractSpringComponent(Path filePath, ClassOrInterfaceDeclaration classDecl, 
                                                    String packageName, String repositoryId) {
        SpringComponent component = new SpringComponent();
        component.setRepositoryId(repositoryId);
        component.setClassName(classDecl.getNameAsString());
        component.setPackageName(packageName);
        component.setSourceFile(filePath.toString());
        component.setComponentName(classDecl.getNameAsString());
        
        List<AnnotationExpr> annotations = classDecl.getAnnotations();
        
        // Determine component type and set flags
        determineComponentType(annotations, component);

        // Extract annotation details
        for (AnnotationExpr anno : annotations) {
            String annoName = anno.getNameAsString();
            AnnotationInfo annotationInfo = AnnotationInfo.from(anno);
            if (annotationInfo != null) {
                annotationInfo.setTargetType("CLASS");
                annotationInfo.setTargetId(null);
                component.addAnnotation(annotationInfo);
            }
            
            setAnnotationFlags(component, annoName, anno);
        }

        return component;
    }

    /**
     * Determine the Spring component type from annotations.
     */
    private void determineComponentType(List<AnnotationExpr> annotations, SpringComponent component) {
        for (AnnotationExpr anno : annotations) {
            String annoName = anno.getNameAsString();
            switch (annoName) {
                case ANNOTATION_SERVICE:
                    component.setService(true);
                    component.setComponentType("SERVICE");
                    break;
                case ANNOTATION_REPOSITORY:
                    component.setRepository(true);
                    component.setComponentType("REPOSITORY");
                    break;
                case ANNOTATION_CONTROLLER:
                    component.setController(true);
                    component.setComponentType("CONTROLLER");
                    break;
                case ANNOTATION_REST_CONTROLLER:
                    component.setRestController(true);
                    component.setComponentType("REST_CONTROLLER");
                    break;
                case ANNOTATION_CONFIGURATION:
                    component.setConfiguration(true);
                    component.setComponentType("CONFIGURATION");
                    break;
                default:
                    if (ANNOTATION_COMPONENT.equalsIgnoreCase(annoName)) {
                        component.setComponent(true);
                        component.setComponentType("COMPONENT");
                    }
            }
        }
    }

    /**
     * Set specific annotation flags on the component.
     */
    private void setAnnotationFlags(SpringComponent component, String annotationName, AnnotationExpr anno) {
        if (ANNOTATION_BEAN.equalsIgnoreCase(annotationName)) {
            component.setBean(true);
            extractBeanName(component, anno);
            return;
        }
        
        switch (annotationName) {
            case ANNOTATION_CONFIGURATION_PROPERTIES:
                component.setConfigurationProperties(true);
                break;
            case ANNOTATION_PROPERTY_SOURCE:
                component.setPropertySource(true);
                break;
            case ANNOTATION_IMPORT:
                component.setImport(true);
                break;
            case ANNOTATION_AUTOWIRED:
                component.setAutowired(true);
                break;
            case ANNOTATION_INJECT:
                component.setInject(true);
                break;
            case ANNOTATION_RESOURCE:
                component.setResource(true);
                break;
            case ANNOTATION_CONTROLLER_ADVICE:
                component.setControllerAdvice(true);
                break;
            case ANNOTATION_REST_CONTROLLER_ADVICE:
                component.setRestControllerAdvice(true);
                break;
            case ANNOTATION_CROSS_ORIGIN:
                component.setCrossOrigin(true);
                break;
            case ANNOTATION_RESPONSE_BODY:
                component.setResponseBody(true);
                break;
            case ANNOTATION_TRANSACTIONAL:
                component.setTransactional(true);
                extractTransactionAttributes(component, anno);
                break;
            case ANNOTATION_ENABLE_SCHEDULING:
                component.setHasEnableScheduling(true);
                break;
            case ANNOTATION_SCHEDULED:
                component.setScheduled(true);
                break;
            case ANNOTATION_ENABLE_ASYNC:
                component.setHasEnableAsync(true);
                break;
            case ANNOTATION_ASYNC:
                component.setAsync(true);
                break;
            case ANNOTATION_ENABLE_CACHING:
                component.setHasEnableCaching(true);
                break;
            case ANNOTATION_CACHEABLE:
                component.setCacheable(true);
                break;
            case ANNOTATION_CACHE_PUT:
                component.setCachePut(true);
                break;
            case ANNOTATION_CACHE_EVICT:
                component.setCacheEvict(true);
                break;
            case ANNOTATION_ENABLE_WEB_SECURITY:
                component.setHasEnableWebSecurity(true);
                break;
            case ANNOTATION_ENABLE_METHOD_SECURITY:
                component.setHasEnableMethodSecurity(true);
                break;
            case ANNOTATION_PRE_AUTHORIZE:
                component.setPreAuthorize(true);
                break;
            case ANNOTATION_POST_AUTHORIZE:
                component.setPostAuthorize(true);
                break;
            case ANNOTATION_ROLES_ALLOWED:
                component.setRolesAllowed(true);
                break;
            case ANNOTATION_SECURED:
                component.setSecured(true);
                break;
            case ANNOTATION_EVENT_LISTENER:
                component.setEventListener(true);
                break;
            case ANNOTATION_KAFKA_LISTENER:
                component.setKafkaListener(true);
                break;
            case ANNOTATION_RABBIT_LISTENER:
                component.setRabbitListener(true);
                break;
            case ANNOTATION_JMS_LISTENER:
                component.setJmsListener(true);
                break;
        }
    }

    /**
     * Extract bean name from @Bean annotation.
     */
    private void extractBeanName(SpringComponent component, AnnotationExpr anno) {
        if (anno.toString().contains("name=")) {
            String[] parts = anno.toString().split("name=");
            if (parts.length > 1) {
                String namePart = parts[1].trim();
                if (namePart.startsWith("\"")) {
                    namePart = namePart.substring(1, namePart.lastIndexOf("\""));
                }
                component.setBeanName(namePart);
            }
        } else if (anno.toString().contains("value=")) {
            String[] parts = anno.toString().split("value=");
            if (parts.length > 1) {
                String valuePart = parts[1].trim();
                if (valuePart.startsWith("\"")) {
                    valuePart = valuePart.substring(1, valuePart.lastIndexOf("\""));
                }
                component.setBeanName(valuePart);
            }
        }
    }

    /**
     * Extract transaction attributes from @Transactional annotation.
     */
    private void extractTransactionAttributes(SpringComponent component, AnnotationExpr anno) {
        String annotationStr = anno.toString();
        if (annotationStr.contains("propagation=")) {
            String[] parts = annotationStr.split("propagation=");
            if (parts.length > 1) {
                String propPart = parts[1].trim();
                int endIdx = propPart.indexOf(",");
                if (endIdx > 0) {
                    propPart = propPart.substring(0, endIdx).trim();
                }
                component.setTransactionPropagation(propPart.replace("\"", ""));
            }
        }
        if (annotationStr.contains("isolation=")) {
            String[] parts = annotationStr.split("isolation=");
            if (parts.length > 1) {
                String isoPart = parts[1].trim();
                int endIdx = isoPart.indexOf(",");
                if (endIdx > 0) {
                    isoPart = isoPart.substring(0, endIdx).trim();
                }
                component.setTransactionIsolation(isoPart.replace("\"", ""));
            }
        }
    }

    /**
     * Analyze methods in a class for @Bean and other method-level annotations.
     */
    private void analyzeMethods(ClassOrInterfaceDeclaration classDecl, SpringComponent component) {
        List<MethodDeclaration> methods = classDecl.findAll(MethodDeclaration.class);
        
        for (MethodDeclaration method : methods) {
            List<String> methodAnnotations = method.getAnnotations()
                    .stream()
                    .map(AnnotationExpr::getNameAsString)
                    .toList();

            // Check if this is a constructor by checking if return type is empty
            boolean isConstructor = method.getType().toString().equals(classDecl.getNameAsString());

            // Check for @Bean methods in configuration classes
            if (methodAnnotations.contains(ANNOTATION_BEAN)) {
                logger.debug("Found @Bean method: {} in {}", method.getNameAsString(), classDecl.getNameAsString());
            }

            // Check for dependency injection patterns in methods (not constructors)
            if (methodAnnotations.contains(ANNOTATION_AUTOWIRED) && 
                !isConstructor) {
                component.setHasSetterInjection(true);
            }

            // Check @EventListener on methods
            if (methodAnnotations.contains(ANNOTATION_EVENT_LISTENER)) {
                component.setEventListener(true);
            }

            // Check @Scheduled on methods
            if (methodAnnotations.contains(ANNOTATION_SCHEDULED)) {
                component.setScheduled(true);
            }

            // Check @Async on methods
            if (methodAnnotations.contains(ANNOTATION_ASYNC)) {
                component.setAsync(true);
            }

            // Check constructor injection via @Autowired constructor
            if (method.getParameters().size() > 0 && methodAnnotations.contains(ANNOTATION_AUTOWIRED)) {
                // This could be constructor injection
                component.setHasConstructorInjection(true);
            }
        }

        // Check for field-level @Autowired injection
        List<FieldDeclaration> fields = classDecl.findAll(FieldDeclaration.class);
        for (FieldDeclaration field : fields) {
            if (field.getAnnotations().stream()
                    .anyMatch(a -> ANNOTATION_AUTOWIRED.equalsIgnoreCase(a.getNameAsString()))) {
                // Field injection detected - this is already covered by the class-level check
                // if @Autowired appears on the class level for field declarations
            }
        }
    }

    /**
     * Generate statistics from a list of Spring components.
     */
    public SpringComponentStatistics generateStatistics(List<SpringComponent> components, String repositoryId) {
        SpringComponentStatistics stats = new SpringComponentStatistics();
        stats.setRepositoryId(repositoryId);

        for (SpringComponent component : components) {
            stats.setTotalComponents(stats.getTotalComponents() + 1);

            // Count by stereotype
            if (component.isService()) stats.setServiceCount(stats.getServiceCount() + 1);
            if (component.isRepository()) stats.setRepositoryCount(stats.getRepositoryCount() + 1);
            if (component.isController()) stats.setControllerCount(stats.getControllerCount() + 1);
            if (component.isRestController()) stats.setRestControllerCount(stats.getRestControllerCount() + 1);
            if (component.isComponent()) stats.setComponentBeanCount(stats.getComponentBeanCount() + 1);
            if (component.isConfiguration()) stats.setConfigurationClassCount(stats.getConfigurationClassCount() + 1);

            // Count DI annotations
            if (component.hasAutowired()) stats.setAutowiredCount(stats.getAutowiredCount() + 1);
            if (component.hasInject()) stats.setInjectCount(stats.getInjectCount() + 1);
            if (component.hasResource()) stats.setResourceCount(stats.getResourceCount() + 1);
            if (component.isHasConstructorInjection()) stats.setConstructorInjectionCount(stats.getConstructorInjectionCount() + 1);
            if (component.isHasSetterInjection()) stats.setSetterInjectionCount(stats.getSetterInjectionCount() + 1);

            // Count web annotations
            if (component.isControllerAdvice()) stats.setControllerAdviceCount(stats.getControllerAdviceCount() + 1);
            if (component.isRestControllerAdvice()) stats.setRestControllerAdviceCount(stats.getRestControllerAdviceCount() + 1);
            if (component.hasCrossOrigin()) stats.setCrossOriginCount(stats.getCrossOriginCount() + 1);
            if (component.hasResponseBody()) stats.setResponseBodyCount(stats.getResponseBodyCount() + 1);

            // Count transaction annotations
            if (component.hasTransactional()) stats.setTransactionalCount(stats.getTransactionalCount() + 1);

            // Count scheduling
            if (component.hasScheduled()) stats.setScheduledJobCount(stats.getScheduledJobCount() + 1);

            // Count async
            if (component.hasAsync()) stats.setAsyncMethodCount(stats.getAsyncMethodCount() + 1);

            // Count caching
            if (component.hasCacheable()) stats.setCacheableCount(stats.getCacheableCount() + 1);
            if (component.hasCachePut()) stats.setCachePutCount(stats.getCachePutCount() + 1);
            if (component.hasCacheEvict()) stats.setCacheEvictCount(stats.getCacheEvictCount() + 1);

            // Count security
            if (component.hasPreAuthorize()) stats.setPreAuthorizeCount(stats.getPreAuthorizeCount() + 1);
            if (component.hasPostAuthorize()) stats.setPostAuthorizeCount(stats.getPostAuthorizeCount() + 1);
            if (component.hasRolesAllowed()) stats.setRolesAllowedCount(stats.getRolesAllowedCount() + 1);
            if (component.hasSecured()) stats.setSecuredCount(stats.getSecuredCount() + 1);

            // Count events and messaging
            if (component.hasEventListener()) stats.setEventListenerCount(stats.getEventListenerCount() + 1);
            if (component.hasKafkaListener()) stats.setKafkaListenerCount(stats.getKafkaListenerCount() + 1);
            if (component.hasRabbitListener()) stats.setRabbitListenerCount(stats.getRabbitListenerCount() + 1);
            if (component.hasJmsListener()) stats.setJmsListenerCount(stats.getJmsListenerCount() + 1);

            // Count configuration annotations
            if (component.isConfigurationProperties()) stats.setConfigurationPropertiesCount(stats.getConfigurationPropertiesCount() + 1);
            if (component.isPropertySource()) stats.setPropertySourceCount(stats.getPropertySourceCount() + 1);
            if (component.isImport()) stats.setImportAnnotationCount(stats.getImportAnnotationCount() + 1);
        }

        logger.info("Generated Spring component statistics for repository {}: {} total components", 
                repositoryId, stats.getTotalComponents());

        return stats;
    }
}