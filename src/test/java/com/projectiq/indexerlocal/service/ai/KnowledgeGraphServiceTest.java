package com.projectiq.indexerlocal.service.ai;

import com.projectiq.indexerlocal.model.ai.KnowledgeGraph;
import com.projectiq.indexerlocal.model.ai.KnowledgeGraphNode;
import com.projectiq.indexerlocal.model.ai.KnowledgeGraphRelationship;
import com.projectiq.indexerlocal.service.*;
import com.projectiq.indexerlocal.service.ai.impl.RepositoryKnowledgeGraphServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private SymbolSearchService symbolSearchService;
    @Mock
    private SpringSearchService springSearchService;
    @Mock
    private RelationshipSearchService relationshipSearchService;
    @Mock
    private BuildSearchService buildSearchService;
    @Mock
    private RepositorySearchService repositorySearchService;

    private RepositoryKnowledgeGraphService knowledgeGraphService;

    @BeforeEach
    void setUp() {
        knowledgeGraphService = new RepositoryKnowledgeGraphServiceImpl(
                symbolSearchService, springSearchService, relationshipSearchService,
                buildSearchService, repositorySearchService);
    }

    @Test
    void testBuildGraph() {
        KnowledgeGraph graph = knowledgeGraphService.buildGraph("test-repo");
        assertNotNull(graph);
        assertEquals("test-repo", graph.getRepositoryId());
        // Should at least have the repository node
        assertTrue(graph.getTotalNodes() >= 1);
    }

    @Test
    void testGetGraph() {
        knowledgeGraphService.buildGraph("test-repo");
        KnowledgeGraph graph = knowledgeGraphService.getGraph("test-repo");
        assertNotNull(graph);
    }

    @Test
    void testGetGraphStatistics() {
        knowledgeGraphService.buildGraph("test-repo");
        KnowledgeGraph stats = knowledgeGraphService.getGraphStatistics("test-repo");
        assertNotNull(stats);
    }

    @Test
    void testDeleteGraph() {
        knowledgeGraphService.buildGraph("test-repo");
        knowledgeGraphService.deleteGraph("test-repo");
        KnowledgeGraph graph = knowledgeGraphService.getGraph("test-repo");
        assertNull(graph);
    }

    @Test
    void testIncrementalUpdate() {
        KnowledgeGraph graph = knowledgeGraphService.incrementalUpdate("test-repo");
        assertNotNull(graph);
    }

    @Test
    void testGraphNodeCreation() {
        KnowledgeGraphNode node = new KnowledgeGraphNode("test-node", "CLASS", "TestClass");
        node.setRepositoryId("test-repo");
        node.setPackageName("com.test");
        node.setLanguage("java");
        node.addProperty("visibility", "public");

        assertEquals("test-node", node.getId());
        assertEquals("CLASS", node.getType());
        assertEquals("TestClass", node.getName());
        assertEquals("test-repo", node.getRepositoryId());
        assertEquals("com.test", node.getPackageName());
        assertEquals("java", node.getLanguage());
        assertEquals("public", node.getProperties().get("visibility"));
    }

    @Test
    void testGraphRelationshipCreation() {
        KnowledgeGraphRelationship rel = new KnowledgeGraphRelationship(
                "rel-1", "source-node", "target-node", "CONTAINS");
        rel.setWeight(2);
        rel.setDescription("Test relationship");

        assertEquals("rel-1", rel.getId());
        assertEquals("source-node", rel.getSourceNodeId());
        assertEquals("target-node", rel.getTargetNodeId());
        assertEquals("CONTAINS", rel.getType());
        assertEquals(2, rel.getWeight());
        assertEquals("Test relationship", rel.getDescription());
    }

    @Test
    void testGraphAddNode() {
        KnowledgeGraph graph = new KnowledgeGraph("test-repo");
        KnowledgeGraphNode node = new KnowledgeGraphNode("node-1", "CLASS", "TestClass");
        graph.addNode(node);

        assertEquals(1, graph.getTotalNodes());
        assertEquals(1, graph.getNodes().size());
        assertTrue(graph.getNodeTypeCounts().containsKey("CLASS"));
        assertEquals(1L, graph.getNodeTypeCounts().get("CLASS"));
    }

    @Test
    void testGraphAddRelationship() {
        KnowledgeGraph graph = new KnowledgeGraph("test-repo");
        graph.addNode(new KnowledgeGraphNode("node-1", "CLASS", "TestClass"));
        graph.addNode(new KnowledgeGraphNode("node-2", "METHOD", "testMethod"));

        KnowledgeGraphRelationship rel = new KnowledgeGraphRelationship(
                "rel-1", "node-1", "node-2", "CONTAINS");
        graph.addRelationship(rel);

        assertEquals(1, graph.getTotalRelationships());
        assertTrue(graph.getRelationshipTypeCounts().containsKey("CONTAINS"));
    }

    @Test
    void testGraphCalculateStatistics() {
        KnowledgeGraph graph = new KnowledgeGraph("test-repo");
        graph.addNode(new KnowledgeGraphNode("n1", "CLASS", "C1"));
        graph.addNode(new KnowledgeGraphNode("n2", "CLASS", "C2"));
        graph.addNode(new KnowledgeGraphNode("n3", "METHOD", "M1"));
        graph.addRelationship(new KnowledgeGraphRelationship("r1", "n1", "n2", "REFERENCES"));
        graph.addRelationship(new KnowledgeGraphRelationship("r2", "n2", "n3", "CONTAINS"));

        graph.calculateStatistics();

        assertEquals(3, graph.getTotalNodes());
        assertEquals(2, graph.getTotalRelationships());
        assertEquals(2L, graph.getNodeTypeCounts().get("CLASS"));
        assertEquals(1L, graph.getNodeTypeCounts().get("METHOD"));
    }
}