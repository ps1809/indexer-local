package com.projectiq.indexerlocal.service.context;

import com.projectiq.indexerlocal.model.context.ContextEntry;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipEntry;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipType;
import com.projectiq.indexerlocal.service.RelationshipSearchService;
import com.projectiq.indexerlocal.service.SpringSearchService;
import com.projectiq.indexerlocal.service.SymbolSearchService;
import com.projectiq.indexerlocal.service.context.impl.SmartContextServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartContextServiceTest {

    @Mock
    private SymbolSearchService symbolSearchService;
    @Mock
    private RelationshipSearchService relationshipSearchService;
    @Mock
    private SpringSearchService springSearchService;

    private SmartContextService smartContextService;

    @BeforeEach
    void setUp() {
        smartContextService = new SmartContextServiceImpl(
                symbolSearchService, relationshipSearchService, springSearchService);
    }

    @Test
    void testExpandImports() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextEntry entry = new ContextEntry("symbol", "UserService", 
                "com.example.UserService", 1);
        entry.setPackageName("com.example");
        context.getSymbols().add(entry);

        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setMaxRelationships(50);

        RelationshipEntry dep = new RelationshipEntry();
        dep.setTargetSymbol("com.example.UserRepository");
        dep.setRelationshipType(RelationshipType.DEPENDENCY);

        when(relationshipSearchService.findDependencies(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dep));

        // Act
        smartContextService.expandImports(context, request);

        // Assert
        assertFalse(context.getDependencies().isEmpty());
        assertEquals("com.example.UserRepository", context.getDependencies().get(0).getName());
    }

    @Test
    void testExpandDependencies() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextEntry entry = new ContextEntry("symbol", "UserService",
                "com.example.UserService", 1);
        context.getSymbols().add(entry);

        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setMaxRelationships(50);

        RelationshipEntry dep = new RelationshipEntry();
        dep.setTargetSymbol("com.example.Dependency");
        dep.setRelationshipType(RelationshipType.DEPENDENCY);

        when(relationshipSearchService.findDependencies(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dep));
        when(relationshipSearchService.findDependents(any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        smartContextService.expandDependencies(context, request);

        // Assert
        assertFalse(context.getDependencies().isEmpty());
    }

    @Test
    void testExpandCallGraph() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextEntry entry = new ContextEntry("symbol", "UserService",
                "com.example.UserService", 1);
        context.getSymbols().add(entry);

        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setMaxRelationships(50);

        RelationshipEntry callee = new RelationshipEntry();
        callee.setTargetSymbol("com.example.Helper");
        callee.setRelationshipType(RelationshipType.CALL);

        when(relationshipSearchService.findCallees(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(callee));
        when(relationshipSearchService.findCallers(any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        smartContextService.expandCallGraph(context, request);

        // Assert
        assertFalse(context.getRelationships().isEmpty());
    }

    @Test
    void testExpandAllWithEmptyContext() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextRequest request = new ContextRequest();
        request.setExpansionDepth(1);
        request.setIncludeRelationships(true);
        request.setIncludeCallGraph(true);
        request.setIncludeSpringBeans(true);
        request.setIncludeConfiguration(true);

        // Act - should not throw
        smartContextService.expandAll(context, request);

        // Assert
        assertTrue(context.getSymbols().isEmpty());
    }

    @Test
    void testExpandModulesWithNoModules() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setMaxSymbols(50);

        // Act - should not throw
        smartContextService.expandModules(context, request);

        // Assert
        assertTrue(context.getSymbols().isEmpty());
    }

    @Test
    void testExpandSpringBeans() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextEntry entry = new ContextEntry("symbol", "UserService",
                "com.example.UserService", 1);
        entry.setPackageName("com.example");
        context.getSymbols().add(entry);

        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setMaxSymbols(50);

        when(springSearchService.findBeans(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        // Act - should not throw
        smartContextService.expandSpringBeans(context, request);

        // Assert
        assertTrue(context.getSpringComponents().isEmpty());
    }

    @Test
    void testExpandInheritance() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextEntry entry = new ContextEntry("symbol", "BaseClass",
                "com.example.BaseClass", 1);
        context.getSymbols().add(entry);

        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setExpansionDepth(1);
        request.setMaxRelationships(50);

        RelationshipEntry inheritor = new RelationshipEntry();
        inheritor.setSourceSymbol("com.example.SubClass");
        inheritor.setRelationshipType(RelationshipType.INHERITANCE);

        when(relationshipSearchService.findInheritors(any(), any(), anyBoolean(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(inheritor));

        // Act
        smartContextService.expandInheritance(context, request);

        // Assert
        assertFalse(context.getRelationships().isEmpty());
        assertEquals("com.example.SubClass", context.getRelationships().get(0).getName());
    }

    @Test
    void testExpandInterfaces() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextEntry entry = new ContextEntry("symbol", "MyInterface",
                "com.example.MyInterface", 1);
        context.getSymbols().add(entry);

        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setExpansionDepth(1);
        request.setMaxRelationships(50);

        RelationshipEntry impl = new RelationshipEntry();
        impl.setSourceSymbol("com.example.MyImpl");
        impl.setRelationshipType(RelationshipType.IMPLEMENTATION);

        when(relationshipSearchService.findImplementations(any(), any(), anyBoolean(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(impl));

        // Act
        smartContextService.expandInterfaces(context, request);

        // Assert
        assertFalse(context.getRelationships().isEmpty());
        assertEquals("com.example.MyImpl", context.getRelationships().get(0).getName());
    }

    @Test
    void testExpandRestEndpointsWithNoSpringComponents() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setMaxRelationships(50);

        // Act - should not throw
        smartContextService.expandRestEndpoints(context, request);

        // Assert
        assertTrue(context.getSpringComponents().isEmpty());
    }

    @Test
    void testExpandConfiguration() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");
        request.setMaxSymbols(50);
        request.setModuleName("core");

        // Act - should not throw with empty context
        smartContextService.expandConfiguration(context, request);

        // Assert
        assertTrue(context.getConfigurations().isEmpty());
    }
}