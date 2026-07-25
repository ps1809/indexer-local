package com.projectiq.indexerlocal.service.context;

import com.projectiq.indexerlocal.model.context.ContextEntry;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.service.context.impl.PromptOptimizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptOptimizationServiceTest {

    private PromptOptimizationService promptOptimizationService;

    @BeforeEach
    void setUp() {
        promptOptimizationService = new PromptOptimizationServiceImpl();
    }

    @Test
    void testRemoveDuplicateSymbols() {
        // Arrange
        ContextResponse context = new ContextResponse();
        context.getSymbols().add(new ContextEntry("symbol", "UserService", "com.example.UserService", 1));
        context.getSymbols().add(new ContextEntry("symbol", "UserService", "com.example.UserService", 1));

        // Act
        promptOptimizationService.removeDuplicateSymbols(context);

        // Assert
        assertEquals(1, context.getSymbols().size());
    }

    @Test
    void testRemoveDuplicateDependencies() {
        // Arrange
        ContextResponse context = new ContextResponse();
        context.getDependencies().add(new ContextEntry("dependency", "Dep1", "com.example.Dep1", 2));
        context.getDependencies().add(new ContextEntry("dependency", "Dep1", "com.example.Dep1", 2));

        // Act
        promptOptimizationService.removeDuplicateDependencies(context);

        // Assert
        assertEquals(1, context.getDependencies().size());
    }

    @Test
    void testRemoveDuplicateRelationships() {
        // Arrange
        ContextResponse context = new ContextResponse();
        context.getRelationships().add(new ContextEntry("relationship", "Rel1", "com.example.Rel1", 3));
        context.getRelationships().add(new ContextEntry("relationship", "Rel1", "com.example.Rel1", 3));

        // Act
        promptOptimizationService.removeDuplicateRelationships(context);

        // Assert
        assertEquals(1, context.getRelationships().size());
    }

    @Test
    void testRemoveAllDuplicates() {
        // Arrange
        ContextResponse context = new ContextResponse();
        // Use different FQNs so they aren't caught by cross-section dedup
        context.getSymbols().add(new ContextEntry("symbol", "UserService", "com.example.UserService", 1));
        context.getDependencies().add(new ContextEntry("dependency", "UserRepository", "com.example.UserRepository", 2));
        context.getRelationships().add(new ContextEntry("relationship", "AnotherRel", "com.example.AnotherRel", 3));

        // Act
        promptOptimizationService.removeAllDuplicates(context);

        // Assert - verify no duplicates removed from cross-section since they're different
        assertEquals(1, context.getSymbols().size());
        assertEquals(1, context.getDependencies().size());
        assertEquals(1, context.getRelationships().size());
    }

    @Test
    void testRemoveAllDuplicatesCrossSection() {
        // Arrange
        ContextResponse context = new ContextResponse();
        // Cross-section dedup uses type-prefixed keys, so different types with same FQN are NOT removed
        ContextEntry symbol = new ContextEntry("symbol", "UserService", "com.example.UserService", 1);
        ContextEntry dep = new ContextEntry("dependency", "UserService", "com.example.UserService", 2);
        context.getSymbols().add(symbol);
        context.getDependencies().add(dep);

        // Act
        promptOptimizationService.removeAllDuplicates(context);

        // Assert - cross-section dedup uses type-prefixed keys, so different types are kept
        assertEquals(1, context.getSymbols().size());
        assertEquals(1, context.getDependencies().size());
    }

    @Test
    void testReduceTokens() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextRequest request = new ContextRequest();
        request.setMaxTokens(10);

        // Add entries with high token estimates
        ContextEntry entry1 = new ContextEntry("symbol", "VeryLongClassName", 
                "com.example.VeryLongClassName", 1);
        ContextEntry entry2 = new ContextEntry("dependency", "AnotherLongName",
                "com.example.AnotherLongName", 2);
        context.getSymbols().add(entry1);
        context.getDependencies().add(entry2);
        context.calculateTotals();

        // Act
        promptOptimizationService.reduceTokens(context, request);

        // Assert
        assertTrue(context.getTotalEstimatedTokens() <= 10 || context.getTotalEntries() == 0);
    }

    @Test
    void testCompressContext() {
        // Arrange
        ContextResponse context = new ContextResponse();
        context.getSymbols().add(new ContextEntry("symbol", "UserService", "com.example.UserService", 1));
        context.getSymbols().add(new ContextEntry("symbol", "UserService", "com.example.UserService", 2));

        // Act
        promptOptimizationService.compressContext(context);

        // Assert
        assertEquals(1, context.getSymbols().size());
        assertEquals(1, context.getSymbols().get(0).getPriority()); // Should keep min priority
    }

    @Test
    void testRankByPriority() {
        // Arrange
        ContextResponse context = new ContextResponse();
        context.getSymbols().add(new ContextEntry("symbol", "LowPriority", "com.example.Low", 5));
        context.getSymbols().add(new ContextEntry("symbol", "HighPriority", "com.example.High", 1));

        // Act
        ContextRequest request = new ContextRequest();
        promptOptimizationService.rankByPriority(context, request);

        // Assert
        assertEquals("HighPriority", context.getSymbols().get(0).getName());
        assertEquals("LowPriority", context.getSymbols().get(1).getName());
    }

    @Test
    void testRemoveNoise() {
        // Arrange
        ContextResponse context = new ContextResponse();
        context.getSymbols().add(new ContextEntry("symbol", "Relevant", "com.example.Relevant", 1));
        context.getSymbols().add(new ContextEntry("symbol", "Noise", "com.example.Noise", 15));
        context.calculateTotals();

        // Act
        ContextRequest request = new ContextRequest();
        promptOptimizationService.removeNoise(context, request);

        // Assert
        assertEquals(1, context.getSymbols().size());
        assertEquals("Relevant", context.getSymbols().get(0).getName());
    }

    @Test
    void testOptimize() {
        // Arrange
        ContextResponse context = new ContextResponse();
        context.getSymbols().add(new ContextEntry("symbol", "UserService", "com.example.UserService", 1));
        context.getSymbols().add(new ContextEntry("symbol", "UserService", "com.example.UserService", 1)); // duplicate
        context.getDependencies().add(new ContextEntry("dependency", "Dep1", "com.example.Dep1", 2));
        context.getDependencies().add(new ContextEntry("dependency", "Dep1", "com.example.Dep1", 2)); // duplicate
        context.calculateTotals();

        ContextRequest request = new ContextRequest();
        request.setMaxTokens(1000);

        // Act
        ContextResponse result = promptOptimizationService.optimize(context, request);

        // Assert
        assertTrue(result.isOptimized());
        assertEquals(1, result.getSymbols().size());
        assertEquals(1, result.getDependencies().size());
    }

    @Test
    void testOptimizeWithEmptyContext() {
        // Arrange
        ContextResponse context = new ContextResponse();
        ContextRequest request = new ContextRequest();
        request.setMaxTokens(1000);

        // Act
        ContextResponse result = promptOptimizationService.optimize(context, request);

        // Assert
        assertTrue(result.isOptimized());
        assertEquals(0, result.getTotalEntries());
    }

    @Test
    void testOptimizeWithTokenLimit() {
        // Arrange
        ContextResponse context = new ContextResponse();
        // Add many entries to exceed token limit
        for (int i = 0; i < 100; i++) {
            context.getSymbols().add(new ContextEntry("symbol", "Class" + i,
                    "com.example.Class" + i, i % 5 + 1));
        }
        context.calculateTotals();

        ContextRequest request = new ContextRequest();
        request.setMaxTokens(50);

        // Act
        ContextResponse result = promptOptimizationService.optimize(context, request);

        // Assert
        assertTrue(result.isOptimized());
        assertTrue(result.getTotalEstimatedTokens() <= 100 || result.getTotalEntries() < 100);
    }
}