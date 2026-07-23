package com.projectiq.indexerlocal.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.projectiq.indexerlocal.extractor.ClassExtractor;
import com.projectiq.indexerlocal.extractor.FieldExtractor;
import com.projectiq.indexerlocal.extractor.ImportExtractor;
import com.projectiq.indexerlocal.extractor.MethodInfoExtractor;
import com.projectiq.indexerlocal.model.ClassInfo;
import com.projectiq.indexerlocal.model.FieldInfo;
import com.projectiq.indexerlocal.model.FileIndex;
import com.projectiq.indexerlocal.model.IndexResult;
import com.projectiq.indexerlocal.model.MethodInfo;
import org.springframework.stereotype.Service;

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
}