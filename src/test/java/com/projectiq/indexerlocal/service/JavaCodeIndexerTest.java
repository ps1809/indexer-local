package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.exception.NoJavaFilesException;
import com.projectiq.indexerlocal.model.FileIndex;
import com.projectiq.indexerlocal.model.JavaIndexingStatistics;
import com.projectiq.indexerlocal.repository.IndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaCodeIndexer.
 */
@ExtendWith(MockitoExtension.class)
class JavaCodeIndexerTest {

    @Mock
    private IndexRepository indexRepository;

    private JavaCodeIndexer javaCodeIndexer;

    @BeforeEach
    void setUp() {
        javaCodeIndexer = new JavaCodeIndexer(indexRepository);
    }

    @Test
    void testIndexRepository_Success(@TempDir Path tempDir) throws IOException {
        // Create a Java file in the temp directory
        Path javaFile = tempDir.resolve("TestClass.java");
        Files.writeString(javaFile, "package com.test;\n\npublic class TestClass {\n    private String name;\n\n    public String getName() {\n        return name;\n    }\n}\n");

        JavaCodeIndexer.JavaIndexResult result = javaCodeIndexer.indexRepository("test-repo", tempDir.toString());

        assertNotNull(result);
        assertEquals("test-repo", result.getRepositoryId());
        assertNull(result.getError());
        assertNotNull(result.getIndexedFiles());
        assertEquals(1, result.getIndexedFiles().size());
        assertNotNull(result.getStatistics());
        assertEquals(1, result.getStatistics().getTotalJavaFiles());
    }

    @Test
    void testIndexRepository_NoJavaFiles(@TempDir Path tempDir) throws IOException {
        // Create a non-Java file in the temp directory
        Path txtFile = tempDir.resolve("readme.txt");
        Files.writeString(txtFile, "This is a text file.");

        JavaCodeIndexer.JavaIndexResult result = javaCodeIndexer.indexRepository("test-repo", tempDir.toString());

        assertNotNull(result);
        assertEquals("test-repo", result.getRepositoryId());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Repository workspace contains no Java source files."));
    }

    @Test
    void testIndexRepository_NonExistentPath() {
        String nonExistentPath = "C:\\nonexistent\\path\\to\\workspace";

        JavaCodeIndexer.JavaIndexResult result = javaCodeIndexer.indexRepository("test-repo", nonExistentPath);

        assertNotNull(result);
        assertEquals("test-repo", result.getRepositoryId());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Repository workspace contains no Java source files."));
    }

    @Test
    void testIndexRepository_EmptyDirectory(@TempDir Path tempDir) {
        JavaCodeIndexer.JavaIndexResult result = javaCodeIndexer.indexRepository("test-repo", tempDir.toString());

        assertNotNull(result);
        assertEquals("test-repo", result.getRepositoryId());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Repository workspace contains no Java source files."));
    }

    @Test
    void testIndexRepository_MultipleJavaFiles(@TempDir Path tempDir) throws IOException {
        // Create two Java files
        Path javaFile1 = tempDir.resolve("ClassA.java");
        Files.writeString(javaFile1, "package com.test;\n\npublic class ClassA {}\n");

        Path javaFile2 = tempDir.resolve("ClassB.java");
        Files.writeString(javaFile2, "package com.test;\n\npublic class ClassB {}\n");

        JavaCodeIndexer.JavaIndexResult result = javaCodeIndexer.indexRepository("test-repo", tempDir.toString());

        assertNotNull(result);
        assertEquals("test-repo", result.getRepositoryId());
        assertNull(result.getError());
        assertNotNull(result.getIndexedFiles());
        assertEquals(2, result.getIndexedFiles().size());
        assertNotNull(result.getStatistics());
        assertEquals(2, result.getStatistics().getTotalJavaFiles());
    }

    @Test
    void testNoJavaFilesException_Message() {
        NoJavaFilesException exception = new NoJavaFilesException("test-repo", "/tmp/test-workspace");

        assertEquals("test-repo", exception.getRepositoryId());
        assertEquals("/tmp/test-workspace", exception.getWorkspacePath());
        assertEquals("Repository workspace contains no Java source files.", exception.getMessage());
    }

    @Test
    void testJavaIndexResult_DefaultValues() {
        JavaCodeIndexer.JavaIndexResult result = new JavaCodeIndexer.JavaIndexResult();

        assertNull(result.getRepositoryId());
        assertNull(result.getIndexedAt());
        assertNull(result.getStatistics());
        assertNull(result.getIndexedFiles());
        assertNull(result.getParsingErrors());
        assertNull(result.getError());

        result.setRepositoryId("test-repo");
        assertEquals("test-repo", result.getRepositoryId());

        result.setError("An error occurred");
        assertEquals("An error occurred", result.getError());
    }

    @Test
    void testIndexRepository_SubdirectoryWithJavaFiles(@TempDir Path tempDir) throws IOException {
        // Create a subdirectory with a Java file
        Path subDir = tempDir.resolve("src/main/java/com/test");
        Files.createDirectories(subDir);
        Path javaFile = subDir.resolve("MyClass.java");
        Files.writeString(javaFile, "package com.test;\n\npublic class MyClass {}\n");

        JavaCodeIndexer.JavaIndexResult result = javaCodeIndexer.indexRepository("test-repo", tempDir.toString());

        assertNotNull(result);
        assertEquals("test-repo", result.getRepositoryId());
        assertNull(result.getError());
        assertEquals(1, result.getIndexedFiles().size());
        assertEquals(1, result.getStatistics().getTotalJavaFiles());
    }
}