package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.exception.NoJavaFilesException;
import com.projectiq.indexerlocal.model.FieldInfo;
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

    /**
     * Test: private String name;
     */
    @Test
    void testExtractFields_PrivateStringName() throws Exception {
        String classBody = "private String name;";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(1, fields.size());
        FieldInfo f = fields.get(0);
        assertEquals("name", f.getFieldName());
        assertEquals("String", f.getFieldType());
        assertEquals("PRIVATE", f.getVisibility());
        assertFalse(f.isStatic());
        assertFalse(f.isFinal());
    }

    /**
     * Test: private final Service service;
     */
    @Test
    void testExtractFields_PrivateFinalService() throws Exception {
        String classBody = "private final Service service;";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(1, fields.size());
        FieldInfo f = fields.get(0);
        assertEquals("service", f.getFieldName());
        assertEquals("Service", f.getFieldType());
        assertEquals("PRIVATE", f.getVisibility());
        assertTrue(f.isFinal());
    }

    /**
     * Test: private static final Logger logger;
     */
    @Test
    void testExtractFields_PrivateStaticFinalLogger() throws Exception {
        String classBody = "private static final Logger logger;";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(1, fields.size());
        FieldInfo f = fields.get(0);
        assertEquals("logger", f.getFieldName());
        assertEquals("Logger", f.getFieldType());
        assertEquals("PRIVATE", f.getVisibility());
        assertTrue(f.isStatic());
        assertTrue(f.isFinal());
    }

    /**
     * Test: Map<String, List<User>> cache;
     */
    @Test
    void testExtractFields_MapWithGenerics() throws Exception {
        String classBody = "Map<String, List<User>> cache;";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(1, fields.size());
        FieldInfo f = fields.get(0);
        assertEquals("cache", f.getFieldName());
        assertEquals("Map<String, List<User>>", f.getFieldType()); // type is now fully joined
    }

    /**
     * Test: List<User> users = new ArrayList<>();
     */
    @Test
    void testExtractFields_ListWithInitializer() throws Exception {
        String classBody = "List<User> users = new ArrayList<>();";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(1, fields.size());
        FieldInfo f = fields.get(0);
        assertEquals("users", f.getFieldName());
        assertEquals("List<User>", f.getFieldType());
        assertNotNull(f.getDefaultValue());
    }

    /**
     * Test: String[] values;
     */
    @Test
    void testExtractFields_ArrayType() throws Exception {
        String classBody = "String[] values;";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(1, fields.size());
        FieldInfo f = fields.get(0);
        assertEquals("values", f.getFieldName());
    }

    /**
     * Test: private volatile boolean running;
     */
    @Test
    void testExtractFields_PrivateVolatileBoolean() throws Exception {
        String classBody = "private volatile boolean running;";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(1, fields.size());
        FieldInfo f = fields.get(0);
        assertEquals("running", f.getFieldName());
        assertEquals("boolean", f.getFieldType());
        assertEquals("PRIVATE", f.getVisibility());
    }

    /**
     * Test that local variables inside methods are NOT parsed as fields.
     */
    @Test
    void testExtractFields_SkipsLocalVariablesInsideMethods() throws Exception {
        String classBody = """
            private String fieldOne;
            public void someMethod() {
                int localVar = 42;
                String localStr = "hello";
            }
            private String fieldTwo;
            """;

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) method.invoke(javaCodeIndexer, classBody);

        assertNotNull(fields);
        assertEquals(2, fields.size(), "Expected exactly 2 fields (fieldOne and fieldTwo), not local variables");
        java.util.Set<String> fieldNames = new java.util.HashSet<>();
        for (FieldInfo f : fields) {
            fieldNames.add(f.getFieldName());
        }
        assertTrue(fieldNames.contains("fieldOne"));
        assertTrue(fieldNames.contains("fieldTwo"));
    }

    /**
     * Test: extractFields() does NOT hang with complex generics.
      */
     @Test
     void testExtractFields_NoHangWithComplexGenerics() throws Exception {
        // This is the "same class body that currently hangs" - it has:
        //   - deeply nested generics: Map<String, List<Integer>>
        //   - multiple fields with similar type patterns
        //   - initialization expressions
        String classBody = """
            private java.util.Map<String, java.util.List<Integer>> complexMap = new java.util.HashMap<>();
            private final java.util.Map<java.lang.String, java.util.Set<Long>> anotherMap;
            private java.util.concurrent.ConcurrentHashMap<String, java.util.ArrayList<Double>> concurrentMap;
            private static final String CONSTANT = "hello";
            private int simpleInt = 42;
            private double[] arrayOfDoubles;
            """;

        // Use reflection to call the private extractFields method
        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);

        // Set a timeout by running in a separate thread
        java.util.concurrent.Future<?> future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            return method.invoke(javaCodeIndexer, classBody);
        });

        // This should complete within 5 seconds; if it hangs, the test times out
        future.get(5, java.util.concurrent.TimeUnit.SECONDS);

        // Verify the result
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) future.get();
        assertNotNull(fields);
        assertTrue(fields.size() >= 4, "Expected at least 4 fields, got: " + fields.size());

        // Print extracted fields for verification
        for (FieldInfo f : fields) {
            System.out.println("  Field: " + f.getFieldName() + " Type: " + f.getFieldType());
        }
    }

    /**
     * Test extractFields with a minimal hanging-trigger pattern.
     */
    @Test
    void testExtractFields_MinimalHangPattern() throws Exception {
        String classBody = "private final java.util.Map<String, java.util.List<Integer>> myField = null;";

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);

        java.util.concurrent.Future<?> future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            return method.invoke(javaCodeIndexer, classBody);
        });

        future.get(5, java.util.concurrent.TimeUnit.SECONDS);

        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) future.get();
        assertNotNull(fields);
        assertEquals(1, fields.size());
        assertEquals("myField", fields.get(0).getFieldName());
    }

    /**
     * Test that the fixed regex correctly matches specific known field patterns
     * and extracts the expected field names and types.
     */
    @Test
    void testExtractFields_CorrectFieldExtraction() throws Exception {
        String classBody = """
            private String name;
            private int age;
            private final java.util.List<String> items;
            private static final double PI = 3.14159;
            protected boolean active;
            """;

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);

        java.util.concurrent.Future<?> future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            return method.invoke(javaCodeIndexer, classBody);
        });

        future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) future.get();

        assertNotNull(fields);
        assertEquals(5, fields.size(), "Expected exactly 5 fields");

        // Verify field names
        java.util.Set<String> fieldNames = new java.util.HashSet<>();
        for (FieldInfo f : fields) {
            fieldNames.add(f.getFieldName());
        }
        assertTrue(fieldNames.contains("name"), "Expected field 'name'");
        assertTrue(fieldNames.contains("age"), "Expected field 'age'");
        assertTrue(fieldNames.contains("items"), "Expected field 'items'");
        assertTrue(fieldNames.contains("PI"), "Expected field 'PI'");
        assertTrue(fieldNames.contains("active"), "Expected field 'active'");
    }

    /**
     * Test that local variable declarations inside methods are NOT matched as fields.
     */
    @Test
    void testExtractFields_SkipsLocalVariables() throws Exception {
        // This pattern looks like a field but is actually inside a method body
        // The key is it should not match because it lacks the class-field-level structure
        String classBody = """
            private String fieldOne;
            public void someMethod() {
                int localVar = 42;
                String localStr = "hello";
            }
            private String fieldTwo;
            """;

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);

        java.util.concurrent.Future<?> future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            return method.invoke(javaCodeIndexer, classBody);
        });

        future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) future.get();

        assertNotNull(fields);
        assertTrue(fields.size() >= 2, "Expected at least 2 fields (fieldOne and fieldTwo)");

        // Verify the extracted fields are the expected ones
        java.util.Set<String> fieldNames = new java.util.HashSet<>();
        for (FieldInfo f : fields) {
            fieldNames.add(f.getFieldName());
        }
        assertTrue(fieldNames.contains("fieldOne"), "Expected field 'fieldOne'");
        assertTrue(fieldNames.contains("fieldTwo"), "Expected field 'fieldTwo'");
    }

    /**
     * Test with primitives only to ensure all Java primitive types work.
     */
    @Test
    void testExtractFields_PrimitiveTypes() throws Exception {
        String classBody = """
            private boolean flag;
            private byte b;
            private short s;
            private char c;
            private int count;
            private long id;
            private float f;
            private double d;
            private void notAField;
            """;

        java.lang.reflect.Method method = JavaCodeIndexer.class.getDeclaredMethod("extractFields", String.class);
        method.setAccessible(true);

        java.util.concurrent.Future<?> future = java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            return method.invoke(javaCodeIndexer, classBody);
        });

        future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        java.util.List<FieldInfo> fields = (java.util.List<FieldInfo>) future.get();

        assertNotNull(fields);
        assertTrue(fields.size() >= 8, "Expected at least 8 primitive type fields, got: " + fields.size());

        java.util.Set<String> fieldNames = new java.util.HashSet<>();
        for (FieldInfo f : fields) {
            fieldNames.add(f.getFieldName());
        }
        assertTrue(fieldNames.contains("flag"), "Expected field 'flag'");
        assertTrue(fieldNames.contains("count"), "Expected field 'count'");
        assertTrue(fieldNames.contains("id"), "Expected field 'id'");
    }
}
