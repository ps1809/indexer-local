package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.ProjectStructureRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing and managing project structure metadata.
 */
@Service
public class ProjectStructureService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectStructureService.class);

    private static final Map<String, FileClassification> EXTENSION_CLASSIFICATIONS = new HashMap<>();
    static {
        EXTENSION_CLASSIFICATIONS.put("java", FileClassification.JAVA_SOURCE);
        EXTENSION_CLASSIFICATIONS.put("kt", FileClassification.KOTLIN);
        EXTENSION_CLASSIFICATIONS.put("kts", FileClassification.KOTLIN);
        EXTENSION_CLASSIFICATIONS.put("groovy", FileClassification.GROOVY);
        EXTENSION_CLASSIFICATIONS.put("gradle", FileClassification.GROOVY);
        EXTENSION_CLASSIFICATIONS.put("xml", FileClassification.XML);
        EXTENSION_CLASSIFICATIONS.put("yml", FileClassification.YAML);
        EXTENSION_CLASSIFICATIONS.put("yaml", FileClassification.YAML);
        EXTENSION_CLASSIFICATIONS.put("properties", FileClassification.PROPERTIES);
        EXTENSION_CLASSIFICATIONS.put("prop", FileClassification.PROPERTIES);
        EXTENSION_CLASSIFICATIONS.put("json", FileClassification.JSON);
        EXTENSION_CLASSIFICATIONS.put("sql", FileClassification.SQL);
        EXTENSION_CLASSIFICATIONS.put("md", FileClassification.MARKDOWN);
        EXTENSION_CLASSIFICATIONS.put("markdown", FileClassification.MARKDOWN);
        EXTENSION_CLASSIFICATIONS.put("html", FileClassification.HTML);
        EXTENSION_CLASSIFICATIONS.put("htm", FileClassification.HTML);
        EXTENSION_CLASSIFICATIONS.put("jsp", FileClassification.HTML);
        EXTENSION_CLASSIFICATIONS.put("js", FileClassification.JAVASCRIPT);
        EXTENSION_CLASSIFICATIONS.put("ts", FileClassification.TYPESCRIPT);
        EXTENSION_CLASSIFICATIONS.put("tsx", FileClassification.TYPESCRIPT);
        EXTENSION_CLASSIFICATIONS.put("css", FileClassification.CSS);
        EXTENSION_CLASSIFICATIONS.put("scss", FileClassification.CSS);
        EXTENSION_CLASSIFICATIONS.put("less", FileClassification.CSS);
        EXTENSION_CLASSIFICATIONS.put("sh", FileClassification.SHELL_SCRIPT);
        EXTENSION_CLASSIFICATIONS.put("bash", FileClassification.SHELL_SCRIPT);
        EXTENSION_CLASSIFICATIONS.put("bat", FileClassification.SHELL_SCRIPT);
        EXTENSION_CLASSIFICATIONS.put("cmd", FileClassification.SHELL_SCRIPT);
        EXTENSION_CLASSIFICATIONS.put("ps1", FileClassification.SHELL_SCRIPT);
    }

    private final ProjectStructureRepository structureRepository;
    private final RepositoryRepository repositoryRepository;

    public ProjectStructureService(ProjectStructureRepository structureRepository,
                                   RepositoryRepository repositoryRepository) {
        this.structureRepository = structureRepository;
        this.repositoryRepository = repositoryRepository;
    }

    /**
     * Analyze the project structure of a repository.
     */
    public ProjectStructureStatistics analyzeStructure(String repositoryId) {
        // Verify repository exists
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with ID '" + repositoryId + "' not found");
        }

        Path workspacePath = Paths.get(repository.getWorkspacePath());
        if (!Files.exists(workspacePath) || !Files.isDirectory(workspacePath)) {
            logger.warn("Workspace path does not exist: {}", workspacePath);
            return createEmptyStatistics(repositoryId);
        }

        logger.info("Starting project structure analysis for repository: {}", repositoryId);
        long startTime = System.currentTimeMillis();

        // Use atomic reference to allow modification in inner class
        final int[] deepestLevel = {0};
        
        List<DirectoryMetadata> directories = new ArrayList<>();
        List<FileMetadata> files = new ArrayList<>();

        try {
            // Walk the directory tree
            Files.walkFileTree(workspacePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    try {
                        Path relativePath = workspacePath.relativize(dir);
                        String relativePathStr = relativePath.toString().equals("") ? "." : relativePath.toString();
                        int depth = countPathParts(relativePathStr);

                        if (depth > deepestLevel[0]) {
                            deepestLevel[0] = depth;
                        }

                        DirectoryMetadata dirMeta = new DirectoryMetadata();
                        dirMeta.setRepositoryId(repositoryId);
                        dirMeta.setPath(dir.toAbsolutePath().toString());
                        dirMeta.setName(dir.getFileName() != null ? dir.getFileName().toString() : "");
                        dirMeta.setRelativePath(relativePathStr);
                        dirMeta.setDepth(depth);
                        dirMeta.setClassification(classifyDirectory(dir));
                        dirMeta.setHidden(isHidden(dir));
                        dirMeta.setLastModified(attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                        LocalDateTime now = LocalDateTime.now();
                        dirMeta.setCreatedAt(now);
                        dirMeta.setUpdatedAt(now);

                        directories.add(dirMeta);
                    } catch (Exception e) {
                        logger.warn("Failed to process directory: {}", dir, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Path relativePath = workspacePath.relativize(file);
                        String relativePathStr = relativePath.toString();
                        int depth = countPathParts(relativePathStr);

                        FileMetadata fileMeta = new FileMetadata();
                        fileMeta.setRepositoryId(repositoryId);
                        fileMeta.setPath(file.toAbsolutePath().toString());
                        fileMeta.setName(file.getFileName() != null ? file.getFileName().toString() : "");
                        fileMeta.setRelativePath(relativePathStr);
                        fileMeta.setExtension(getFileExtension(file));
                        fileMeta.setFileSize(attrs.size());
                        fileMeta.setClassification(classifyFile(file));
                        fileMeta.setHidden(isHidden(file));
                        fileMeta.setDepth(depth);
                        fileMeta.setLastModified(attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                        LocalDateTime now = LocalDateTime.now();
                        fileMeta.setCreatedAt(now);
                        fileMeta.setUpdatedAt(now);

                        files.add(fileMeta);
                    } catch (Exception e) {
                        logger.warn("Failed to process file: {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk file tree: " + workspacePath, e);
        }

        // Calculate statistics
        long totalSize = files.stream().mapToLong(FileMetadata::getFileSize).sum();

        Map<String, Integer> fileCountByExtension = files.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getExtension() != null ? f.getExtension() : "no_extension",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<DirectoryClassification, Integer> directoryCountByClassification = directories.stream()
                .collect(Collectors.groupingBy(
                        DirectoryMetadata::getClassification,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<FileClassification, Integer> fileCountByClassification = files.stream()
                .collect(Collectors.groupingBy(
                        FileMetadata::getClassification,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Find largest file
        Optional<FileMetadata> largestFile = files.stream()
                .max(Comparator.comparingLong(FileMetadata::getFileSize));

        // Persist directories
        for (DirectoryMetadata dir : directories) {
            structureRepository.saveDirectory(dir);
        }

        // Persist files
        for (FileMetadata file : files) {
            structureRepository.saveFile(file);
        }

        // Persist statistics
        ProjectStructureStatistics statistics = new ProjectStructureStatistics();
        statistics.setRepositoryId(repositoryId);
        statistics.setTotalDirectories(directories.size());
        statistics.setTotalFiles(files.size());
        statistics.setTotalSize(totalSize);
        statistics.setFileCountByExtension(fileCountByExtension);
        statistics.setDirectoryCountByClassification(directoryCountByClassification);
        statistics.setFileCountByClassification(fileCountByClassification);
        statistics.setDeepestDirectoryLevel(deepestLevel[0]);
        largestFile.ifPresent(f -> {
            statistics.setLargestFileSize(f.getFileSize());
            statistics.setLargestFileName(f.getName());
            statistics.setLargestFilePath(f.getRelativePath());
        });
        LocalDateTime now = LocalDateTime.now();
        statistics.setAnalyzedAt(now);

        structureRepository.saveStatistics(
                repositoryId,
                directories.size(),
                files.size(),
                totalSize,
                fileCountByExtension,
                directoryCountByClassification,
                fileCountByClassification,
                deepestLevel[0],
                largestFile.map(FileMetadata::getFileSize).orElse(0L),
                largestFile.map(FileMetadata::getName).orElse(""),
                largestFile.map(FileMetadata::getRelativePath).orElse("")
        );

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Project structure analysis completed for repository: {}. Found {} directories, {} files. Duration: {}ms",
                repositoryId, directories.size(), files.size(), duration);

        return statistics;
    }

    /**
     * Get the project structure for a repository.
     */
    public List<DirectoryMetadata> getStructure(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return structureRepository.findDirectoriesByRepositoryId(repositoryId);
    }

    /**
     * Get statistics for a repository's structure.
     */
    public ProjectStructureStatistics getStatistics(String repositoryId) {
        validateRepositoryExists(repositoryId);

        // Check if statistics exist
        try {
            Map<String, Integer> fileCountByExtension = structureRepository.findFileCountByExtension(repositoryId);
            if (fileCountByExtension == null || fileCountByExtension.isEmpty()) {
                return createEmptyStatistics(repositoryId);
            }
        } catch (Exception e) {
            return createEmptyStatistics(repositoryId);
        }

        // Build statistics from persisted data
        ProjectStructureStatistics statistics = new ProjectStructureStatistics();
        statistics.setRepositoryId(repositoryId);
        statistics.setTotalDirectories(structureRepository.countDirectoriesByRepositoryId(repositoryId));
        statistics.setTotalFiles(structureRepository.countFilesByRepositoryId(repositoryId));

        // Get total size by summing file sizes
        List<FileMetadata> files = structureRepository.findFilesByRepositoryId(repositoryId);
        long totalSize = files.stream().mapToLong(FileMetadata::getFileSize).sum();
        statistics.setTotalSize(totalSize);

        // Get counts by extension
        statistics.setFileCountByExtension(structureRepository.findFileCountByExtension(repositoryId));
        statistics.setDirectoryCountByClassification(structureRepository.findDirectoryCountByClassification(repositoryId));
        statistics.setFileCountByClassification(structureRepository.findFileCountByClassification(repositoryId));

        return statistics;
    }

    /**
     * Get files for a repository.
     */
    public List<FileMetadata> getFiles(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return structureRepository.findFilesByRepositoryId(repositoryId);
    }

    /**
     * Get directories for a repository.
     */
    public List<DirectoryMetadata> getDirectories(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return structureRepository.findDirectoriesByRepositoryId(repositoryId);
    }

    /**
     * Classify a directory based on its name.
     */
    private DirectoryClassification classifyDirectory(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString().toLowerCase() : "";

        if (name.equals("java") || name.equals("source") || name.equals("src")) {
            return DirectoryClassification.SOURCE;
        }
        if (name.equals("resources") || name.equals("res") || name.equals("assets")) {
            return DirectoryClassification.RESOURCE;
        }
        if (name.equals("test") || name.equals("tests") || name.equals("testing")) {
            return DirectoryClassification.TEST;
        }
        if (name.equals("config") || name.equals("configuration") || name.equals("cfg")) {
            return DirectoryClassification.CONFIGURATION;
        }
        if (name.equals("docs") || name.equals("documentation")) {
            return DirectoryClassification.DOCUMENTATION;
        }
        if (name.equals("build") || name.equals("target") || name.equals("compile")) {
            return DirectoryClassification.BUILD;
        }
        if (name.equals("dist") || name.equals("output") || name.equals("release")) {
            return DirectoryClassification.OUTPUT;
        }

        return DirectoryClassification.UNKNOWN;
    }

    /**
     * Classify a file based on its extension.
     */
    private FileClassification classifyFile(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        String extension = getFileExtension(path).toLowerCase();

        // Check for specific build files first (by name)
        if (name.equals("pom.xml") || name.equals("build.gradle") || name.equals("build.gradle.kts")
                || name.equals("gradle.properties") || name.equals("Makefile") || name.equals("CMakeLists.txt")) {
            return FileClassification.BUILD_FILE;
        }

        return EXTENSION_CLASSIFICATIONS.getOrDefault(extension, FileClassification.UNKNOWN);
    }

    /**
     * Get the file extension.
     */
    private String getFileExtension(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Check if a file or directory is hidden.
     */
    private boolean isHidden(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        return name.startsWith(".");
    }

    /**
     * Count the number of parts in a path.
     */
    private int countPathParts(String path) {
        if (path == null || path.equals(".") || path.isEmpty()) {
            return 0;
        }
        return path.split("/").length;
    }

    /**
     * Validate that the repository exists.
     */
    private void validateRepositoryExists(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with ID '" + repositoryId + "' not found");
        }
    }

    /**
     * Create empty statistics for a repository.
     */
    private ProjectStructureStatistics createEmptyStatistics(String repositoryId) {
        ProjectStructureStatistics statistics = new ProjectStructureStatistics();
        statistics.setRepositoryId(repositoryId);
        statistics.setTotalDirectories(0);
        statistics.setTotalFiles(0);
        statistics.setTotalSize(0L);
        statistics.setFileCountByExtension(new HashMap<>());
        statistics.setDirectoryCountByClassification(new HashMap<>());
        statistics.setFileCountByClassification(new HashMap<>());
        statistics.setDeepestDirectoryLevel(0);
        statistics.setAnalyzedAt(LocalDateTime.now());
        return statistics;
    }
}