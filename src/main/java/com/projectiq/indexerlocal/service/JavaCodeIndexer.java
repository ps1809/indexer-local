package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.exception.NoJavaFilesException;
import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.IndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Java Code Indexing Engine - MVP implementation.
 * Parses Java source files and indexes structural metadata including:
 * - Packages, imports
 * - Classes, interfaces, enums, records
 * - Methods, constructors, fields
 * - Annotations, inheritance
 * 
 * Does NOT analyze business logic or generate call graphs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JavaCodeIndexer {

    private static final long SLOW_STEP_THRESHOLD_MS = 5000;

    private final IndexRepository indexRepository;

    /**
     * Index all Java files in a repository.
     */
    @Transactional
    public JavaIndexResult indexRepository(String repositoryId, String workspacePath) {
        log.info("================================================");
        log.info("Starting Java code indexing for repository: {}", repositoryId);
        log.info("Workspace path: {}", workspacePath);
        log.info("================================================");
        long startTime = System.currentTimeMillis();

        JavaIndexResult result = new JavaIndexResult();
        result.setRepositoryId(repositoryId);
        result.setIndexedAt(LocalDateTime.now());

        try {
            // Validate workspace exists and is accessible
            log.info("Starting indexing");
            log.info("Validating workspace path: {}", workspacePath);
            long validationStart = System.currentTimeMillis();
            
            Path rootPath = Paths.get(workspacePath);
            if (!Files.exists(rootPath)) {
                log.error("Indexing failed for repository {}: workspace path does not exist: {}", repositoryId, workspacePath);
                throw new NoJavaFilesException(repositoryId, workspacePath);
            }
            if (!Files.isDirectory(rootPath)) {
                log.error("Indexing failed for repository {}: workspace path is not a directory: {}", repositoryId, workspacePath);
                throw new NoJavaFilesException(repositoryId, workspacePath);
            }
            
            long validationDuration = System.currentTimeMillis() - validationStart;
            log.info("Workspace validation completed in {} ms", validationDuration);

            // Find all Java files
            log.info("Scanning workspace at: {}", workspacePath);
            long scanStart = System.currentTimeMillis();
            List<Path> javaFiles = findJavaFiles(workspacePath);
            long scanDuration = System.currentTimeMillis() - scanStart;
            log.info("Found {} Java files to index (scan took {} ms)", javaFiles.size(), scanDuration);

            if (scanDuration > SLOW_STEP_THRESHOLD_MS) {
                log.warn("WARNING: Step taking unusually long: scanning workspace");
                log.warn("Current stage: findJavaFiles");
                log.warn("Path: {}", workspacePath);
            }

            // Check if any Java files were found
            if (javaFiles.isEmpty()) {
                log.error("Indexing failed for repository {}: repository workspace contains no Java source files at path: {}", repositoryId, workspacePath);
                throw new NoJavaFilesException(repositoryId, workspacePath);
            }

            // Index each file
            List<FileIndex> indexedFiles = new ArrayList<>();
            List<String> parsingErrors = new ArrayList<>();
            
            for (int fileNum = 0; fileNum < javaFiles.size(); fileNum++) {
                Path javaFile = javaFiles.get(fileNum);
                log.info("------------------------------------------------");
                log.info("Processing file [{}/{}]: {}", fileNum + 1, javaFiles.size(), javaFile.toAbsolutePath());
                long fileStartTime = System.currentTimeMillis();

                try {
                    log.info("Entering indexJavaFile");
                    log.info("File: {}", javaFile.toAbsolutePath());
                    
                    FileIndex fileIndex = indexJavaFile(repositoryId, javaFile);
                    
                    log.info("Leaving indexJavaFile");
                    log.info("File: {}", javaFile.toAbsolutePath());
                    
                    indexedFiles.add(fileIndex);
                    
                    long fileDuration = System.currentTimeMillis() - fileStartTime;
                    log.info("Finished processing file: {}", javaFile.toAbsolutePath());
                    log.info("File processing took {} ms", fileDuration);
                    
                    if (fileDuration > SLOW_STEP_THRESHOLD_MS) {
                        log.warn("WARNING: Step taking unusually long: processing entire file");
                        log.warn("Current file: {}", javaFile.toAbsolutePath());
                        log.warn("Duration: {} ms", fileDuration);
                    }
                    
                    log.debug("Indexed: {}", javaFile.getFileName());
                } catch (Exception e) {
                    String errorMsg = "Failed to index " + javaFile.getFileName() + ": " + e.getMessage();
                    parsingErrors.add(errorMsg);
                    log.error("Error indexing file {}: {}", javaFile.getFileName(), e.getMessage(), e);
                    long fileDuration = System.currentTimeMillis() - fileStartTime;
                    log.info("Finished processing file (with errors): {} ({} ms)", javaFile.toAbsolutePath(), fileDuration);
                }
                
                log.info("------------------------------------------------");
            }

            // Calculate statistics
            log.info("Calculating indexing statistics...");
            long statsStart = System.currentTimeMillis();
            JavaIndexingStatistics stats = calculateStatistics(indexedFiles);
            long statsDuration = System.currentTimeMillis() - statsStart;
            log.info("Statistics calculation completed in {} ms", statsDuration);
            result.setStatistics(stats);
            result.setIndexedFiles(indexedFiles);
            result.setParsingErrors(parsingErrors);

            long duration = System.currentTimeMillis() - startTime;
            log.info("================================================");
            log.info("Java code indexing completed in {}ms. Indexed {} files, {} errors", 
                    duration, indexedFiles.size(), parsingErrors.size());
            log.info("================================================");

        } catch (Exception e) {
            log.error("Failed to index repository {}: {}", repositoryId, e.getMessage(), e);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * Find all Java source files in the workspace.
     */
    private List<Path> findJavaFiles(String workspacePath) {
        log.info("Entering findJavaFiles");
        log.info("Workspace: {}", workspacePath);
        
        List<Path> javaFiles = new ArrayList<>();
        Path rootPath = Paths.get(workspacePath);

        try {
            // Check if path exists and is a directory
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                log.warn("Workspace path does not exist or is not a directory: {}", workspacePath);
                log.info("Leaving findJavaFiles");
                log.info("Workspace: {}", workspacePath);
                return javaFiles;
            }

            // Find all .java files recursively
            log.info("Walking directory tree recursively...");
            javaFiles = Files.walk(rootPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            
            log.info("Found {} Java files", javaFiles.size());
        } catch (IOException e) {
            log.error("Error walking directory {}: {}", workspacePath, e.getMessage(), e);
        }

        log.info("Leaving findJavaFiles");
        log.info("Workspace: {}", workspacePath);
        return javaFiles;
    }

    /**
     * Index a single Java file.
     */
    private FileIndex indexJavaFile(String repositoryId, Path javaFile) throws IOException {
        log.info("Entering indexJavaFile");
        log.info("File: {}", javaFile.toAbsolutePath());
        
        long readStart = System.currentTimeMillis();
        String content = new String(Files.readAllBytes(javaFile));
        long readDuration = System.currentTimeMillis() - readStart;
        log.info("Read file content in {} ms ({} bytes)", readDuration, content.length());
        
        if (readDuration > SLOW_STEP_THRESHOLD_MS) {
            log.warn("WARNING: Step taking unusually long: reading file content");
            log.warn("Current file: {}", javaFile.toAbsolutePath());
        }
        
        String relativePath = javaFile.toString();
        
        FileIndex fileIndex = new FileIndex();
        fileIndex.setFilePath(relativePath);
        fileIndex.setFileName(javaFile.getFileName().toString());
        fileIndex.setRepositoryId(repositoryId);
        fileIndex.setIndexedAt(LocalDateTime.now());
        fileIndex.setStatus("INDEXED");

        // Extract package
        log.info("Parsing file...");
        long parseStart = System.currentTimeMillis();
        String packageName = extractPackage(content);
        long parseDuration = System.currentTimeMillis() - parseStart;
        log.info("Parsing completed in {} ms", parseDuration);
        
        if (parseDuration > SLOW_STEP_THRESHOLD_MS) {
            log.warn("WARNING: Step taking unusually long: extracting package");
            log.warn("Current file: {}", javaFile.toAbsolutePath());
        }
        
        log.info("Extracting package...");
        fileIndex.setPackageName(packageName);
        log.info("Package extracted: {}", packageName);

        // Extract imports
        log.info("Extracting imports...");
        long importStart = System.currentTimeMillis();
        List<ImportInfo> imports = extractImports(content);
        long importDuration = System.currentTimeMillis() - importStart;
        log.info("Imports extracted: {} imports found in {} ms", imports.size(), importDuration);
        
        if (importDuration > SLOW_STEP_THRESHOLD_MS) {
            log.warn("WARNING: Step taking unusually long: extracting imports");
            log.warn("Current file: {}", javaFile.toAbsolutePath());
        }
        
        fileIndex.setImports(imports);

        // Extract types
        log.info("Extracting classes...");
        long classStart = System.currentTimeMillis();
        List<ClassInfo> classes = extractClasses(content, relativePath, javaFile.getFileName().toString());
        long classDuration = System.currentTimeMillis() - classStart;
        log.info("Found {} classes in {} ms", classes.size(), classDuration);
        
        if (classDuration > SLOW_STEP_THRESHOLD_MS) {
            log.warn("WARNING: Step taking unusually long: extracting classes");
            log.warn("Current file: {}", javaFile.toAbsolutePath());
        }
        
        // Log each class
        for (ClassInfo ci : classes) {
            log.info("Processing class:");
            log.info("    name: {}", ci.getClassName());
            log.info("    package: {}", packageName);
        }
        
        fileIndex.setClasses(classes);
        fileIndex.setClassCount((long) classes.size());

        // Extract fields for each class
        log.info("Extracting fields...");
        long fieldCount = 0;
        for (ClassInfo ci : classes) {
            if (ci.getFields() != null) {
                log.info("Fields for class {}: {}", ci.getClassName(), ci.getFields().size());
                fieldCount += ci.getFields().size();
            }
        }
        log.info("Found {} total fields", fieldCount);

        // Extract constructors for each class
        log.info("Extracting constructors...");
        long constructorCount = 0;
        for (ClassInfo ci : classes) {
            if (ci.getConstructors() != null) {
                log.info("Constructors for class {}: {}", ci.getClassName(), ci.getConstructors().size());
                constructorCount += ci.getConstructors().size();
            }
        }
        log.info("Found {} total constructors", constructorCount);

        // Extract methods for each class
        log.info("Extracting methods...");
        long methodCountForLog = 0;
        for (ClassInfo ci : classes) {
            if (ci.getMethods() != null) {
                log.info("Methods for class {}: {}", ci.getClassName(), ci.getMethods().size());
                methodCountForLog += ci.getMethods().size();
            }
        }
        log.info("Found {} total methods", methodCountForLog);

        // Calculate counts
        long methodCount = classes.stream()
                .flatMap(c -> c.getMethods().stream())
                .count();
        long fieldCountTotal = classes.stream()
                .flatMap(c -> c.getFields().stream())
                .count();
        long annotationCount = classes.stream()
                .flatMap(c -> c.getAnnotations().stream())
                .count();
        
        fileIndex.setMethodCount(methodCount);
        fileIndex.setFieldCount(fieldCountTotal);
        fileIndex.setAnnotationCount(annotationCount);

        log.info("Leaving indexJavaFile");
        log.info("File: {}", javaFile.toAbsolutePath());
        
        return fileIndex;
    }

    /**
     * Extract package declaration from source code.
     */
    private String extractPackage(String content) {
        Pattern pattern = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Extract imports from source code.
     */
    private List<ImportInfo> extractImports(String content) {
        List<ImportInfo> imports = new ArrayList<>();
        
        // Normal imports
        Pattern normalPattern = Pattern.compile("^\\s*import\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
        Matcher normalMatcher = normalPattern.matcher(content);
        while (normalMatcher.find()) {
            ImportInfo imp = new ImportInfo();
            imp.setImportStatement(normalMatcher.group(1));
            imp.setType("NORMAL");
            imp.setIsWildcard(false);
            imports.add(imp);
        }

        // Static imports
        Pattern staticPattern = Pattern.compile("^\\s*import\\s+static\\s+([\\w.*]+)\\s*;", Pattern.MULTILINE);
        Matcher staticMatcher = staticPattern.matcher(content);
        while (staticMatcher.find()) {
            ImportInfo imp = new ImportInfo();
            imp.setImportStatement(staticMatcher.group(1));
            imp.setType("STATIC");
            imp.setIsWildcard(staticMatcher.group(1).contains("*"));
            imports.add(imp);
        }

        // Wildcard imports
        Pattern wildcardPattern = Pattern.compile("^\\s*import\\s+([\\w.]+)\\.\\*\\s*;", Pattern.MULTILINE);
        Matcher wildcardMatcher = wildcardPattern.matcher(content);
        while (wildcardMatcher.find()) {
            String statement = wildcardMatcher.group(1);
            // Check if it's a static import (already handled above)
            boolean isStatic = Pattern.compile("^\\s*import\\s+static\\s+" + Pattern.quote(statement) + "\\.\\*", Pattern.MULTILINE).matcher(content).find();
            if (!isStatic) {
                ImportInfo imp = new ImportInfo();
                imp.setImportStatement(statement);
                imp.setType("WILDCARD");
                imp.setIsWildcard(true);
                imports.add(imp);
            }
        }

        return imports;
    }

    /**
     * Extract class/interface/enum/record definitions from source code.
     */
    private List<ClassInfo> extractClasses(String content, String filePath, String fileName) {
        log.info("Entering extractClasses");
        log.info("File: {}", filePath);
        
        List<ClassInfo> classes = new ArrayList<>();
        
        // Pattern to match class/interface/enum/record declarations
        Pattern pattern = Pattern.compile(
            "((?:public|private|protected)?\\s*(?:abstract)?\\s*(?:final)?\\s*)?" +
            "(class|interface|enum|record)\\s+" +
            "([\\w<>\\[\\],\\s]+)" +
            "\\s*((?:extends|[\\w.]+<[^>]*>)?\\s*(?:implements\\s+([\\w.,\\s<>\\[\\]]+))?)?" +
            "\\s*\\{",
            Pattern.MULTILINE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            try {
                ClassInfo classInfo = new ClassInfo();
                
                // Get modifiers
                String modifiers = matcher.group(1) != null ? matcher.group(1).trim() : "";
                String visibility = extractVisibility(modifiers);
                boolean isAbstract = modifiers.contains("abstract");
                boolean isFinal = modifiers.contains("final");
                
                String className = matcher.group(3).split("[<>\\[]")[0].trim();
                classInfo.setFileName(fileName);
                classInfo.setFilePath(filePath);
                classInfo.setClassName(className);
                classInfo.setClassType(matcher.group(2).toUpperCase());
                classInfo.setVisibility(visibility);
                classInfo.setAbstract(isAbstract);
                classInfo.setFinal(isFinal);
                
                log.info("Found class: {} (type: {}, visibility: {})", className, matcher.group(2), visibility);
                
                // Extract superclass
                String extendsClause = matcher.group(4);
                if (extendsClause != null && extendsClause.contains("extends")) {
                    Pattern superPattern = Pattern.compile("extends\\s+([\\w.]+)");
                    Matcher superMatcher = superPattern.matcher(extendsClause);
                    if (superMatcher.find()) {
                        classInfo.setSuperClass(superMatcher.group(1));
                        log.info("  extends: {}", superMatcher.group(1));
                    }
                }
                
                // Extract implemented interfaces
                if (extendsClause != null && extendsClause.contains("implements")) {
                    Pattern implPattern = Pattern.compile("implements\\s+([\\w.,\\s<>\\[\\]]+)");
                    Matcher implMatcher = implPattern.matcher(extendsClause);
                    if (implMatcher.find()) {
                        String interfacesStr = implMatcher.group(1);
                        List<String> interfaces = Arrays.stream(interfacesStr.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                        classInfo.setInterfaces(interfaces);
                        log.info("  implements: {}", interfaces);
                    }
                }

                // Extract annotation for this type
                String typeAnnotations = extractTypeAnnotations(content, matcher.start());
                classInfo.setAnnotations(parseAnnotations(typeAnnotations));
                log.info("  annotations: {}", classInfo.getAnnotations().size());

                // Extract fields
                long fieldExtractStart = System.currentTimeMillis();
                String classBody = extractClassBody(content, matcher.end() - 1);
                classInfo.setFields(extractFields(classBody));
                long fieldExtractDuration = System.currentTimeMillis() - fieldExtractStart;
                log.info("  fields extracted: {} in {} ms", classInfo.getFields().size(), fieldExtractDuration);
                
                if (fieldExtractDuration > SLOW_STEP_THRESHOLD_MS) {
                    log.warn("WARNING: Step taking unusually long: extracting fields for class {}", className);
                    log.warn("Current file: {}", filePath);
                }
                
                // Extract methods (including constructors for records)
                long methodExtractStart = System.currentTimeMillis();
                classInfo.setMethods(extractMethods(classBody, matcher.group(3).split("[<>\\[]")[0].trim()));
                long methodExtractDuration = System.currentTimeMillis() - methodExtractStart;
                log.info("  methods extracted: {} in {} ms", classInfo.getMethods().size(), methodExtractDuration);
                
                if (methodExtractDuration > SLOW_STEP_THRESHOLD_MS) {
                    log.warn("WARNING: Step taking unusually long: extracting methods for class {}", className);
                    log.warn("Current file: {}", filePath);
                }

                classes.add(classInfo);
            } catch (Exception e) {
                log.warn("Error parsing class at position: {}", matcher.start());
            }
        }

        log.info("Leaving extractClasses");
        log.info("File: {}", filePath);
        log.info("Total classes found: {}", classes.size());
        
        return classes;
    }

    /**
     * Extract visibility from modifiers string.
     */
    private String extractVisibility(String modifiers) {
        if (modifiers.contains("public")) return "PUBLIC";
        if (modifiers.contains("protected")) return "PROTECTED";
        if (modifiers.contains("private")) return "PRIVATE";
        return "PACKAGE_PRIVATE";
    }

    /**
     * Extract annotations from source code at a given position.
     */
    private String extractTypeAnnotations(String content, int startPos) {
        StringBuilder annotations = new StringBuilder();
        
        // Look backwards for @Annotations before the class declaration
        int i = startPos - 1;
        while (i >= 0 && (content.charAt(i) == '@' || Character.isWhitespace(content.charAt(i)) || content.charAt(i) == ']')) {
            if (content.charAt(i) == '@') {
                // Found annotation, extract it
                int end = i;
                int start = i;
                while (start > 0 && content.charAt(start - 1) != '\n' && content.charAt(start - 1) != ';') {
                    start--;
                }
                annotations.insert(0, content.substring(start, end + 10).trim() + " ");
            }
            i--;
        }
        
        return annotations.toString();
    }

    /**
     * Parse annotation strings into AnnotationInfo objects.
     */
    private List<AnnotationInfo> parseAnnotations(String annotations) {
        List<AnnotationInfo> result = new ArrayList<>();
        
        if (annotations == null || annotations.isEmpty()) {
            return result;
        }

        Pattern pattern = Pattern.compile("@(\\w+)(?:<([^>]+)>)?\\s*(?:\\((.*)\\))?");
        Matcher matcher = pattern.matcher(annotations);
        
        while (matcher.find()) {
            AnnotationInfo annotation = new AnnotationInfo();
            annotation.setAnnotationName(matcher.group(1));
            
            if (matcher.group(2) != null) {
                annotation.setTypeParameters(matcher.group(2));
            }
            
            if (matcher.group(3) != null) {
                annotation.setArguments(matcher.group(3));
                // Count commas to estimate argument count
                long argCount = matcher.group(3).chars().filter(ch -> ch == ',').count() + 1;
                annotation.setArgumentCount((int) argCount);
            }
            
            result.add(annotation);
        }
        
        return result;
    }

    /**
     * Extract class body from source code.
     */
    private String extractClassBody(String content, int startIndex) {
        int braceCount = 0;
        int i = startIndex;
        
        // Find opening brace
        while (i < content.length() && content.charAt(i) != '{') {
            i++;
        }
        
        if (i >= content.length()) return "";
        
        braceCount = 1;
        i++;
        int start = i;
        
        while (i < content.length() && braceCount > 0) {
            if (content.charAt(i) == '{') braceCount++;
            else if (content.charAt(i) == '}') braceCount--;
            i++;
        }
        
        return content.substring(start, i - 1);
    }

    /**
     * Extract fields from class body.
     */
    private List<FieldInfo> extractFields(String classBody) {
        List<FieldInfo> fields = new ArrayList<>();
        
        // Pattern for field declarations
        Pattern pattern = Pattern.compile(
            "((?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*(?:final\\s+)?" +
            "[\\w<>\\[\\],\\s\\.]+)\\s+" +
            "([\\w<>\\[\\],\\s\\.]+)" +
            "(?:\\s*=\\s*([^;{\\n]+))?\\s*;",
            Pattern.MULTILINE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(classBody);
        
        while (matcher.find()) {
            // Skip if it looks like a method return type
            String fullMatch = matcher.group(0);
            if (fullMatch.trim().startsWith("return") || fullMatch.trim().startsWith("throw")) {
                continue;
            }

            FieldInfo field = new FieldInfo();
            field.setName(matcher.group(2).trim());
            field.setType(matcher.group(1).trim());
            
            String modifiers = fullMatch.toLowerCase();
            if (modifiers.contains("public")) field.setVisibility("PUBLIC");
            else if (modifiers.contains("protected")) field.setVisibility("PROTECTED");
            else if (modifiers.contains("private")) field.setVisibility("PRIVATE");
            else field.setVisibility("PACKAGE_PRIVATE");
            
            field.setStatic(modifiers.contains("static"));
            field.setFinal(modifiers.contains("final"));
            
            if (matcher.group(3) != null) {
                field.setDefaultValue(matcher.group(3).trim());
            }

            fields.add(field);
        }

        return fields;
    }

    /**
     * Extract methods from class body.
     */
    private List<MethodInfo> extractMethods(String classBody, String className) {
        log.info("Entering extractMethods");
        log.info("Class: {}", className);
        
        List<MethodInfo> methods = new ArrayList<>();
        
        // Pattern for method declarations
        Pattern pattern = Pattern.compile(
            "((?:public|private|protected)?\\s*(?:static)?\\s*(?:abstract)?\\s*(?:final)?\\s*" +
            "[\\w<>\\[\\],\\.\\s]+?)\\s+" +
            "([\\w<>])" +  // method name - must start with letter
            "\\s*\\(([^)]*)\\)" +  // parameters
            "(?:\\s*throws\\s+([\\w.,\\s<>\\[\\]]+))?" +  // throws clause
            "\\s*(?:\\{)",  // opening brace
            Pattern.MULTILINE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(classBody);
        
        while (matcher.find()) {
            String returnTypeAndModifiers = matcher.group(1).trim();
            String methodName = matcher.group(2);
            String params = matcher.group(3) != null ? matcher.group(3).trim() : "";
            String throwsClause = matcher.group(4) != null ? matcher.group(4).trim() : "";

            // Skip if it looks like a constructor call or control flow
            if (methodName.equals(className) || methodName.equals("new") || 
                methodName.equals("if") || methodName.equals("while") || 
                methodName.equals("for") || methodName.equals("switch") ||
                methodName.equals("return")) {
                continue;
            }

            MethodInfo method = new MethodInfo();
            method.setMethodName(methodName);
            
            // Extract return type (last type in the returnTypeAndModifiers)
            String[] parts = returnTypeAndModifiers.split("\\s+");
            if (parts.length > 0) {
                method.setReturnType(parts[parts.length - 1]);
            }
            
            // Extract visibility
            if (returnTypeAndModifiers.contains("public")) method.setVisibility("PUBLIC");
            else if (returnTypeAndModifiers.contains("protected")) method.setVisibility("PROTECTED");
            else if (returnTypeAndModifiers.contains("private")) method.setVisibility("PRIVATE");
            else method.setVisibility("PACKAGE_PRIVATE");
            
            method.setStatic(returnTypeAndModifiers.contains("static"));
            method.setAbstract(returnTypeAndModifiers.contains("abstract"));
            method.setFinal(returnTypeAndModifiers.contains("final"));
            
            // Extract parameters
            if (!params.isEmpty()) {
                List<String> paramList = parseParameters(params);
                method.setParameters(paramList);
            }
            
            // Store throws clause as exceptions
            if (!throwsClause.isEmpty()) {
                List<String> exceptions = Arrays.asList(throwsClause.split(","));
                method.setExceptions(exceptions);
            }

            methods.add(method);
        }

        log.info("Leaving extractMethods");
        log.info("Class: {}", className);
        log.info("Total methods found: {}", methods.size());
        
        return methods;
    }

    /**
     * Parse parameter string into list of "type paramName" strings.
     */
    private List<String> parseParameters(String params) {
        List<String> parameters = new ArrayList<>();
        
        // Simple comma split (doesn't handle nested generics perfectly)
        String[] paramArray = params.split(",");
        for (String param : paramArray) {
            String trimmed = param.trim();
            if (!trimmed.isEmpty()) {
                parameters.add(trimmed);
            }
        }
        
        return parameters;
    }

    /**
     * Calculate indexing statistics from indexed files.
     */
    private JavaIndexingStatistics calculateStatistics(List<FileIndex> indexedFiles) {
        JavaIndexingStatistics stats = new JavaIndexingStatistics();
        
        Set<String> packages = new HashSet<>();
        Set<String> classes = new HashSet<>();
        Set<String> interfaces = new HashSet<>();
        Set<String> enums = new HashSet<>();
        Set<String> records = new HashSet<>();
        
        for (FileIndex file : indexedFiles) {
            packages.add(file.getPackageName());
            
            if (file.getClasses() != null) {
                for (ClassInfo classInfo : file.getClasses()) {
                    classes.add(classInfo.getClassName());
                    
                    switch (classInfo.getClassType()) {
                        case "CLASS":
                            // Could be regular class or enum - check context
                            break;
                        case "INTERFACE":
                            interfaces.add(classInfo.getClassName());
                            break;
                        case "ENUM":
                            enums.add(classInfo.getClassName());
                            break;
                        case "RECORD":
                            records.add(classInfo.getClassName());
                            break;
                    }
                }
            }
        }

        stats.setTotalJavaFiles((long) indexedFiles.size());
        stats.setTotalPackages((long) packages.size());
        stats.setTotalClasses((long) classes.size());
        stats.setTotalInterfaces((long) interfaces.size());
        stats.setTotalEnums((long) enums.size());
        stats.setTotalRecords((long) records.size());
        stats.setTotalMethods((long) indexedFiles.stream()
                .flatMap(f -> f.getClasses().stream())
                .flatMap(c -> c.getMethods().stream())
                .count());
        stats.setTotalFields((long) indexedFiles.stream()
                .flatMap(f -> f.getClasses().stream())
                .flatMap(c -> c.getFields().stream())
                .count());

        return stats;
    }

    /**
     * Simple result class for indexing operation.
     */
    public static class JavaIndexResult {
        private String repositoryId;
        private LocalDateTime indexedAt;
        private JavaIndexingStatistics statistics;
        private List<FileIndex> indexedFiles;
        private List<String> parsingErrors;
        private String error;

        // Getters and setters
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        public LocalDateTime getIndexedAt() { return indexedAt; }
        public void setIndexedAt(LocalDateTime indexedAt) { this.indexedAt = indexedAt; }
        public JavaIndexingStatistics getStatistics() { return statistics; }
        public void setStatistics(JavaIndexingStatistics statistics) { this.statistics = statistics; }
        public List<FileIndex> getIndexedFiles() { return indexedFiles; }
        public void setIndexedFiles(List<FileIndex> indexedFiles) { this.indexedFiles = indexedFiles; }
        public List<String> getParsingErrors() { return parsingErrors; }
        public void setParsingErrors(List<String> parsingErrors) { this.parsingErrors = parsingErrors; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}