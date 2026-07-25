package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.repositorysearch.RepositorySearchResult;
import com.projectiq.indexerlocal.service.RepositorySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Repository Search Engine.
 * Provides endpoints for deterministic discovery of repository resources
 * including files, folders, metadata, languages, and repository structure.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Repository Search", description = "Repository Search Engine APIs for deterministic resource discovery")
public class RepositorySearchControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(RepositorySearchControllerV1.class);

    private final RepositorySearchService repositorySearchService;

    public RepositorySearchControllerV1(RepositorySearchService repositorySearchService) {
        this.repositorySearchService = repositorySearchService;
    }

    @GetMapping("/file")
    @Operation(summary = "Find files", description = "Search for files across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findFiles(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "File name to search") String fileName,
            @RequestParam(required = false) @Parameter(description = "File extension filter") String extension,
            @RequestParam(required = false) @Parameter(description = "Module filter") String module,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(required = false) @Parameter(description = "Minimum file size in bytes") Long minSize,
            @RequestParam(required = false) @Parameter(description = "Maximum file size in bytes") Long maxSize,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findFiles called: repositoryId={}, fileName={}, extension={}, module={}, matchMode={}, page={}, size={}",
                repositoryId, fileName, extension, module, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFiles(
                repositoryId, fileName, extension, module, matchMode, minSize, maxSize, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/folder")
    @Operation(summary = "Find folders", description = "Search for folders across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findFolders(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Folder name to search") String folderName,
            @RequestParam(required = false) @Parameter(description = "Directory classification filter") String classification,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findFolders called: repositoryId={}, folderName={}, classification={}, matchMode={}, page={}, size={}",
                repositoryId, folderName, classification, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findFolders(
                repositoryId, folderName, classification, matchMode, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/extension")
    @Operation(summary = "Find file extensions", description = "Search for file extensions across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findExtensions(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Extension to search") String extension,
            @RequestParam(required = false) @Parameter(description = "Language filter") String language,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findExtensions called: repositoryId={}, extension={}, language={}, matchMode={}, page={}, size={}",
                repositoryId, extension, language, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findExtensions(
                repositoryId, extension, language, matchMode, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/repository")
    @Operation(summary = "Find repositories", description = "Search for repository metadata")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findRepositories(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Repository name to search") String repositoryName,
            @RequestParam(required = false) @Parameter(description = "Status filter") String status,
            @RequestParam(required = false) @Parameter(description = "Build system filter") String buildSystem,
            @RequestParam(required = false) @Parameter(description = "Technology stack filter") String technologyStack,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findRepositories called: repositoryId={}, repositoryName={}, status={}, buildSystem={}, technologyStack={}, matchMode={}, page={}, size={}",
                repositoryId, repositoryName, status, buildSystem, technologyStack, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findRepositories(
                repositoryId, repositoryName, status, buildSystem, technologyStack, matchMode, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/language")
    @Operation(summary = "Find languages", description = "Search for languages across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findLanguages(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Language to search") String language,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findLanguages called: repositoryId={}, language={}, matchMode={}, page={}, size={}",
                repositoryId, language, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findLanguages(
                repositoryId, language, matchMode, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/module")
    @Operation(summary = "Find root modules", description = "Search for root modules across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findRootModules(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Module name to search") String moduleName,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findRootModules called: repositoryId={}, moduleName={}, matchMode={}, page={}, size={}",
                repositoryId, moduleName, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findRootModules(
                repositoryId, moduleName, matchMode, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/source-directory")
    @Operation(summary = "Find source directories", description = "Search for source directories across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findSourceDirectories(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Directory name to search") String directoryName,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findSourceDirectories called: repositoryId={}, directoryName={}, matchMode={}, page={}, size={}",
                repositoryId, directoryName, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findSourceDirectories(
                repositoryId, directoryName, matchMode, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/resource-directory")
    @Operation(summary = "Find resource directories", description = "Search for resource directories across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findResourceDirectories(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Directory name to search") String directoryName,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findResourceDirectories called: repositoryId={}, directoryName={}, matchMode={}, page={}, size={}",
                repositoryId, directoryName, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findResourceDirectories(
                repositoryId, directoryName, matchMode, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/test-directory")
    @Operation(summary = "Find test directories", description = "Search for test directories across indexed repositories")
    public ResponseEntity<PaginatedResponse<RepositorySearchResult>> findTestDirectories(
            @RequestParam(required = false) @Parameter(description = "Repository ID filter") String repositoryId,
            @RequestParam(required = false) @Parameter(description = "Directory name to search") String directoryName,
            @RequestParam(required = false, defaultValue = "partial") @Parameter(description = "Match mode: exact, prefix, partial, wildcard") String matchMode,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {
        logger.debug("findTestDirectories called: repositoryId={}, directoryName={}, matchMode={}, page={}, size={}",
                repositoryId, directoryName, matchMode, page, size);
        PaginatedResponse<RepositorySearchResult> result = repositorySearchService.findTestDirectories(
                repositoryId, directoryName, matchMode, page, size);
        return ResponseEntity.ok(result);
    }
}