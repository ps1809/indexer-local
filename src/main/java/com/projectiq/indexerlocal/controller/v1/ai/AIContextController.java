package com.projectiq.indexerlocal.controller.v1.ai;

import com.projectiq.indexerlocal.model.ai.*;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.service.ai.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the AI Knowledge Layer API.
 * Provides endpoints for chunk generation, embeddings, vector search,
 * knowledge graph, and AI context building.
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Knowledge Layer API", description = "Semantic chunks, embeddings, vector search, knowledge graph, and AI context generation")
public class AIContextController {

    private static final Logger log = LoggerFactory.getLogger(AIContextController.class);

    private final ChunkGenerationService chunkGenerationService;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final RepositoryKnowledgeGraphService knowledgeGraphService;
    private final AIContextService aiContextService;

    public AIContextController(
            ChunkGenerationService chunkGenerationService,
            EmbeddingService embeddingService,
            VectorSearchService vectorSearchService,
            RepositoryKnowledgeGraphService knowledgeGraphService,
            AIContextService aiContextService) {
        this.chunkGenerationService = chunkGenerationService;
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.aiContextService = aiContextService;
    }

    // ==================== AI Context ====================

    @PostMapping("/context")
    @Operation(summary = "Build AI context", description = "Build complete AI retrieval context using the full pipeline: symbol search -> vector search -> knowledge graph -> context engine -> prompt optimization")
    public ResponseEntity<ApiResponse<AIContextResult>> buildContext(@Valid @RequestBody AIContextRequest request) {
        log.info("POST /api/ai/context - query='{}', repositoryId={}", request.getQuery(), request.getRepositoryId());
        AIContextResult result = aiContextService.buildContext(request);
        return ResponseEntity.ok(ApiResponse.success("AI context built successfully", result));
    }

    // ==================== Semantic Search ====================

    @PostMapping("/search")
    @Operation(summary = "Semantic search", description = "Perform semantic search across indexed repository chunks")
    public ResponseEntity<ApiResponse<List<VectorSearchResult>>> search(@Valid @RequestBody AISearchRequest request) {
        log.info("POST /api/ai/search - query='{}', type={}", request.getQuery(), request.getSearchType());
        List<VectorSearchResult> results = switch (request.getSearchType() != null ? request.getSearchType() : "semantic") {
            case "code" -> vectorSearchService.similarCodeSearch(
                    request.getRepositoryId(), request.getQuery(), request.getTopK(), request.getMinSimilarityScore());
            case "documentation" -> vectorSearchService.similarDocumentationSearch(
                    request.getRepositoryId(), request.getQuery(), request.getTopK(), request.getMinSimilarityScore());
            case "filtered" -> vectorSearchService.searchWithFilters(
                    request.getRepositoryId(), request.getQuery(), request.getModule(),
                    request.getPackageName(), request.getLanguage(), request.getChunkType(),
                    request.getTopK(), request.getMinSimilarityScore());
            default -> vectorSearchService.semanticSearch(
                    request.getRepositoryId(), request.getQuery(), request.getTopK(), request.getMinSimilarityScore());
        };
        return ResponseEntity.ok(ApiResponse.success("Search completed", results));
    }

    // ==================== Chunks ====================

    @PostMapping("/chunks")
    @Operation(summary = "Generate chunks", description = "Generate AI-ready semantic chunks from indexed repository data")
    public ResponseEntity<ApiResponse<AIResponse>> generateChunks(@Valid @RequestBody AIChunkRequest request) {
        log.info("POST /api/ai/chunks - repositoryId={}, incremental={}", request.getRepositoryId(), request.isIncremental());

        List<Chunk> chunks;
        if (request.isForceRegenerate()) {
            chunkGenerationService.deleteChunks(request.getRepositoryId());
            chunks = chunkGenerationService.generateAllChunks(request.getRepositoryId());
        } else if (request.isIncremental()) {
            chunks = chunkGenerationService.incrementalUpdate(request.getRepositoryId());
        } else {
            chunks = chunkGenerationService.generateAllChunks(request.getRepositoryId());
        }

        AIResponse response = new AIResponse("chunk_generation", "completed");
        response.setRepositoryId(request.getRepositoryId());
        response.setChunks(chunks);
        response.setTotalCount(chunks.size());
        response.setMessage("Generated " + chunks.size() + " chunks");

        return ResponseEntity.ok(ApiResponse.success("Chunks generated successfully", response));
    }

    @GetMapping("/chunks/{repositoryId}")
    @Operation(summary = "Get chunks", description = "Get all chunks for a repository")
    public ResponseEntity<ApiResponse<List<Chunk>>> getChunks(@PathVariable String repositoryId) {
        log.info("GET /api/ai/chunks/{}", repositoryId);
        List<Chunk> chunks = chunkGenerationService.getChunks(repositoryId);
        return ResponseEntity.ok(ApiResponse.success("Chunks retrieved", chunks));
    }

    // ==================== Embeddings ====================

    @PostMapping("/embeddings")
    @Operation(summary = "Generate embeddings", description = "Generate embeddings for repository chunks")
    public ResponseEntity<ApiResponse<AIResponse>> generateEmbeddings(@Valid @RequestBody AIEmbeddingRequest request) {
        log.info("POST /api/ai/embeddings - repositoryId={}, incremental={}", request.getRepositoryId(), request.isIncremental());

        List<Embedding> embeddings;
        if (request.isForceRegenerate()) {
            embeddingService.deleteEmbeddings(request.getRepositoryId());
            embeddings = embeddingService.generateAllEmbeddings(request.getRepositoryId());
        } else if (request.isIncremental()) {
            embeddings = embeddingService.incrementalUpdate(request.getRepositoryId());
        } else {
            embeddings = embeddingService.generateAllEmbeddings(request.getRepositoryId());
        }

        AIResponse response = new AIResponse("embedding_generation", "completed");
        response.setRepositoryId(request.getRepositoryId());
        response.setEmbeddings(embeddings);
        response.setTotalCount(embeddings.size());
        response.setMessage("Generated " + embeddings.size() + " embeddings");

        return ResponseEntity.ok(ApiResponse.success("Embeddings generated successfully", response));
    }

    // ==================== Vector Search ====================

    @PostMapping("/vector-search")
    @Operation(summary = "Vector search", description = "Perform vector similarity search across repository chunks")
    public ResponseEntity<ApiResponse<List<VectorSearchResult>>> vectorSearch(@Valid @RequestBody AISearchRequest request) {
        log.info("POST /api/ai/vector-search - query='{}', topK={}", request.getQuery(), request.getTopK());
        List<VectorSearchResult> results = vectorSearchService.semanticSearch(
                request.getRepositoryId(), request.getQuery(), request.getTopK(), request.getMinSimilarityScore());
        return ResponseEntity.ok(ApiResponse.success("Vector search completed", results));
    }

    // ==================== Knowledge Graph ====================

    @PostMapping("/knowledge-graph")
    @Operation(summary = "Build knowledge graph", description = "Build or retrieve the repository knowledge graph")
    public ResponseEntity<ApiResponse<AIResponse>> knowledgeGraph(@Valid @RequestBody KnowledgeGraphRequest request) {
        log.info("POST /api/ai/knowledge-graph - repositoryId={}", request.getRepositoryId());

        KnowledgeGraph graph;
        if (request.isIncremental()) {
            graph = knowledgeGraphService.incrementalUpdate(request.getRepositoryId());
        } else {
            graph = knowledgeGraphService.buildGraph(request.getRepositoryId());
        }

        AIResponse response = new AIResponse("knowledge_graph", "completed");
        response.setRepositoryId(request.getRepositoryId());
        response.setKnowledgeGraph(graph);
        response.setTotalCount((int) (graph.getTotalNodes() + graph.getTotalRelationships()));
        response.setMessage("Knowledge graph built: " + graph.getTotalNodes() + " nodes, " + graph.getTotalRelationships() + " relationships");

        return ResponseEntity.ok(ApiResponse.success("Knowledge graph built successfully", response));
    }

    @GetMapping("/knowledge-graph/{repositoryId}")
    @Operation(summary = "Get knowledge graph", description = "Get the knowledge graph for a repository")
    public ResponseEntity<ApiResponse<KnowledgeGraph>> getKnowledgeGraph(@PathVariable String repositoryId) {
        log.info("GET /api/ai/knowledge-graph/{}", repositoryId);
        KnowledgeGraph graph = knowledgeGraphService.getGraph(repositoryId);
        if (graph == null) {
            return ResponseEntity.ok(ApiResponse.success("No knowledge graph found for repository", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Knowledge graph retrieved", graph));
    }

    @GetMapping("/knowledge-graph/{repositoryId}/statistics")
    @Operation(summary = "Get graph statistics", description = "Get knowledge graph statistics for a repository")
    public ResponseEntity<ApiResponse<KnowledgeGraph>> getGraphStatistics(@PathVariable String repositoryId) {
        log.info("GET /api/ai/knowledge-graph/{}/statistics", repositoryId);
        KnowledgeGraph stats = knowledgeGraphService.getGraphStatistics(repositoryId);
        return ResponseEntity.ok(ApiResponse.success("Graph statistics retrieved", stats));
    }
}