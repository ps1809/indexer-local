package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.symbol.SymbolEntry;
import com.projectiq.indexerlocal.model.symbol.SymbolSearchResult;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.service.impl.SymbolSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SymbolSearchServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class SymbolSearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private IndexRepository indexRepository;

    private SymbolSearchService symbolSearchService;

    @BeforeEach
    void setUp() {
        symbolSearchService = new SymbolSearchServiceImpl(jdbcTemplate, indexRepository);
    }

    @Test
    void findClass_WithExactMatch_ReturnsResults() {
        // Mock count query
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        // Mock data query
        SymbolEntry mockEntry = new SymbolEntry();
        mockEntry.setSymbolName("TestClass");
        mockEntry.setSymbolType("CLASS");
        mockEntry.setVisibility("PUBLIC");
        mockEntry.setFilePath("/repo/src/main/java/com/test/TestClass.java");
        mockEntry.setLanguage("java");
        mockEntry.setPackageName("com.test");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(mockEntry));

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "TestClass", "EXACT", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("TestClass", result.getContent().get(0).getSymbolName());
        assertEquals("CLASS", result.getContent().get(0).getSymbolType());
    }

    @Test
    void findClass_WithPrefixMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(2L);

        SymbolEntry entry1 = new SymbolEntry();
        entry1.setSymbolName("TestClass");
        entry1.setSymbolType("CLASS");
        entry1.setVisibility("PUBLIC");
        entry1.setFilePath("/repo/src/main/java/com/test/TestClass.java");
        entry1.setLanguage("java");

        SymbolEntry entry2 = new SymbolEntry();
        entry2.setSymbolName("TestHelper");
        entry2.setSymbolType("CLASS");
        entry2.setVisibility("PUBLIC");
        entry2.setFilePath("/repo/src/main/java/com/test/TestHelper.java");
        entry2.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry1, entry2));

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "Test", "PREFIX", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findClass_WithNoResults_ReturnsEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(0L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of());

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "NonExistent", "EXACT", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void findInterface_WithPartialMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("MyInterface");
        entry.setSymbolType("INTERFACE");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/MyInterface.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findInterface("repo1", "Interface", "PARTIAL", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("MyInterface", result.getContent().get(0).getSymbolName());
        assertEquals("INTERFACE", result.getContent().get(0).getSymbolType());
    }

    @Test
    void findEnum_WithExactMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("Color");
        entry.setSymbolType("ENUM");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/Color.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findEnum("repo1", "Color", "EXACT", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Color", result.getContent().get(0).getSymbolName());
    }

    @Test
    void findRecord_WithExactMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("Point");
        entry.setSymbolType("RECORD");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/Point.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findRecord("repo1", "Point", "EXACT", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Point", result.getContent().get(0).getSymbolName());
    }

    @Test
    void findMethod_WithPartialMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("calculate");
        entry.setSymbolType("METHOD");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/Calculator.java");
        entry.setParentSymbol("Calculator");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findMethod("repo1", "calc", "PARTIAL", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("calculate", result.getContent().get(0).getSymbolName());
        assertEquals("Calculator", result.getContent().get(0).getParentSymbol());
    }

    @Test
    void findField_WithExactMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("name");
        entry.setSymbolType("FIELD");
        entry.setVisibility("PRIVATE");
        entry.setFilePath("/repo/src/main/java/com/test/Person.java");
        entry.setParentSymbol("Person");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findField("repo1", "name", "EXACT", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("name", result.getContent().get(0).getSymbolName());
        assertEquals("Person", result.getContent().get(0).getParentSymbol());
    }

    @Test
    void findPackage_WithPartialMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("com.test");
        entry.setSymbolType("PACKAGE");
        entry.setFilePath("/repo/src/main/java/com/test/TestClass.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findPackage("repo1", "com.test", "PARTIAL", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("com.test", result.getContent().get(0).getSymbolName());
    }

    @Test
    void findAnnotation_WithExactMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("Override");
        entry.setFullyQualifiedName("java.lang.Override");
        entry.setSymbolType("ANNOTATION");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findAnnotation("repo1", "Override", "EXACT", null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Override", result.getContent().get(0).getSymbolName());
    }

    @Test
    void searchSymbols_WithClassType_DelegatesToFindClass() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("TestClass");
        entry.setSymbolType("CLASS");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/TestClass.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.searchSymbols("repo1", "TestClass", "EXACT", "CLASS", null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("CLASS", result.getContent().get(0).getSymbolType());
    }

    @Test
    void searchSymbols_WithUnknownType_ReturnsEmpty() {
        SymbolSearchResult result = symbolSearchService.searchSymbols("repo1", "test", "PARTIAL", "UNKNOWN_TYPE", null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void searchSymbols_WithNoType_SearchesAll() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(0L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of());

        SymbolSearchResult result = symbolSearchService.searchSymbols("repo1", "test", "PARTIAL", null, null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findClass_WithVisibilityFilter_AppliesFilter() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("InternalClass");
        entry.setSymbolType("CLASS");
        entry.setVisibility("PRIVATE");
        entry.setFilePath("/repo/src/main/java/com/test/InternalClass.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "Internal", "PREFIX", null, "PRIVATE", 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PRIVATE", result.getContent().get(0).getVisibility());
    }

    @Test
    void findClass_WithPackageFilter_AppliesFilter() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("ServiceClass");
        entry.setSymbolType("CLASS");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/service/ServiceClass.java");
        entry.setPackageName("com.test.service");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "Service", "PREFIX", "com.test.service", null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findClass_WithPagination_ReturnsCorrectPage() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(5L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("Class3");
        entry.setSymbolType("CLASS");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/Class3.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "Class", "PREFIX", null, null, 1, 2);

        assertNotNull(result);
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
    }

    @Test
    void findClass_WithEmptyQuery_ReturnsAll() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(3L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("ClassA");
        entry.setSymbolType("CLASS");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/ClassA.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "", "PARTIAL", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void findConstructor_WithExactMatch_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("TestClass");
        entry.setSymbolType("METHOD");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/TestClass.java");
        entry.setParentSymbol("TestClass");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findConstructor("repo1", "TestClass", "EXACT", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findClass_WithFqnMode_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("MyClass");
        entry.setSymbolType("CLASS");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/MyClass.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findClass("repo1", "MyClass", "FQN", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findClass_WithNullRepositoryId_ReturnsResults() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);

        SymbolEntry entry = new SymbolEntry();
        entry.setSymbolName("GlobalClass");
        entry.setSymbolType("CLASS");
        entry.setVisibility("PUBLIC");
        entry.setFilePath("/repo/src/main/java/com/test/GlobalClass.java");
        entry.setLanguage("java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(entry));

        SymbolSearchResult result = symbolSearchService.findClass(null, "GlobalClass", "EXACT", null, null, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}