package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.RepositoryReadme;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.service.ReadmeService;
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
 * REST controller for repository README generation and retrieval.
 */
@RestController
@RequestMapping("/api/v1/repositories")
@Tag(name = "README Enhancement", description = "APIs for generating and retrieving enhanced README files")
public class ReadmeController {

    private static final Logger logger = LoggerFactory.getLogger(ReadmeController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReadmeService readmeService;

    public ReadmeController(ReadmeService readmeService) {
        this.readmeService = readmeService;
    }

    /**
     * Generate enhanced README for a repository.
     */
    @PostMapping("/{repositoryId}/readme/generate")
    @Operation(summary = "Generate enhanced README", 
               description = "Generate a concise, developer-friendly README.md from persisted indexed metadata")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "README generated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReadme(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to generate README for repository: {}", repositoryId);

        try {
            long startTime = System.currentTimeMillis();
            RepositoryReadme readme = readmeService.generateReadme(repositoryId);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", readme.getRepositoryId());
            result.put("generatedAt", readme.getGeneratedAt() != null ? 
                    readme.getGeneratedAt().format(DATE_FORMATTER) : null);
            result.put("contentSize", readme.getContentSize());
            result.put("generationStatus", readme.getGenerationStatus());
            result.put("generationDurationMs", duration);

            logger.info("README generated successfully for repository: {}. Duration: {}ms, Size: {} bytes",
                    repositoryId, duration, readme.getContentSize());

            return ResponseEntity.ok(ApiResponse.success("README generated successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error generating README for repository: {}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieve generated README for a repository.
     */
    @GetMapping("/{repositoryId}/readme")
    @Operation(summary = "Retrieve generated README", 
               description = "Retrieve the generated enhanced README for a repository")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "README retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository or README not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReadme(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to retrieve README for repository: {}", repositoryId);

        try {
            RepositoryReadme readme = readmeService.getReadme(repositoryId);

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", readme.getRepositoryId());
            result.put("markdownContent", readme.getMarkdownContent());
            result.put("generatedAt", readme.getGeneratedAt() != null ? 
                    readme.getGeneratedAt().format(DATE_FORMATTER) : null);
            result.put("contentSize", readme.getContentSize());
            result.put("generationStatus", readme.getGenerationStatus());

            return ResponseEntity.ok(ApiResponse.success("README retrieved successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("README not found or repository not indexed: repositoryId={}, error={}", 
                    repositoryId, e.getMessage());
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            logger.error("Error retrieving README for repository: {}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download generated README.md file.
     */
    @GetMapping("/{repositoryId}/readme/download")
    @Operation(summary = "Download README", 
               description = "Download the generated enhanced README as a .md file")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "README downloaded successfully",
            content = @Content(mediaType = "text/markdown")),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository or README not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<byte[]> downloadReadme(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to download README for repository: {}", repositoryId);

        try {
            RepositoryReadme readme = readmeService.getReadme(repositoryId);

            String markdownContent = readme.getMarkdownContent();
            byte[] content = markdownContent.getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/markdown"));
            headers.setContentDispositionFormData("attachment", "README.md");
            headers.setContentLength(content.length);

            logger.info("README downloaded for repository: {}. Size: {} bytes", 
                    repositoryId, content.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("README not found or repository not indexed: repositoryId={}, error={}", 
                    repositoryId, e.getMessage());
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            logger.error("Error downloading README for repository: {}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}