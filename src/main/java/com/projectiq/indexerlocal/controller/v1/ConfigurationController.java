package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.ConfigurationFile;
import com.projectiq.indexerlocal.model.ConfigurationType;
import com.projectiq.indexerlocal.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * REST controller for project configuration analysis operations.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/configurations")
@Tag(name = "Configuration Analyzer", description = "APIs for discovering and analyzing project configuration files")
public class ConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Analyze project configuration files for a repository.
     */
    @PostMapping
    @Operation(summary = "Analyze project configuration", description = "Discover, analyze, classify, and persist all project configuration files. " +
            "Prerequisite: Project structure analysis must be completed first.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration analysis completed successfully",
                    content = @Content(schema = @Schema(type = "array", implementation = ConfigurationFile.class))),
            @ApiResponse(responseCode = "404", description = "Repository not found"),
            @ApiResponse(responseCode = "409", description = "Project structure analysis not completed")
    })
    public ResponseEntity<List<ConfigurationFile>> analyzeConfigurations(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("POST /api/v1/repositories/{}/configurations - Analyze configurations requested", repositoryId);

        List<ConfigurationFile> configurationFiles = configurationService.analyzeConfigurations(repositoryId);

        return ResponseEntity.ok(configurationFiles);
    }

    /**
     * Retrieve configuration inventory for a repository.
     */
    @GetMapping
    @Operation(summary = "Get configuration inventory", description = "Retrieve all discovered configuration files for a repository.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration inventory retrieved successfully",
                    content = @Content(schema = @Schema(type = "array", implementation = ConfigurationFile.class))),
            @ApiResponse(responseCode = "404", description = "Repository not found"),
            @ApiResponse(responseCode = "503", description = "Configuration analysis not completed")
    })
    public ResponseEntity<List<ConfigurationFile>> getConfigurations(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("GET /api/v1/repositories/{}/configurations - Get configurations requested", repositoryId);

        List<ConfigurationFile> configurationFiles = configurationService.getConfigurations(repositoryId);

        return ResponseEntity.ok(configurationFiles);
    }

    /**
     * Retrieve configuration files filtered by type.
     */
    @GetMapping("/type/{configurationType}")
    @Operation(summary = "Get configuration files by type", description = "Retrieve configuration files filtered by configuration type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration files retrieved successfully",
                    content = @Content(schema = @Schema(type = "array", implementation = ConfigurationFile.class))),
            @ApiResponse(responseCode = "404", description = "Repository not found"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration type")
    })
    public ResponseEntity<List<ConfigurationFile>> getConfigurationsByType(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId,
            @Parameter(description = "Configuration type", required = true) @PathVariable ConfigurationType configurationType) {
        logger.info("GET /api/v1/repositories/{}/configurations/type/{} - Get configurations by type requested", 
                repositoryId, configurationType);

        List<ConfigurationFile> configurationFiles = configurationService.getConfigurationsByType(repositoryId, configurationType);

        return ResponseEntity.ok(configurationFiles);
    }

    /**
     * Retrieve configuration statistics for a repository.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get configuration statistics", description = "Retrieve statistics about project configuration files including counts by type and format.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration statistics retrieved successfully",
                    content = @Content(schema = @Schema(type = "object"))),
            @ApiResponse(responseCode = "404", description = "Repository not found"),
            @ApiResponse(responseCode = "503", description = "Configuration analysis not completed")
    })
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Parameter(description = "Repository ID", required = true) @PathVariable String repositoryId) {
        logger.info("GET /api/v1/repositories/{}/configurations/statistics - Get statistics requested", repositoryId);

        Map<String, Object> statistics = configurationService.getStatistics(repositoryId);

        return ResponseEntity.ok(statistics);
    }
}