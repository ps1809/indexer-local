package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.DirectoryClassification;
import com.projectiq.indexerlocal.model.DirectoryMetadata;
import com.projectiq.indexerlocal.model.FileClassification;
import com.projectiq.indexerlocal.model.FileMetadata;
import com.projectiq.indexerlocal.model.ProjectStructureStatistics;
import com.projectiq.indexerlocal.service.ProjectStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for project structure analysis APIs.
 */
@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/structure")
@Tag(name = "Project Structure API", description = "APIs for analyzing and retrieving project structure metadata")
public class ProjectStructureController {

    private final ProjectStructureService projectStructureService;

    public ProjectStructureController(ProjectStructureService projectStructureService) {
        this.projectStructureService = projectStructureService;
    }

    @PostMapping
    @Operation(summary = "Analyze project structure", description = "Scan and analyze the project structure of a registered repository.")
    @ApiResponse(description = "Project structure statistics returned on success")
    public ProjectStructureStatistics analyzeStructure(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        return projectStructureService.analyzeStructure(repositoryId);
    }

    @GetMapping
    @Operation(summary = "Get project structure", description = "Retrieve the directory structure of a repository.")
    @ApiResponse(description = "List of directories returned on success")
    public List<DirectoryMetadata> getStructure(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        return projectStructureService.getStructure(repositoryId);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get structure statistics", description = "Retrieve statistics about the project structure.")
    @ApiResponse(description = "Project structure statistics returned on success")
    public ProjectStructureStatistics getStatistics(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        return projectStructureService.getStatistics(repositoryId);
    }

    @GetMapping("/files")
    @Operation(summary = "Get files", description = "Retrieve all discovered files in the repository.")
    @ApiResponse(description = "List of file metadata returned on success")
    public List<FileMetadata> getFiles(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        return projectStructureService.getFiles(repositoryId);
    }

    @GetMapping("/directories")
    @Operation(summary = "Get directories", description = "Retrieve all discovered directories in the repository.")
    @ApiResponse(description = "List of directory metadata returned on success")
    public List<DirectoryMetadata> getDirectories(
            @Parameter(description = "Repository ID") @PathVariable String repositoryId) {
        return projectStructureService.getDirectories(repositoryId);
    }

    @GetMapping("/file-classifications")
    @Operation(summary = "Get file classifications", description = "Retrieve available file classifications.")
    public Map<String, String> getFileClassifications() {
        Map<String, String> classifications = new HashMap<>();
        classifications.put("JAVA_SOURCE", "Java source files (.java)");
        classifications.put("KOTLIN", "Kotlin files (.kt, .kts)");
        classifications.put("GROOVY", "Groovy files (.groovy, .gradle)");
        classifications.put("XML", "XML files (.xml)");
        classifications.put("YAML", "YAML files (.yml, .yaml)");
        classifications.put("PROPERTIES", "Properties files (.properties, .prop)");
        classifications.put("JSON", "JSON files (.json)");
        classifications.put("SQL", "SQL files (.sql)");
        classifications.put("MARKDOWN", "Markdown files (.md, .markdown)");
        classifications.put("HTML", "HTML files (.html, .htm, .jsp)");
        classifications.put("JAVASCRIPT", "JavaScript files (.js)");
        classifications.put("TYPESCRIPT", "TypeScript files (.ts, .tsx)");
        classifications.put("CSS", "CSS files (.css, .scss, .less)");
        classifications.put("SHELL_SCRIPT", "Shell scripts (.sh, .bash, .bat, .cmd, .ps1)");
        classifications.put("BUILD_FILE", "Build files (pom.xml, build.gradle, etc.)");
        classifications.put("UNKNOWN", "Unknown file type");
        return classifications;
    }

    @GetMapping("/directory-classifications")
    @Operation(summary = "Get directory classifications", description = "Retrieve available directory classifications.")
    public Map<String, String> getDirectoryClassifications() {
        Map<String, String> classifications = new HashMap<>();
        classifications.put("SOURCE", "Source code directories");
        classifications.put("RESOURCE", "Resource directories");
        classifications.put("TEST", "Test directories");
        classifications.put("CONFIGURATION", "Configuration directories");
        classifications.put("DOCUMENTATION", "Documentation directories");
        classifications.put("BUILD", "Build directories");
        classifications.put("OUTPUT", "Output directories");
        classifications.put("UNKNOWN", "Unknown directory type");
        return classifications;
    }
}