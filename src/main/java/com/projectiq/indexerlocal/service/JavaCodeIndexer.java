package com.projectiq.indexerlocal.service;

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

    private final IndexRepository indexRepository;

    /**
     * Index all Java files in a repository.
     */
    @Transactional
    public JavaIndexResult indexRepository(String repositoryId, String workspacePath) {
        log.info("Starting Java code indexing for repository: {}", repositoryId);
        long startTime = System.currentTimeMillis();

        JavaIndexResult result = new JavaIndexResult();
        result.setRepositoryId(repositoryId);
        result.setIndexedAt(LocalDateTime.now());

        try {
            // Find all Java files
            List<Path> javaFiles = findJavaFiles(workspacePath);
            log.info("Found {} Java files to index", javaFiles.size());

            // Index each file
            List<FileIndex> indexedFiles = new ArrayList<>();
            List<String> parsingErrors = new ArrayList<>();
            
            for (Path javaFile : javaFiles) {
                try {
                    FileIndex fileIndex = indexJavaFile(repositoryId, javaFile);
                    indexedFiles.add(fileIndex);
                    log.debug("Indexed: {}", javaFile.getFileName());
                } catch (Exception e) {
                    String errorMsg = "Failed to index " + javaFile.getFileName() + ": " + e.getMessage();
                    parsingErrors.add(errorMsg);
                    log.error("Error indexing file {}: {}", javaFile.getFileName(), e.getMessage(), e);
                }
            }

            // Calculate statistics
            JavaIndexingStatistics stats = calculateStatistics(indexedFiles);
            result.setStatistics(stats);
            result.setIndexedFiles(indexedFiles);
            result.setParsingErrors(parsingErrors);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Java code indexing completed in {}ms. Indexed {} files, {} errors", 
                    duration, indexedFiles.size(), parsingErrors.size());

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
        List<Path> javaFiles = new ArrayList<>();
        Path rootPath = Paths.get(workspacePath);

        try {
            // Check if path exists and is a directory
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                log.warn("Workspace path does not exist or is not a directory: {}", workspacePath);
                return javaFiles;
            }

            // Find all .java files recursively
            javaFiles = Files.walk(rootPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error walking directory {}: {}", workspacePath, e.getMessage(), e);
        }

        return javaFiles;
    }

    /**
     * Index a single Java file.
     */
    private FileIndex indexJavaFile(String repositoryId, Path javaFile) throws IOException {
        String content = new String(Files.readAllBytes(javaFile));
        String relativePath = javaFile.toString();
        
        FileIndex fileIndex = new FileIndex();
        fileIndex.setFilePath(relativePath);
        fileIndex.setFileName(javaFile.getFileName().toString());
        fileIndex.setRepositoryId(repositoryId);
        fileIndex.setIndexedAt(LocalDateTime.now());
        fileIndex.setStatus("INDEXED");

        // Extract package
        String packageName = extractPackage(content);
        fileIndex.setPackageName(packageName);

        // Extract imports
        List<ImportInfo> imports = extractImports(content);
        fileIndex.setImports(imports);

        // Extract types
        List<ClassInfo> classes = extractClasses(content, relativePath, javaFile.getFileName().toString());
        fileIndex.setClasses(classes);
        fileIndex.setClassCount((long) classes.size());

        // Calculate counts
        long methodCount = classes.stream()
                .flatMap(c -> c.getMethods().stream())
                .count();
        long fieldCount = classes.stream()
                .flatMap(c -> c.getFields().stream())
                .count();
        long annotationCount = classes.stream()
                .flatMap(c -> c.getAnnotations().stream())
                .count();
        
        fileIndex.setMethodCount(methodCount);
        fileIndex.setFieldCount(fieldCount);
        fileIndex.setAnnotationCount(annotationCount);

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
                
                classInfo.setFileName(fileName);
                classInfo.setFilePath(filePath);
                classInfo.setClassName(matcher.group(3).split("[<>\\[]")[0].trim());
                classInfo.setClassType(matcher.group(2).toUpperCase());
                classInfo.setVisibility(visibility);
                classInfo.setAbstract(isAbstract);
                classInfo.setFinal(isFinal);
                
                // Extract superclass
                String extendsClause = matcher.group(4);
                if (extendsClause != null && extendsClause.contains("extends")) {
                    Pattern superPattern = Pattern.compile("extends\\s+([\\w.]+)");
                    Matcher superMatcher = superPattern.matcher(extendsClause);
                    if (superMatcher.find()) {
                        classInfo.setSuperClass(superMatcher.group(1));
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
                    }
                }

                // Extract annotation for this type
                String typeAnnotations = extractTypeAnnotations(content, matcher.start());
                classInfo.setAnnotations(parseAnnotations(typeAnnotations));

                // Extract fields
                String classBody = extractClassBody(content, matcher.end() - 1);
                classInfo.setFields(extractFields(classBody));
                
                // Extract methods (including constructors for records)
                classInfo.setMethods(extractMethods(classBody, matcher.group(3).split("[<>\\[]")[0].trim()));

                classes.add(classInfo);
            } catch (Exception e) {
                log.warn("Error parsing class at position: {}", matcher.start());
            }
        }

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