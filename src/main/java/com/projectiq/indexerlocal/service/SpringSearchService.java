package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.springsearch.SpringSearchResult;

import java.util.List;

/**
 * Service interface for the Spring Search Engine.
 * Provides fast, deterministic search over indexed Spring Framework artifacts
 * without filesystem scanning.
 */
public interface SpringSearchService {

    /**
     * Find all Spring controllers in a repository.
     */
    List<SpringSearchResult> findControllers(String repositoryId, String packageName,
                                              String module, int page, int size);

    /**
     * Find all REST endpoints in a repository, with optional filters.
     */
    List<SpringSearchResult> findEndpoints(String repositoryId, String httpMethod,
                                            String path, String controllerName,
                                            String packageName, String module,
                                            int page, int size);

    /**
     * Find all Spring services in a repository.
     */
    List<SpringSearchResult> findServices(String repositoryId, String packageName,
                                           String module, int page, int size);

    /**
     * Find all Spring repositories in a repository.
     */
    List<SpringSearchResult> findRepositories(String repositoryId, String packageName,
                                                String module, int page, int size);

    /**
     * Find all Spring components in a repository.
     */
    List<SpringSearchResult> findComponents(String repositoryId, String packageName,
                                             String module, int page, int size);

    /**
     * Find all Spring configuration classes in a repository.
     */
    List<SpringSearchResult> findConfigurationClasses(String repositoryId, String packageName,
                                                       String module, int page, int size);

    /**
     * Find all Spring beans in a repository.
     */
    List<SpringSearchResult> findBeans(String repositoryId, String packageName,
                                        String beanName, String module, int page, int size);

    /**
     * Find all scheduled tasks in a repository.
     */
    List<SpringSearchResult> findScheduledTasks(String repositoryId, String packageName,
                                                  String module, int page, int size);

    /**
     * Find all event listeners in a repository.
     */
    List<SpringSearchResult> findEventListeners(String repositoryId, String packageName,
                                                  String module, int page, int size);

    /**
     * Count the total number of results for a given search type and filters.
     */
    long countControllers(String repositoryId, String packageName, String module);

    long countEndpoints(String repositoryId, String httpMethod, String path,
                        String controllerName, String packageName, String module);

    long countServices(String repositoryId, String packageName, String module);

    long countRepositories(String repositoryId, String packageName, String module);

    long countComponents(String repositoryId, String packageName, String module);

    long countConfigurationClasses(String repositoryId, String packageName, String module);

    long countBeans(String repositoryId, String packageName, String beanName, String module);

    long countScheduledTasks(String repositoryId, String packageName, String module);

    long countEventListeners(String repositoryId, String packageName, String module);
}