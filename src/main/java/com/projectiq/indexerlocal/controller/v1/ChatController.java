package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.chat.*;
import com.projectiq.indexerlocal.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * v1 REST controller for Repository Chat APIs.
 * Exposes indexed repository information for future AI integrations.
 * All operations query only persisted indexed metadata.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/chat")
@Tag(name = "Repository Chat API (v1)", description = "Repository Chat APIs for retrieving indexed repository context and entities for AI integrations")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Generate structured repository context.
     * POST /api/v1/repositories/{repositoryId}/chat/context
     */
    @PostMapping("/context")
    @Operation(summary = "Generate structured repository context",
            description = "Generates structured context from persisted indexed metadata for a repository. " +
                    "Retrieves matching entities, repository metadata, project summary, technology stack, " +
                    "build system, configuration summary, database summary, Spring component summary, " +
                    "REST API summary, and repository statistics. Does not integrate with any AI model.")
    public ResponseEntity<ApiResponse<ChatContextResponse>> generateContext(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Valid @RequestBody(required = false) ChatContextRequest request) {

        long startTime = System.currentTimeMillis();
        log.info("Chat context API called: repositoryId={}", repositoryId);

        if (request == null) {
            request = new ChatContextRequest();
        }

        ChatContextResponse response = chatService.generateContext(repositoryId, request);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Chat context API completed: repositoryId={}, duration={}ms", repositoryId, duration);

        return ResponseEntity.ok(ApiResponse.success("Repository context generated successfully", response));
    }

    /**
     * Retrieve indexed entities matching supplied criteria.
     * POST /api/v1/repositories/{repositoryId}/chat/search
     */
    @PostMapping("/search")
    @Operation(summary = "Search indexed entities",
            description = "Retrieves indexed entities matching supplied criteria. " +
                    "Supports exact search, partial search, case-insensitive search, and filter by entity type. " +
                    "Entity types: CLASS, INTERFACE, ENUM, RECORD, METHOD, FIELD, ANNOTATION, COMPONENT, " +
                    "ENDPOINT, DEPENDENCY, CONFIGURATION, DATABASE_ARTIFACT. Does not parse source code.")
    public ResponseEntity<ApiResponse<ChatSearchResponse>> searchEntities(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId,
            @Valid @RequestBody(required = false) ChatSearchRequest request) {

        long startTime = System.currentTimeMillis();
        log.info("Chat search API called: repositoryId={}", repositoryId);

        if (request == null) {
            request = new ChatSearchRequest();
        }

        ChatSearchResponse response = chatService.searchEntities(repositoryId, request);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Chat search API completed: repositoryId={}, results={}, duration={}ms",
                repositoryId, response.getTotalResults(), duration);

        return ResponseEntity.ok(ApiResponse.success("Entity search completed successfully", response));
    }

    /**
     * Retrieve repository chat request history.
     * GET /api/v1/repositories/{repositoryId}/chat/history
     */
    @GetMapping("/history")
    @Operation(summary = "Retrieve chat request history",
            description = "Retrieves the chat request history for a repository. " +
                    "Returns all persisted chat requests including context and search requests " +
                    "with execution statistics.")
    public ResponseEntity<ApiResponse<List<ChatHistory>>> getChatHistory(
            @Parameter(description = "Repository ID") @PathVariable @NotBlank String repositoryId) {

        long startTime = System.currentTimeMillis();
        log.info("Chat history API called: repositoryId={}", repositoryId);

        List<ChatHistory> history = chatService.getChatHistory(repositoryId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Chat history API completed: repositoryId={}, records={}, duration={}ms",
                repositoryId, history.size(), duration);

        return ResponseEntity.ok(ApiResponse.success("Chat history retrieved successfully", history));
    }
}