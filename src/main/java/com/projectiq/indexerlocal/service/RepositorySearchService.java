package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.repositorysearch.RepositorySearchResult;

/**
 * Service interface for the Repository Search Engine.
 * Provides deterministic discovery of repository resources including files, folders,
 * metadata, languages, and repository structure entirely from the index database
 * without filesystem scanning.
 */
public interface RepositorySearchService {

    /**
     * Find files matching the given criteria.
     */
    PaginatedResponse<RepositorySearchResult> findFiles(
            String repositoryId, String fileName, String extension, String module,
            String matchMode, Long minSize, Long maxSize,
            int page, int size);

    /**
     * Find folders matching the given criteria.
     */
    PaginatedResponse<RepositorySearchResult> findFolders(
            String repositoryId, String folderName, String classification,
            String matchMode,
            int page, int size);

    /**
     * Find file extensions across repositories.
     */
    PaginatedResponse<RepositorySearchResult> findExtensions(
            String repositoryId, String extension, String language,
            String matchMode,
            int page, int size);

    /**
     * Find repository metadata matching the given criteria.
     */
    PaginatedResponse<RepositorySearchResult> findRepositories(
            String repositoryId, String repositoryName, String status,
            String buildSystem, String technologyStack,
            String matchMode,
            int page, int size);

    /**
     * Find languages across repositories.
     */
    PaginatedResponse<RepositorySearchResult> findLanguages(
            String repositoryId, String language,
            String matchMode,
            int page, int size);

    /**
     * Find root modules across repositories.
     */
    PaginatedResponse<RepositorySearchResult> findRootModules(
            String repositoryId, String moduleName,
            String matchMode,
            int page, int size);

    /**
     * Find source directories across repositories.
     */
    PaginatedResponse<RepositorySearchResult> findSourceDirectories(
            String repositoryId, String directoryName,
            String matchMode,
            int page, int size);

    /**
     * Find resource directories across repositories.
     */
    PaginatedResponse<RepositorySearchResult> findResourceDirectories(
            String repositoryId, String directoryName,
            String matchMode,
            int page, int size);

    /**
     * Find test directories across repositories.
     */
    PaginatedResponse<RepositorySearchResult> findTestDirectories(
            String repositoryId, String directoryName,
            String matchMode,
            int page, int size);
}