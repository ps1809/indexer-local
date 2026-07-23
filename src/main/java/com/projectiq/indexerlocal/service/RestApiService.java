package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.analyzer.RestApiAnalyzer;
import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for analyzing and managing REST API endpoints in repositories.
 * Reuses SpringComponentAnalyzer results for REST controller detection.
 */
@Service
public class RestApiService {

    private static final Logger logger = LoggerFactory.getLogger(RestApiService.class);

    private final RestApiAnalyzer restApiAnalyzer;
    private final RepositoryRepository repositoryRepository;
    private final SpringComponentService springComponentService;
    private final IndexRepository indexRepository;

    public RestApiService(RestApiAnalyzer restApiAnalyzer,
                          RepositoryRepository repositoryRepository,
                          SpringComponentService springComponentService,
                          IndexRepository indexRepository) {
        this.restApiAnalyzer = restApiAnalyzer;
        this.repositoryRepository = repositoryRepository;
        this.springComponentService = springComponentService;
        this.indexRepository = indexRepository;
    }

    /**
     * Analyze REST APIs in a repository.
     * Requires that Spring Component Analysis has already been completed.
     */
    public List<RestApiEndpoint> analyzeRestApis(String repositoryId) {
        RestApiAnalysisResult result = analyzeRestApiInternal(repositoryId);
        return result.endpoints();
    }

    /**
     * Internal method that performs the actual REST API analysis.
     */
    private RestApiAnalysisResult analyzeRestApiInternal(String repositoryId) {
        logger.info("Starting REST API analysis for repository: {}", repositoryId);

        // Verify repository exists
        Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            logger.error("Repository not found: {}", repositoryId);
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }

        // Verify Spring Component Analysis has been completed
        List<SpringComponent> existingComponents = springComponentService.getSpringComponents(repositoryId);
        if (existingComponents.isEmpty()) {
            logger.warn("No Spring components found for repository {}. Please run Spring Component Analysis first.", repositoryId);
        }

        // Get workspace path from repository
        String projectPath = repo.getWorkspacePath();
        if (projectPath == null || projectPath.isEmpty()) {
            projectPath = repo.getOriginalPath();
        }
        
        if (projectPath == null || projectPath.isEmpty()) {
            logger.error("No valid path found for repository: {}", repositoryId);
            throw new IllegalArgumentException("No valid path found for repository: " + repositoryId);
        }

        // Analyze REST APIs using JavaParser (independent of Spring Component Analyzer)
        List<RestApiEndpoint> endpoints = restApiAnalyzer.analyze(projectPath, repositoryId);

        logger.info("Discovered {} REST controller(s) with {} endpoint(s)", 
                endpoints.stream().map(RestApiEndpoint::getControllerName).distinct().count(),
                endpoints.size());

        // Generate statistics
        RestApiStatistics statistics = restApiAnalyzer.generateStatistics(endpoints, repositoryId);
        statistics.setRepositoryId(repositoryId);
        
        logger.info("REST API statistics - Total Controllers: {}, Total Endpoints: {}, Secure: {}, Public: {}",
                statistics.getTotalRestControllers(),
                statistics.getTotalEndpoints(),
                statistics.getSecureEndpoints(),
                statistics.getPublicEndpoints());

        // Persist results
        persistRestApiResults(repositoryId, endpoints, statistics);

        logger.info("REST API analysis complete for repository {}. Found {} endpoints.", repositoryId, endpoints.size());

        return new RestApiAnalysisResult(endpoints, statistics);
    }

    /**
     * Get REST API inventory for a repository.
     * Returns endpoints that were detected by RestApiAnalyzer.
     */
    public List<RestApiEndpoint> getRestApis(String repositoryId) {
        return getRestApiInventory(repositoryId);
    }

    /**
     * Get raw REST API inventory for a repository (deprecated, use getRestApis instead).
     */
    @Deprecated
    public List<RestApiEndpoint> getRestApiInventory(String repositoryId) {
        // Verify repository exists
        Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            logger.error("Repository not found: {}", repositoryId);
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }

        // Get persisted Spring components that are REST controllers
        List<SpringComponent> components = springComponentService.getSpringComponents(repositoryId);
        
        // Filter to only REST controllers
        return components.stream()
                .filter(SpringComponent::isRestController)
                .filter(c -> "REST_ENDPOINT".equals(c.getComponentType()))
                .map(this::convertToRestApiEndpoint)
                .toList();
    }

    /**
     * Get REST API statistics for a repository.
     */
    public RestApiStatistics getRestApiStatistics(String repositoryId) {
        // Verify repository exists
        Repository repo = repositoryRepository.findByRepositoryId(repositoryId);
        if (repo == null) {
            logger.error("Repository not found: {}", repositoryId);
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }

        // Get persisted Spring components
        List<SpringComponent> components = springComponentService.getSpringComponents(repositoryId);
        
        // Filter to only REST controllers
        List<RestApiEndpoint> endpoints = components.stream()
                .filter(SpringComponent::isRestController)
                .filter(c -> "REST_ENDPOINT".equals(c.getComponentType()))
                .map(this::convertToRestApiEndpoint)
                .toList();

        if (endpoints.isEmpty()) {
            RestApiStatistics stats = new RestApiStatistics();
            stats.setRepositoryId(repositoryId);
            stats.setTotalRestControllers(0);
            stats.setTotalEndpoints(0);
            stats.setSecureEndpoints(0);
            stats.setPublicEndpoints(0);
            return stats;
        }

        return restApiAnalyzer.generateStatistics(endpoints, repositoryId);
    }

    /**
     * Persist REST API analysis results as Spring components.
     * Stores REST endpoint metadata in the spring_component table via IndexRepository.
     */
    private void persistRestApiResults(String repositoryId, List<RestApiEndpoint> endpoints, RestApiStatistics statistics) {
        // Use IndexRepository to save/rest APIs
        indexRepository.saveRestApiEndpoints(repositoryId, endpoints, statistics);
    }

    /**
     * Convert SpringComponent entity to RestApiEndpoint.
     */
    private RestApiEndpoint convertToRestApiEndpoint(SpringComponent component) {
        RestApiEndpoint endpoint = new RestApiEndpoint();
        endpoint.setClassName(component.getClassName());
        
        // Parse HTTP method and path from componentName (format: "GET /api/...")
        String componentName = component.getComponentName();
        if (componentName != null && componentName.contains(" ")) {
            String[] parts = componentName.split(" ", 2);
            endpoint.setHttpMethod(parts[0]);
            endpoint.setEndpointPath(parts.length > 1 ? parts[1] : "");
        }

        // Parse basepath from beanName
        String beanName = component.getBeanName();
        if (beanName != null && !beanName.isEmpty()) {
            endpoint.setBasepath(beanName);
        }

        // Parse produces/consumes from sourceFile
        String sourceFile = component.getSourceFile();
        if (sourceFile != null && !sourceFile.isEmpty()) {
            String[] parts = sourceFile.split(";");
            for (String part : parts) {
                if (part.startsWith("produces=")) {
                    endpoint.setProducesMediaType(part.substring(9));
                } else if (part.startsWith("consumes=")) {
                    endpoint.setConsumesMediaType(part.substring(9));
                }
            }
        }

        // Set security flags
        endpoint.setPreAuthorize(component.hasPreAuthorize());
        endpoint.setPostAuthorize(component.hasPostAuthorize());
        endpoint.setRolesAllowed(component.hasRolesAllowed());
        endpoint.setSecured(component.hasSecured());

        return endpoint;
    }

    /**
     * Result of REST API analysis.
     */
    public record RestApiAnalysisResult(
            List<RestApiEndpoint> endpoints,
            RestApiStatistics statistics
    ) {}
}