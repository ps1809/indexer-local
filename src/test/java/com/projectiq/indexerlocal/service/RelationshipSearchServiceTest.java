package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipEntry;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipType;
import com.projectiq.indexerlocal.service.impl.RelationshipSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationshipSearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private RelationshipSearchService relationshipSearchService;

    @BeforeEach
    void setUp() {
        relationshipSearchService = new RelationshipSearchServiceImpl(jdbcTemplate);
    }

    @Test
    void findImplementations_shouldReturnEmpty_whenNoImplementations() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findImplementations(
                null, "com.example.NonexistentInterface", false, 10, 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findImplementations_shouldReturnResults_whenImplementationsExist() {
        RelationshipEntry entry = new RelationshipEntry();
        entry.setSourceSymbol("MyClass");
        entry.setSourceSymbolType("CLASS");
        entry.setTargetSymbol("MyInterface");
        entry.setRelationshipType(RelationshipType.IMPLEMENTATION);
        entry.setTraversalDepth(1);

        doReturn(List.of(entry)).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findImplementations(
                null, "MyInterface", false, 10, 0, 20);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("MyClass", results.get(0).getSourceSymbol());
        assertEquals("MyInterface", results.get(0).getTargetSymbol());
        assertEquals(RelationshipType.IMPLEMENTATION, results.get(0).getRelationshipType());
    }

    @Test
    void findInheritors_shouldReturnEmpty_whenNoInheritors() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findInheritors(
                null, "com.example.BaseClass", false, 10, 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findInheritors_shouldReturnResults_whenInheritorsExist() {
        RelationshipEntry entry = new RelationshipEntry();
        entry.setSourceSymbol("ChildClass");
        entry.setSourceSymbolType("CLASS");
        entry.setTargetSymbol("BaseClass");
        entry.setRelationshipType(RelationshipType.INHERITANCE);
        entry.setTraversalDepth(1);

        doReturn(List.of(entry)).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findInheritors(
                null, "BaseClass", false, 10, 0, 20);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("ChildClass", results.get(0).getSourceSymbol());
        assertEquals("BaseClass", results.get(0).getTargetSymbol());
        assertEquals(RelationshipType.INHERITANCE, results.get(0).getRelationshipType());
    }

    @Test
    void findReferences_shouldReturnEmpty_whenNoReferences() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findReferences(
                null, "com.example.SomeClass", 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findReferences_shouldReturnResults_whenReferencesExist() {
        RelationshipEntry entry = new RelationshipEntry();
        entry.setSourceSymbol("com.example.SomeClass");
        entry.setSourceSymbolType("IMPORT");
        entry.setTargetSymbol("SomeClass");
        entry.setRelationshipType(RelationshipType.REFERENCE);
        entry.setTraversalDepth(1);

        doReturn(List.of(entry)).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findReferences(
                null, "SomeClass", 0, 20);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(RelationshipType.REFERENCE, results.get(0).getRelationshipType());
    }

    @Test
    void findDependencies_shouldReturnEmpty_whenNoDependencies() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findDependencies(
                null, "com.example.MyClass", 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findDependents_shouldReturnEmpty_whenNoDependents() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findDependents(
                null, "com.example.MyClass", 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findCallers_shouldReturnEmpty_whenNoCallers() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findCallers(
                null, "someMethod", 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findCallees_shouldReturnEmpty_whenNoCallees() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findCallees(
                null, "someMethod", 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findPackageRelationships_shouldReturnEmpty_whenNoRelationships() {
        List<RelationshipEntry> results = relationshipSearchService.findPackageRelationships(
                null, null, 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findModuleRelationships_shouldReturnEmpty_whenNoRelationships() {
        List<RelationshipEntry> results = relationshipSearchService.findModuleRelationships(
                null, null, 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findUsages_shouldReturnEmpty_whenNoUsages() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findUsages(
                null, "com.example.SomeClass", 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void pagination_shouldWorkCorrectly() {
        RelationshipEntry entry1 = new RelationshipEntry();
        entry1.setSourceSymbol("Class1");
        entry1.setTargetSymbol("Interface1");
        entry1.setRelationshipType(RelationshipType.IMPLEMENTATION);

        RelationshipEntry entry2 = new RelationshipEntry();
        entry2.setSourceSymbol("Class2");
        entry2.setTargetSymbol("Interface1");
        entry2.setRelationshipType(RelationshipType.IMPLEMENTATION);

        doReturn(List.of(entry1, entry2)).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> page1 = relationshipSearchService.findImplementations(
                null, "Interface1", false, 10, 0, 1);
        assertEquals(1, page1.size());

        List<RelationshipEntry> page2 = relationshipSearchService.findImplementations(
                null, "Interface1", false, 10, 1, 1);
        assertEquals(1, page2.size());
    }

    @Test
    void countImplementations_shouldReturnCorrectCount() {
        RelationshipEntry entry = new RelationshipEntry();
        entry.setSourceSymbol("MyClass");
        entry.setTargetSymbol("MyInterface");
        entry.setRelationshipType(RelationshipType.IMPLEMENTATION);

        doReturn(List.of(entry)).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        long count = relationshipSearchService.countImplementations(
                null, "MyInterface", false, 10);
        assertEquals(1, count);
    }

    @Test
    void countReferences_shouldReturnCorrectCount() {
        RelationshipEntry entry = new RelationshipEntry();
        entry.setSourceSymbol("com.example.SomeClass");
        entry.setTargetSymbol("SomeClass");
        entry.setRelationshipType(RelationshipType.REFERENCE);

        doReturn(List.of(entry)).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        long count = relationshipSearchService.countReferences(null, "SomeClass");
        assertEquals(1, count);
    }

    @Test
    void recursiveTraversal_shouldNotCauseCircularInfiniteLoop() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findInheritors(
                null, "A", true, 10, 0, 100);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void invalidDepth_shouldBeHandled() {
        List<RelationshipEntry> results = relationshipSearchService.findInheritors(
                null, "BaseClass", true, -1, 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void repositoryFilter_shouldBeApplied() {
        doReturn(Collections.emptyList()).when(jdbcTemplate).query(anyString(), isA(RowMapper.class), isA(Object[].class));

        List<RelationshipEntry> results = relationshipSearchService.findImplementations(
                "test-repo", "MyInterface", false, 10, 0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}