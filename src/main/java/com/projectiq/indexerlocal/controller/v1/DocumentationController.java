package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.RepositoryDocumentation;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.service.DocumentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for repository documentation generation and retrieval.
 */
@RestController
@RequestMapping("/api/v1/repositories")
@Tag(name = "Documentation", description = "APIs for generating and retrieving repository documentation")
public class DocumentationController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DocumentationService documentationService;

    public DocumentationController(DocumentationService documentationService) {
        this.documentationService = documentationService;
    }

    /**
     * Generate documentation for a repository.
     */
    @PostMapping("/{repositoryId}/documentation/generate")
    @Operation(summary = "Generate repository documentation", 
               description = "Generate human-readable Markdown documentation from persisted indexed metadata")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documentation generated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateDocumentation(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to generate documentation for repository: {}", repositoryId);

        try {
            long startTime = System.currentTimeMillis();
            RepositoryDocumentation documentation = documentationService.generateDocumentation(repositoryId);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", documentation.getRepositoryId());
            result.put("generatedAt", documentation.getGeneratedAt() != null ? 
                    documentation.getGeneratedAt().format(DATE_FORMATTER) : null);
            result.put("contentSize", documentation.getContentSize());
            result.put("generationStatus", documentation.getGenerationStatus());
            result.put("generationDurationMs", duration);

            logger.info("Documentation generated successfully for repository: {}. Duration: {}ms, Size: {} bytes",
                    repositoryId, duration, documentation.getContentSize());

            return ResponseEntity.ok(ApiResponse.success("Documentation generated successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error generating documentation for repository: {}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieve generated documentation for a repository.
     */
    @GetMapping("/{repositoryId}/documentation")
    @Operation(summary = "Retrieve generated documentation", 
               description = "Retrieve the generated Markdown documentation for a repository")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documentation retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository or documentation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentation(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to retrieve documentation for repository: {}", repositoryId);

        try {
            RepositoryDocumentation documentation = documentationService.getDocumentation(repositoryId);

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", documentation.getRepositoryId());
            result.put("markdownContent", documentation.getMarkdownContent());
            result.put("generatedAt", documentation.getGeneratedAt() != null ? 
                    documentation.getGeneratedAt().format(DATE_FORMATTER) : null);
            result.put("contentSize", documentation.getContentSize());
            result.put("generationStatus", documentation.getGenerationStatus());

            return ResponseEntity.ok(ApiResponse.success("Documentation retrieved successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Documentation not found or repository not indexed: repositoryId={}, error={}", 
                    repositoryId, e.getMessage());
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            logger.error("Error retrieving documentation for repository: {}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download generated Markdown documentation.
     */
    @GetMapping("/{repositoryId}/documentation/download")
    @Operation(summary = "Download documentation", 
               description = "Download the generated Markdown documentation as a .md file")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documentation downloaded successfully",
            content = @Content(mediaType = "text/markdown")),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository or documentation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<byte[]> downloadDocumentation(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to download documentation for repository: {}", repositoryId);

        try {
            RepositoryDocumentation documentation = documentationService.getDocumentation(repositoryId);

            String markdownContent = documentation.getMarkdownContent();
            byte[] content = markdownContent.getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/markdown"));
            headers.setContentDispositionFormData("attachment", 
                    documentation.getRepositoryId() + "-documentation.md");
            headers.setContentLength(content.length);

            logger.info("Documentation downloaded for repository: {}. Size: {} bytes", 
                    repositoryId, content.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Documentation not found or repository not indexed: repositoryId={}, error={}", 
                    repositoryId, e.getMessage());
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            logger.error("Error downloading documentation for repository: {}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}