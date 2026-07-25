package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.model.buildsearch.BuildSearchResult;

/**
 * Service interface for the Build Search Engine.
 * Provides deterministic discovery of build system metadata across indexed repositories
 * without filesystem scanning.
 */
public interface BuildSearchService {

    /**
     * Search for Maven projects across repositories.
     */
    PaginatedResponse<BuildSearchResult> findMavenProjects(
            String repositoryId, String groupId, String artifactId, String version,
            int page, int size);

    /**
     * Search for Gradle projects across repositories.
     */
    PaginatedResponse<BuildSearchResult> findGradleProjects(
            String repositoryId, String projectName, String group,
            int page, int size);

    /**
     * Search for build files across repositories.
     */
    PaginatedResponse<BuildSearchResult> findBuildFiles(
            String repositoryId, String buildSystem, String buildFileName,
            int page, int size);

    /**
     * Search for modules across repositories.
     */
    PaginatedResponse<BuildSearchResult> findModules(
            String repositoryId, String moduleName, String parentModule,
            int page, int size);

    /**
     * Search for parent projects.
     */
    PaginatedResponse<BuildSearchResult> findParentProjects(
            String repositoryId, String parentGroupId, String parentArtifactId,
            int page, int size);

    /**
     * Search for child modules of a given parent.
     */
    PaginatedResponse<BuildSearchResult> findChildModules(
            String repositoryId, String parentModule,
            int page, int size);

    /**
     * Search for plugins across repositories.
     */
    PaginatedResponse<BuildSearchResult> findPlugins(
            String repositoryId, String pluginName, String moduleName,
            int page, int size);

    /**
     * Search for dependencies across repositories.
     */
    PaginatedResponse<BuildSearchResult> findDependencies(
            String repositoryId, String groupId, String artifactId, String version,
            String moduleName,
            int page, int size);

    /**
     * Search for build profiles across repositories.
     */
    PaginatedResponse<BuildSearchResult> findBuildProfiles(
            String repositoryId, String profileName, String moduleName,
            int page, int size);

    /**
     * Search for build configurations across repositories.
     */
    PaginatedResponse<BuildSearchResult> findBuildConfigurations(
            String repositoryId, String buildSystem, String packaging,
            String moduleName,
            int page, int size);

    /**
     * General build search with multiple filters.
     */
    PaginatedResponse<BuildSearchResult> searchBuild(
            String repositoryId, String buildSystem, String moduleName,
            String groupId, String artifactId, String version,
            String plugin, String dependency, String profile,
            int page, int size);
}