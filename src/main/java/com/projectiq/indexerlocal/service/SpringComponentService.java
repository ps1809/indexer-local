package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.analyzer.SpringComponentAnalyzer;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.model.SpringComponent;
import com.projectiq.indexerlocal.model.SpringComponentStatistics;
import com.projectiq.indexerlocal.repository.ProjectStructureRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.repository.IndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service for analyzing and managing Spring components in repositories.
 */
@Service
public class SpringComponentService {

    private static final Logger logger = LoggerFactory.getLogger(SpringComponentService.class);

    private final SpringComponentAnalyzer springComponentAnalyzer;
    private final RepositoryRepository repositoryRepository;
    private final ProjectStructureRepository projectStructureRepository;
    private final IndexRepository indexRepository;
    private final JdbcTemplate jdbcTemplate;

    public SpringComponentService(SpringComponentAnalyzer springComponentAnalyzer,
                                  RepositoryRepository repositoryRepository,
                                  ProjectStructureRepository projectStructureRepository,
                                  IndexRepository indexRepository,
                                  JdbcTemplate jdbcTemplate) {
        this.springComponentAnalyzer = springComponentAnalyzer;
        this.repositoryRepository = repositoryRepository;
        this.projectStructureRepository = projectStructureRepository;
        this.indexRepository = indexRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Analyze Spring components for a repository.
     */
    public List<SpringComponent> analyzeSpringComponents(String repositoryId) {
        // Validate repository exists
        Repository repository = validateRepositoryExists(repositoryId);

        // Check if project structure has been analyzed
        long fileCount = projectStructureRepository.countFilesByRepositoryId(repositoryId);
        if (fileCount == 0) {
            throw new IllegalStateException("Project structure must be analyzed before analyzing Spring components for repository: " + repositoryId);
        }

        logger.info("Starting Spring component analysis for repository: {}", repositoryId);
        long startTime = System.currentTimeMillis();

        Path workspacePath = Path.of(repository.getWorkspacePath());

        // Perform Spring component analysis (reuse Technology Stack Detection if available)
        List<SpringComponent> components = springComponentAnalyzer.analyze(workspacePath.toString(), repositoryId);

        // Persist Spring components
        indexRepository.saveSpringComponents(repositoryId, components);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Spring component analysis completed for repository: {}. Found {} components. Duration: {}ms",
                repositoryId, components.size(), duration);

        return components;
    }

    /**
     * Get Spring component inventory for a repository.
     */
    public List<SpringComponent> getSpringComponents(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return indexRepository.findSpringComponentsByRepository(repositoryId);
    }

    /**
     * Get Spring component statistics for a repository.
     */
    public SpringComponentStatistics getSpringComponentStatistics(String repositoryId) {
        validateRepositoryExists(repositoryId);

        // Build statistics from the persisted Spring components using existing counts
        SpringComponentStatistics statistics = new SpringComponentStatistics();
        statistics.setRepositoryId(repositoryId);

        // Get component type counts
        Map<String, Integer> typeCounts = indexRepository.getSpringComponentTypeCounts(repositoryId);
        statistics.setComponentTypeCounts(typeCounts);

        // Get annotation counts  
        Map<String, Integer> annotationCounts = indexRepository.getSpringAnnotationCounts(repositoryId);
        statistics.setAnnotationCounts(annotationCounts);

        // Get total component count
        String sql = "SELECT COUNT(*) FROM spring_component WHERE repository_id = ?";
        Long totalCount = jdbcTemplate.queryForObject(sql, Long.class, repositoryId);
        statistics.setTotalComponents(totalCount != null ? totalCount.intValue() : 0);

        // Set individual counts from the component data
        statistics.setServiceCount(typeCounts.getOrDefault("SERVICE", 0));
        statistics.setRepositoryCount(typeCounts.getOrDefault("REPOSITORY", 0));
        statistics.setControllerCount(typeCounts.getOrDefault("CONTROLLER", 0));
        statistics.setRestControllerCount(typeCounts.getOrDefault("REST_CONTROLLER", 0));
        statistics.setConfigurationClassCount(typeCounts.getOrDefault("CONFIGURATION", 0));

        // Set annotation counts
        Map<String, Integer> annCounts = indexRepository.getSpringAnnotationCounts(repositoryId);
        statistics.setAutowiredCount(annCounts.getOrDefault("AUTOWIRED", 0));
        statistics.setTransactionalCount(annCounts.getOrDefault("TRANSACTIONAL", 0));

        return statistics;
    }

    // ==================== Private Methods ====================

    private Repository validateRepositoryExists(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository with ID '" + repositoryId + "' not found");
        }
        return repository;
    }
}