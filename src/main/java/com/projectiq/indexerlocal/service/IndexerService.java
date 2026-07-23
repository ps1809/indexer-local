package com.projectiq.indexerlocal.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.projectiq.indexerlocal.extractor.ClassExtractor;
import com.projectiq.indexerlocal.extractor.FieldExtractor;
import com.projectiq.indexerlocal.extractor.ImportExtractor;
import com.projectiq.indexerlocal.extractor.MethodInfoExtractor;
import com.projectiq.indexerlocal.model.AnnotationInfo;
import com.projectiq.indexerlocal.model.ClassInfo;
import com.projectiq.indexerlocal.model.RepositorySummary;
import com.projectiq.indexerlocal.model.FieldInfo;
import com.projectiq.indexerlocal.model.FileIndex;
import com.projectiq.indexerlocal.model.IndexResult;
import com.projectiq.indexerlocal.model.MethodInfo;
import com.projectiq.indexerlocal.model.SpringComponent;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Core indexing service that scans, parses, and extracts metadata from Java files.
 */
@Service
public class IndexerService {

    private final JavaParser javaParser = new JavaParser();
    private final ClassExtractor classExtractor = new ClassExtractor();
    private final FieldExtractor fieldExtractor = new FieldExtractor();
    private final MethodInfoExtractor methodInfoExtractor = new MethodInfoExtractor();
    private final ImportExtractor importExtractor = new ImportExtractor();
    private final IndexRepository indexRepository;
    private final RepositoryRepository repositoryRepository;

    public IndexerService(IndexRepository indexRepository, RepositoryRepository repositoryRepository) {
        this.indexRepository = indexRepository;
        this.repositoryRepository = repositoryRepository;
    }

    /**
     * Index a Spring Boot project at the given path.
     */
    public IndexResult index(String projectPath) {
        Path path = Path.of(projectPath);
        List<FileIndex> fileIndexes = new ArrayList<>();

        // Scan for Java files
        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                FileIndex fileIndex = parseAndExtract(javaFile);
                if (fileIndex != null) {
                    fileIndexes.add(fileIndex);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to index project at " + projectPath, e);
        }

        return new IndexResult(projectPath, fileIndexes);
    }

    private FileIndex parseAndExtract(Path filePath) {
        try {
            String content = Files.readString(filePath);
            CompilationUnit cu = javaParser.parse(content).getResult()
                    .orElseThrow(() -> new IOException("Failed to parse: " + filePath));

            String relativePath = filePath.toString();
            String fileName = filePath.getFileName().toString();

            FileIndex fileIndex = new FileIndex(relativePath, fileName);

            // Run extractors
            classExtractor.extract(cu, fileIndex);
            importExtractor.extract(cu, fileIndex);

            // Count fields and methods from classes
            long totalFields = fileIndex.getClasses().stream()
                    .mapToLong(c -> c.getFields() != null ? c.getFields().size() : 0)
                    .sum();
            long totalMethods = fileIndex.getClasses().stream()
                    .mapToLong(c -> c.getMethods() != null ? c.getMethods().size() : 0)
                    .sum();
            long totalAnnotations = fileIndex.getClasses().stream()
                    .mapToLong(c -> c.getAnnotations() != null ? c.getAnnotations().size() : 0)
                    .sum();

            fileIndex.setFieldCount(totalFields);
            fileIndex.setMethodCount(totalMethods);
            fileIndex.setAnnotationCount(totalAnnotations);

            return fileIndex;
        } catch (IOException e) {
            System.err.println("Failed to process file: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    // ==================== File Indexing Operations ====================

    /**
     * Index a single uploaded file.
     */
    public void indexFile(MultipartFile file, String filePath) throws IOException {
        String content = new String(file.getBytes());
        CompilationUnit cu = javaParser.parse(content).getResult()
                .orElseThrow(() -> new IOException("Failed to parse file"));

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        if (filePath != null) {
            fileName = filePath;
        }

        FileIndex fileIndex = new FileIndex(fileName, fileName);

        classExtractor.extract(cu, fileIndex);
        importExtractor.extract(cu, fileIndex);

        long totalFields = fileIndex.getClasses().stream()
                .mapToLong(c -> c.getFields() != null ? c.getFields().size() : 0)
                .sum();
        long totalMethods = fileIndex.getClasses().stream()
                .mapToLong(c -> c.getMethods() != null ? c.getMethods().size() : 0)
                .sum();
        long totalAnnotations = fileIndex.getClasses().stream()
                .mapToLong(c -> c.getAnnotations() != null ? c.getAnnotations().size() : 0)
                .sum();

        fileIndex.setFieldCount(totalFields);
        fileIndex.setMethodCount(totalMethods);
        fileIndex.setAnnotationCount(totalAnnotations);

        indexRepository.saveIndexResult(new IndexResult(fileName, List.of(fileIndex)));
    }

    /**
     * Index a directory.
     */
    public void indexDirectory(String directoryPath) {
        IndexResult result = index(directoryPath);
        indexRepository.saveIndexResult(result);
    }

    // ==================== File Query Operations ====================

    /**
     * List all indexed source files.
     */
    public List<FileIndex> listFiles() {
        return indexRepository.findAllFiles();
    }

    /**
     * Get a source file by ID.
     */
    public FileIndex getFileById(Long id) {
        return indexRepository.findFileById(id);
    }

    /**
     * Get a source file by path.
     */
    public FileIndex getFileByPath(String path) {
        return indexRepository.findFileByPath(path);
    }

    // ==================== Class Query Operations ====================

    /**
     * List all Java classes.
     */
    public List<ClassInfo> listClasses() {
        return indexRepository.findAllClasses();
    }

    /**
     * Get a class by ID.
     */
    public ClassInfo getClassById(Long id) {
        return indexRepository.findClassById(id);
    }

    /**
     * Get a class by name.
     */
    public ClassInfo getClassByName(String name) {
        return indexRepository.findClassByName(name);
    }

    // ==================== Method Query Operations ====================

    /**
     * List all methods.
     */
    public List<MethodInfo> listMethods() {
        return indexRepository.findAllMethods();
    }

    /**
     * Get a method by ID.
     */
    public MethodInfo getMethodById(Long id) {
        return indexRepository.findMethodById(id);
    }

    /**
     * Get a method by name.
     */
    public MethodInfo getMethodByName(String name) {
        return indexRepository.findMethodByName(name);
    }

    // ==================== Field Query Operations ====================

    /**
     * List all fields.
     */
    public List<FieldInfo> listFields() {
        return indexRepository.findAllFields();
    }

    /**
     * Get a field by ID.
     */
    public FieldInfo getFieldById(Long id) {
        return indexRepository.findFieldById(id);
    }

    /**
     * Get a field by name.
     */
    public FieldInfo getFieldByName(String name) {
        return indexRepository.findFieldByName(name);
    }

    // ==================== Spring Component Query Operations ====================

    /**
     * List all Spring components.
     */
    public List<SpringComponent> listSpringComponents() {
        return indexRepository.findAllSpringComponents();
    }

    // ==================== Lookup Operations ====================

    /**
     * Search classes by name pattern (partial match).
     */
    public List<ClassInfo> searchClassesByName(String namePattern) {
        return indexRepository.searchClassesByName(namePattern);
    }

    /**
     * Search methods by name pattern (partial match).
     */
    public List<MethodInfo> searchMethodsByName(String namePattern) {
        return indexRepository.searchMethodsByName(namePattern);
    }

    /**
     * Search fields by name pattern (partial match).
     */
    public List<FieldInfo> searchFieldsByName(String namePattern) {
        return indexRepository.searchFieldsByName(namePattern);
    }

    /**
     * Search classes by package/path pattern.
     */
    public List<ClassInfo> searchClassesByPackage(String packagePattern) {
        return indexRepository.searchClassesByPackage(packagePattern);
    }

    /**
     * Search annotations by name pattern.
     */
    public List<AnnotationInfo> searchAnnotationsByName(String namePattern) {
        return indexRepository.searchAnnotationsByName(namePattern);
    }

    /**
     * Get annotations for a specific target (class, method, field).
     */
    public List<AnnotationInfo> getAnnotationsByTarget(String targetType, Long targetId) {
        return indexRepository.findAnnotationsByTarget(targetType, targetId);
    }

    /**
     * Get full class detail including methods, fields, and annotations.
     */
    public ClassInfo getClassDetailById(Long classId) {
        return indexRepository.findClassDetailById(classId);
    }

    /**
     * Get methods for a specific class.
     */
    public List<MethodInfo> getMethodsByClassId(Long classId) {
        return indexRepository.findMethodsByClassId(classId);
    }

    /**
     * Get fields for a specific class.
     */
    public List<FieldInfo> getFieldsByClassId(Long classId) {
        return indexRepository.findFieldsByClassId(classId);
    }

    /**
     * Search files by path pattern.
     */
    public List<FileIndex> searchFilesByPath(String pathPattern) {
        return indexRepository.searchFilesByPath(pathPattern);
    }

    /**
     * Get all distinct Spring component types.
     */
    public List<String> getAvailableComponentTypes() {
        return indexRepository.findAllComponentTypes();
    }

    /**
     * Search Spring components by type (case-insensitive).
     */
    public List<SpringComponent> searchSpringComponentsByType(String type) {
        return indexRepository.searchSpringComponentsByTypeIgnoreCase(type);
    }

    /**
     * Search Spring components by class name pattern.
     */
    public List<SpringComponent> searchSpringComponentsByName(String namePattern) {
        return indexRepository.searchSpringComponentsByName(namePattern);
    }

    // ==================== Repository Summary Operations ====================

    /**
     * Get repository summary statistics from indexed metadata.
     */
    public RepositorySummary getRepositorySummary() {
        return indexRepository.getRepositorySummary();
    }

    // ==================== Java Code Indexing Engine Operations ====================

    /**
     * Get a repository by its ID.
     */
    public Repository getRepositoryById(String repositoryId) {
        return repositoryRepository.findByRepositoryId(repositoryId);
    }

    /**
     * List all indexed files for a specific repository.
     */
    public List<FileIndex> listFilesByRepositoryId(String repositoryId) {
        return indexRepository.findAllFilesByRepositoryId(repositoryId);
    }
}
