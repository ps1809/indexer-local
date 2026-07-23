package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.testsupport.*;
import com.projectiq.indexerlocal.service.TestGenerationSupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Unit Test Generation Support APIs.
 * Exposes metadata required for future automatic unit test generation.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/test-support")
@Tag(name = "Test Generation Support", description = "APIs for retrieving metadata required for future automatic unit test generation")
public class TestGenerationSupportController {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerationSupportController.class);

    private final TestGenerationSupportService testGenerationSupportService;

    public TestGenerationSupportController(TestGenerationSupportService testGenerationSupportService) {
        this.testGenerationSupportService = testGenerationSupportService;
    }

    /**
     * Retrieve classes available for future test generation.
     */
    @GetMapping("/classes")
    @Operation(summary = "Get classes for test generation",
               description = "Retrieve all classes available for future test generation in the repository")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Classes retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClassesForTestGeneration(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to get classes for test generation for repository: {}", repositoryId);

        try {
            long startTime = System.currentTimeMillis();
            List<TestSupportClassContext> classes = testGenerationSupportService.getClassesForTestGeneration(repositoryId);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", repositoryId);
            result.put("classes", classes);
            result.put("totalClasses", classes.size());
            result.put("retrievalDurationMs", duration);

            logger.info("Retrieved {} classes for test generation for repository: {}. Duration: {}ms",
                    classes.size(), repositoryId, duration);

            return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error retrieving classes for test generation: repositoryId={}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieve complete metadata for a specific class.
     */
    @GetMapping("/classes/{classId}")
    @Operation(summary = "Get class detail for test generation",
               description = "Retrieve complete metadata for a specific class including methods, fields, constructors, and Spring context")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class detail retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository or class not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClassDetail(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId,
            @Parameter(description = "Class ID", required = true) @PathVariable Long classId) {
        logger.info("Received request to get class detail for classId: {} in repository: {}", classId, repositoryId);

        try {
            long startTime = System.currentTimeMillis();
            TestSupportClassDetail classDetail = testGenerationSupportService.getClassDetail(repositoryId, classId);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", repositoryId);
            result.put("classDetail", classDetail);
            result.put("retrievalDurationMs", duration);

            int methodCount = classDetail.getMethods() != null ? classDetail.getMethods().size() : 0;
            int fieldCount = classDetail.getFields() != null ? classDetail.getFields().size() : 0;
            int constructorCount = classDetail.getConstructors() != null ? classDetail.getConstructors().size() : 0;

            logger.info("Retrieved class detail for classId: {}. Methods: {}, Fields: {}, Constructors: {}. Duration: {}ms",
                    classId, methodCount, fieldCount, constructorCount, duration);

            return ResponseEntity.ok(ApiResponse.success("Class detail retrieved successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository or class not found: repositoryId={}, classId={}, error={}",
                    repositoryId, classId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error retrieving class detail: repositoryId={}, classId={}", repositoryId, classId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieve complete metadata for a specific method.
     */
    @GetMapping("/methods/{methodId}")
    @Operation(summary = "Get method detail for test generation",
               description = "Retrieve complete metadata for a specific method including parameters, return type, exceptions, and annotations")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Method detail retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository or method not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMethodDetail(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId,
            @Parameter(description = "Method ID", required = true) @PathVariable Long methodId) {
        logger.info("Received request to get method detail for methodId: {} in repository: {}", methodId, repositoryId);

        try {
            long startTime = System.currentTimeMillis();
            TestSupportMethodContext methodContext = testGenerationSupportService.getMethodDetail(repositoryId, methodId);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", repositoryId);
            result.put("methodContext", methodContext);
            result.put("retrievalDurationMs", duration);

            logger.info("Retrieved method detail for methodId: {}. Duration: {}ms", methodId, duration);

            return ResponseEntity.ok(ApiResponse.success("Method detail retrieved successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository or method not found: repositoryId={}, methodId={}, error={}",
                    repositoryId, methodId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error retrieving method detail: repositoryId={}, methodId={}", repositoryId, methodId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate structured unit test context from persisted metadata.
     */
    @PostMapping("/context")
    @Operation(summary = "Generate test context",
               description = "Generate structured unit test context from persisted metadata for future AI-powered test generation")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Test context generated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<TestSupportContextResponse>> generateTestContext(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId,
            @RequestBody TestSupportContextRequest request) {
        logger.info("Received request to generate test context for repository: {}", repositoryId);

        try {
            TestSupportContextResponse response = testGenerationSupportService.generateTestContext(repositoryId, request);

            int classCount = response.getClassDetails() != null ? response.getClassDetails().size() : 0;
            int methodCount = response.getMethodDetails() != null ? response.getMethodDetails().size() : 0;

            logger.info("Generated test context for repository: {}. Classes: {}, Methods: {}. Duration: {}ms",
                    repositoryId, classCount, methodCount, response.getGenerationDurationMs());

            return ResponseEntity.ok(ApiResponse.success("Test context generated successfully", response));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error generating test context: repositoryId={}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get test generation statistics for a repository.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get test generation statistics",
               description = "Retrieve execution statistics for test generation requests")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("Received request to get test generation statistics for repository: {}", repositoryId);

        try {
            Map<String, Object> statistics = testGenerationSupportService.getStatistics(repositoryId);

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", repositoryId);
            result.put("statistics", statistics);

            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error retrieving statistics: repositoryId={}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get test generation history for a repository.
     */
    @GetMapping("/history")
    @Operation(summary = "Get test generation history",
               description = "Retrieve history of test generation requests for a repository")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repository not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Repository is not indexed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        logger.info("Received request to get test generation history for repository: {}", repositoryId);

        try {
            List<TestGenerationHistory> history = testGenerationSupportService.getHistory(repositoryId, page, size);

            Map<String, Object> result = new HashMap<>();
            result.put("repositoryId", repositoryId);
            result.put("history", history);
            result.put("page", page);
            result.put("size", size);
            result.put("totalRecords", history.size());

            return ResponseEntity.ok(ApiResponse.success("History retrieved successfully", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Repository not found: repositoryId={}", repositoryId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Repository not indexed: repositoryId={}, error={}", repositoryId, e.getMessage());
            return ResponseEntity.status(409).build();
        } catch (Exception e) {
            logger.error("Error retrieving history: repositoryId={}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}