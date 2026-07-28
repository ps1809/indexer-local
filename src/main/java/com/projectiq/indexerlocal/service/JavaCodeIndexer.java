package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.exception.NoJavaFilesException;
import com.projectiq.indexerlocal.extractor.DeterministicJavaParser;
import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.IndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Java Code Indexing Engine - MVP implementation.
 * Parses Java source files and indexes structural metadata including:
 * - Packages, imports
 * - Classes, interfaces, enums, records
 * - Methods, constructors, fields
 * - Annotations, inheritance
 * 
 * Uses a deterministic character-by-character parser (no regex).
 * Does NOT analyze business logic or generate call graphs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JavaCodeIndexer {

    private static final long SLOW_STEP_THRESHOLD_MS = 5000;
    private static final long SLOW_OP_THRESHOLD_MS = 2000;

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

        // Extract package using deterministic parser
        log.info("Parsing file...");
        long parseStart = System.currentTimeMillis();
        String packageName = DeterministicJavaParser.extractPackage(content);
        long parseDuration = System.currentTimeMillis() - parseStart;
        log.info("Parsing completed in {} ms", parseDuration);
        
        if (parseDuration > SLOW_STEP_THRESHOLD_MS) {
            log.warn("WARNING: Step taking unusually long: extracting package");
            log.warn("Current file: {}", javaFile.toAbsolutePath());
        }
        
        log.info("Extracting package...");
        fileIndex.setPackageName(packageName);
        log.info("Package extracted: {}", packageName);

        // Extract imports using deterministic parser
        log.info("Extracting imports...");
        long importStart = System.currentTimeMillis();
        List<ImportInfo> imports = DeterministicJavaParser.extractImports(content);
        long importDuration = System.currentTimeMillis() - importStart;
        log.info("Imports extracted: {} imports found in {} ms", imports.size(), importDuration);
        
        if (importDuration > SLOW_STEP_THRESHOLD_MS) {
            log.warn("WARNING: Step taking unusually long: extracting imports");
            log.warn("Current file: {}", javaFile.toAbsolutePath());
        }
        
        fileIndex.setImports(imports);

        // Extract types using deterministic parser
        log.info("Extracting classes...");
        long classStart = System.currentTimeMillis();
        List<ClassInfo> classes = DeterministicJavaParser.extractClasses(content, relativePath, javaFile.getFileName().toString());
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