package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.service.context.ContextBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Context Engine API.
 * Provides endpoints to build, expand, and optimize development context
 * from indexed repository data.
 */
@RestController
@RequestMapping("/api/context")
@Tag(name = "Context Engine API", description = "Build, expand, and optimize AI-ready development context from indexed repositories")
public class ContextController {

    private static final Logger log = LoggerFactory.getLogger(ContextController.class);

    private final ContextBuilderService contextBuilderService;

    public ContextController(ContextBuilderService contextBuilderService) {
        this.contextBuilderService = contextBuilderService;
    }

    @PostMapping("/build")
    @Operation(summary = "Build context", description = "Build complete development context from search results")
    public ResponseEntity<ApiResponse<ContextResponse>> buildContext(@Valid @RequestBody ContextRequest request) {
        log.info("POST /api/context/build - query={}, type={}", request.getQuery(), request.getContextType());
        ContextResponse response = contextBuilderService.buildContext(request);
        return ResponseEntity.ok(ApiResponse.success("Context built successfully", response));
    }

    @PostMapping("/symbol")
    @Operation(summary = "Build symbol context", description = "Build context focused on a specific symbol")
    public ResponseEntity<ApiResponse<ContextResponse>> buildSymbolContext(@Valid @RequestBody ContextRequest request) {
        log.info("POST /api/context/symbol - symbol={}, type={}", request.getSymbolName(), request.getSymbolType());
        request.setContextType("symbol");
        ContextResponse response = contextBuilderService.buildSymbolContext(request);
        return ResponseEntity.ok(ApiResponse.success("Symbol context built successfully", response));
    }

    @PostMapping("/file")
    @Operation(summary = "Build file context", description = "Build context focused on a specific file")
    public ResponseEntity<ApiResponse<ContextResponse>> buildFileContext(@Valid @RequestBody ContextRequest request) {
        log.info("POST /api/context/file - filePath={}", request.getFilePath());
        request.setContextType("file");
        ContextResponse response = contextBuilderService.buildFileContext(request);
        return ResponseEntity.ok(ApiResponse.success("File context built successfully", response));
    }

    @PostMapping("/module")
    @Operation(summary = "Build module context", description = "Build context focused on a specific module")
    public ResponseEntity<ApiResponse<ContextResponse>> buildModuleContext(@Valid @RequestBody ContextRequest request) {
        log.info("POST /api/context/module - module={}", request.getModuleName());
        request.setContextType("module");
        ContextResponse response = contextBuilderService.buildModuleContext(request);
        return ResponseEntity.ok(ApiResponse.success("Module context built successfully", response));
    }

    @PostMapping("/repository")
    @Operation(summary = "Build repository context", description = "Build context for an entire repository")
    public ResponseEntity<ApiResponse<ContextResponse>> buildRepositoryContext(@Valid @RequestBody ContextRequest request) {
        log.info("POST /api/context/repository - repositoryId={}", request.getRepositoryId());
        request.setContextType("repository");
        ContextResponse response = contextBuilderService.buildRepositoryContext(request);
        return ResponseEntity.ok(ApiResponse.success("Repository context built successfully", response));
    }

    @PostMapping("/prompt")
    @Operation(summary = "Build optimized prompt context", description = "Build and optimize context for AI prompt consumption")
    public ResponseEntity<ApiResponse<ContextResponse>> buildPromptContext(@Valid @RequestBody ContextRequest request) {
        log.info("POST /api/context/prompt - query={}, type={}", request.getQuery(), request.getContextType());
        ContextResponse response = contextBuilderService.buildContext(request);
        return ResponseEntity.ok(ApiResponse.success("Optimized prompt context built successfully", response));
    }
}