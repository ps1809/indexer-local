package com.projectiq.indexerlocal.service.ai.impl;

import com.projectiq.indexerlocal.model.ai.*;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.model.symbol.SymbolEntry;
import com.projectiq.indexerlocal.service.*;
import com.projectiq.indexerlocal.service.ai.*;
import com.projectiq.indexerlocal.service.context.ContextBuilderService;
import com.projectiq.indexerlocal.service.context.PromptOptimizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AIContextService.
 * Implements the full AI retrieval pipeline: symbol search -> vector search -> knowledge graph -> context engine -> prompt optimization.
 */
@Service
public class AIContextServiceImpl implements AIContextService {

    private static final Logger log = LoggerFactory.getLogger(AIContextServiceImpl.class);

    private final SymbolSearchService symbolSearchService;
    private final VectorSearchService vectorSearchService;
    private final RepositoryKnowledgeGraphService knowledgeGraphService;
    private final ContextBuilderService contextBuilderService;
    private final PromptOptimizationService promptOptimizationService;
    private final ChunkGenerationService chunkGenerationService;
    private final SpringSearchService springSearchService;
    private final RepositorySearchService repositorySearchService;
    private final RelationshipSearchService relationshipSearchService;

    public AIContextServiceImpl(
            SymbolSearchService symbolSearchService,
            VectorSearchService vectorSearchService,
            RepositoryKnowledgeGraphService knowledgeGraphService,
            ContextBuilderService contextBuilderService,
            PromptOptimizationService promptOptimizationService,
            ChunkGenerationService chunkGenerationService,
            SpringSearchService springSearchService,
            RepositorySearchService repositorySearchService,
            RelationshipSearchService relationshipSearchService) {
        this.symbolSearchService = symbolSearchService;
        this.vectorSearchService = vectorSearchService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.contextBuilderService = contextBuilderService;
        this.promptOptimizationService = promptOptimizationService;
        this.chunkGenerationService = chunkGenerationService;
        this.springSearchService = springSearchService;
        this.repositorySearchService = repositorySearchService;
        this.relationshipSearchService = relationshipSearchService;
    }

    @Override
    public AIContextResult buildContext(AIContextRequest request) {
        log.info("Building AI context: query='{}', repositoryId={}", request.getQuery(), request.getRepositoryId());
        AIContextResult result = new AIContextResult();
        result.setQuery(request.getQuery());
        result.setRepositoryId(request.getRepositoryId());

        // Step 1: Symbol Search
        if (request.isIncludeSymbolSearch()) {
            performSymbolSearch(request, result);
        }

        // Step 2: Vector Search
        if (request.isIncludeVectorSearch()) {
            performVectorSearch(request, result);
        }

        // Step 3: Knowledge Graph Expansion
        if (request.isIncludeKnowledgeGraph()) {
            performKnowledgeGraphExpansion(request, result);
        }

        // Step 4: Context Engine
        performContextEngine(request, result);

        // Step 5: Repository Summary
        if (request.isIncludeRepositorySummary()) {
            addRepositorySummary(request, result);
        }

        // Step 6: Collect related data
        if (request.isIncludeSpringComponents()) {
            addSpringComponents(request, result);
        }

        if (request.isIncludeRestApis()) {
            addRestApis(request, result);
        }

        if (request.isIncludeConfiguration()) {
            addConfiguration(request, result);
        }

        if (request.isIncludeDocumentation()) {
            addDocumentation(request, result);
        }

        // Calculate totals
        result.setTotalEntries(countTotalEntries(result));
        result.setEstimatedTokens(estimateTokens(result));

        log.info("AI context built: {} entries, {} estimated tokens",
                result.getTotalEntries(), result.getEstimatedTokens());
        return result;
    }

    @Override
    public AIContextResult buildSymbolContext(AIContextRequest request) {
        log.info("Building AI symbol context: symbol={}, type={}", request.getSymbolName(), request.getSymbolType());
        request.setQuery(request.getSymbolName());
        request.setIncludeVectorSearch(true);
        request.setIncludeKnowledgeGraph(true);
        return buildContext(request);
    }

    @Override
    public AIContextResult buildFileContext(AIContextRequest request) {
        log.info("Building AI file context: filePath={}", request.getFilePath());
        request.setQuery(request.getFilePath());
        request.setIncludeVectorSearch(true);
        request.setIncludeKnowledgeGraph(true);
        return buildContext(request);
    }

    @Override
    public AIContextResult buildPackageContext(AIContextRequest request) {
        log.info("Building AI package context: package={}", request.getPackageName());
        request.setQuery(request.getPackageName());
        request.setIncludeVectorSearch(true);
        request.setIncludeKnowledgeGraph(true);
        return buildContext(request);
    }

    @Override
    public AIContextResult buildModuleContext(AIContextRequest request) {
        log.info("Building AI module context: module={}", request.getModuleName());
        request.setQuery(request.getModuleName());
        request.setIncludeVectorSearch(true);
        request.setIncludeKnowledgeGraph(true);
        return buildContext(request);
    }

    @Override
    public AIContextResult buildRepositoryContext(AIContextRequest request) {
        log.info("Building AI repository context: repositoryId={}", request.getRepositoryId());
        request.setIncludeVectorSearch(true);
        request.setIncludeKnowledgeGraph(true);
        return buildContext(request);
    }

    // ==================== Pipeline Steps ====================

    private void performSymbolSearch(AIContextRequest request, AIContextResult result) {
        try {
            String query = request.getQuery() != null ? request.getQuery() : "";
            String symbolName = request.getSymbolName() != null ? request.getSymbolName() : query;

            var symbolResult = symbolSearchService.searchSymbols(
                    request.getRepositoryId(), symbolName, "PARTIAL",
                    request.getSymbolType(), request.getPackageName(),
                    null, request.getModuleName(),
                    0, request.getTopK());

            if (symbolResult != null && symbolResult.getContent() != null) {
                for (var entry : symbolResult.getContent()) {
                    result.getRelatedClasses().add(entry.getFullyQualifiedName() != null ?
                            entry.getFullyQualifiedName() : entry.getSymbolName());
                    if (entry.getFilePath() != null) {
                        result.getRelevantFiles().add(entry.getFilePath());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Symbol search failed: {}", e.getMessage());
        }
    }

    private void performVectorSearch(AIContextRequest request, AIContextResult result) {
        try {
            if (request.getQuery() == null || request.getQuery().isEmpty()) return;

            var vectorResults = vectorSearchService.semanticSearch(
                    request.getRepositoryId(), request.getQuery(),
                    request.getTopK(), request.getMinSimilarityScore());

            for (var vr : vectorResults) {
                if (vr.getChunk() != null) {
                    result.getSemanticMatches().add(vr.getChunk());
                    String symbol = vr.getChunk().getSymbol();
                    if (symbol != null) {
                        if ("CLASS".equals(vr.getChunk().getChunkType())) {
                            result.getRelatedClasses().add(symbol);
                        } else if ("METHOD".equals(vr.getChunk().getChunkType())) {
                            result.getRelatedMethods().add(symbol);
                        }
                    }
                    if (vr.getChunk().getRelationships() != null && vr.getChunk().getRelationships().containsKey("file")) {
                        result.getRelevantFiles().add(vr.getChunk().getRelationships().get("file"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Vector search failed: {}", e.getMessage());
        }
    }

    private void performKnowledgeGraphExpansion(AIContextRequest request, AIContextResult result) {
        try {
            var graph = knowledgeGraphService.getGraph(request.getRepositoryId());
            if (graph == null) {
                graph = knowledgeGraphService.buildGraph(request.getRepositoryId());
            }

            result.getGraphNodes().addAll(graph.getNodes());
            result.getGraphRelationships().addAll(graph.getRelationships());

            // Extract related classes from graph
            if (!result.getRelatedClasses().isEmpty()) {
                String startClass = result.getRelatedClasses().get(0);
                String nodeId = "class:" + request.getRepositoryId() + ":" + startClass;
                var traversed = knowledgeGraphService.traverseGraph(request.getRepositoryId(), nodeId, request.getMaxGraphDepth());
                if (traversed != null) {
                    for (var node : traversed.getNodes()) {
                        if ("CLASS".equals(node.getType()) && node.getName() != null) {
                            result.getRelatedClasses().add(node.getFullyQualifiedName() != null ?
                                    node.getFullyQualifiedName() : node.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Knowledge graph expansion failed: {}", e.getMessage());
        }
    }

    private void performContextEngine(AIContextRequest request, AIContextResult result) {
        try {
            ContextRequest ctxRequest = new ContextRequest();
            ctxRequest.setRepositoryId(request.getRepositoryId());
            ctxRequest.setQuery(request.getQuery());
            ctxRequest.setSymbolName(request.getSymbolName());
            ctxRequest.setSymbolType(request.getSymbolType());
            ctxRequest.setPackageName(request.getPackageName());
            ctxRequest.setModuleName(request.getModuleName());
            ctxRequest.setMaxTokens(request.getMaxTokens());

            ContextResponse ctxResponse = contextBuilderService.buildContext(ctxRequest);
            if (ctxResponse != null) {
                ctxResponse.getDependencies().forEach(d -> result.getDependencies().add(d.getFullyQualifiedName()));
            }
        } catch (Exception e) {
            log.warn("Context engine failed: {}", e.getMessage());
        }
    }

    private void addRepositorySummary(AIContextRequest request, AIContextResult result) {
        try {
            var repoResult = repositorySearchService.findRepositories(
                    request.getRepositoryId(), null, null, null, null, "EXACT", 0, 1);
            if (repoResult != null && repoResult.getContent() != null && !repoResult.getContent().isEmpty()) {
                var repo = repoResult.getContent().get(0);
                result.setRepositorySummary("Repository: " + repo.getRepositoryName() +
                        "\nPath: " + repo.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Failed to get repository summary: {}", e.getMessage());
        }
    }

    private void addSpringComponents(AIContextRequest request, AIContextResult result) {
        try {
            var controllers = springSearchService.findControllers(
                    request.getRepositoryId(), request.getPackageName(), request.getModuleName(), 0, 50);
            if (controllers != null) {
                controllers.forEach(c -> result.getSpringComponents().add(c.getClassName()));
            }

            var services = springSearchService.findServices(
                    request.getRepositoryId(), request.getPackageName(), request.getModuleName(), 0, 50);
            if (services != null) {
                services.forEach(s -> result.getSpringComponents().add(s.getClassName()));
            }
        } catch (Exception e) {
            log.warn("Failed to get Spring components: {}", e.getMessage());
        }
    }

    private void addRestApis(AIContextRequest request, AIContextResult result) {
        try {
            var endpoints = springSearchService.findEndpoints(
                    request.getRepositoryId(), null, null, null,
                    request.getPackageName(), request.getModuleName(), 0, 50);
            if (endpoints != null) {
                endpoints.forEach(ep -> {
                    String apiDesc = ep.getHttpMethod() + " " + ep.getRestPath() + " (" + ep.getClassName() + ")";
                    result.getRestApis().add(apiDesc);
                });
            }
        } catch (Exception e) {
            log.warn("Failed to get REST APIs: {}", e.getMessage());
        }
    }

    private void addConfiguration(AIContextRequest request, AIContextResult result) {
        try {
            var configs = springSearchService.findConfigurationClasses(
                    request.getRepositoryId(), request.getPackageName(), request.getModuleName(), 0, 50);
            if (configs != null) {
                configs.forEach(c -> result.getConfigurations().add(c.getClassName()));
            }
        } catch (Exception e) {
            log.warn("Failed to get configuration: {}", e.getMessage());
        }
    }

    private void addDocumentation(AIContextRequest request, AIContextResult result) {
        try {
            var docChunks = chunkGenerationService.getChunks(request.getRepositoryId()).stream()
                    .filter(c -> "DOCUMENTATION".equals(c.getChunkType()))
                    .collect(Collectors.toList());
            docChunks.forEach(c -> result.getDocumentation().add(c.getSymbol()));
        } catch (Exception e) {
            log.warn("Failed to get documentation: {}", e.getMessage());
        }
    }

    private int countTotalEntries(AIContextResult result) {
        int count = 0;
        count += result.getRelevantFiles().size();
        count += result.getSemanticMatches().size();
        count += result.getRelatedClasses().size();
        count += result.getRelatedMethods().size();
        count += result.getDependencies().size();
        count += result.getSpringComponents().size();
        count += result.getRestApis().size();
        count += result.getConfigurations().size();
        count += result.getDocumentation().size();
        count += result.getGraphNodes().size();
        count += result.getGraphRelationships().size();
        return count;
    }

    private int estimateTokens(AIContextResult result) {
        int tokens = 0;
        for (var s : result.getRelevantFiles()) tokens += estimateStringTokens(s);
        for (var c : result.getSemanticMatches()) tokens += estimateStringTokens(c.getContent());
        for (var s : result.getRelatedClasses()) tokens += estimateStringTokens(s);
        for (var s : result.getRelatedMethods()) tokens += estimateStringTokens(s);
        for (var s : result.getDependencies()) tokens += estimateStringTokens(s);
        for (var s : result.getSpringComponents()) tokens += estimateStringTokens(s);
        for (var s : result.getRestApis()) tokens += estimateStringTokens(s);
        for (var s : result.getConfigurations()) tokens += estimateStringTokens(s);
        for (var s : result.getDocumentation()) tokens += estimateStringTokens(s);
        if (result.getRepositorySummary() != null) tokens += estimateStringTokens(result.getRepositorySummary());
        return tokens;
    }

    private int estimateStringTokens(String text) {
        if (text == null) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }
}