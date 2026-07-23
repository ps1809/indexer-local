package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.analyzer.ConfigurationAnalyzer;
import com.projectiq.indexerlocal.model.ConfigurationFile;
import com.projectiq.indexerlocal.model.ConfigurationType;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.repository.ConfigurationRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service layer for configuration analysis operations.
 * Coordinates between Repository, ProjectStructureService, and ConfigurationAnalyzer.
 */
@Service
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    private final ConfigurationAnalyzer configurationAnalyzer;
    private final ConfigurationRepository configurationRepository;
    private final RepositoryRepository repositoryRepository;

    public ConfigurationService(ConfigurationAnalyzer configurationAnalyzer,
                                ConfigurationRepository configurationRepository,
                                RepositoryRepository repositoryRepository) {
        this.configurationAnalyzer = configurationAnalyzer;
        this.configurationRepository = configurationRepository;
        this.repositoryRepository = repositoryRepository;
    }

    /**
     * Analyze configurations for a repository.
     * 
     * @param repositoryId the repository ID to analyze
     * @return list of discovered configuration files
     * @throws IllegalArgumentException if repository doesn't exist or project structure analysis not completed
     */
    public List<ConfigurationFile> analyzeConfigurations(String repositoryId) {
        logger.info("Starting configuration analysis for repository: {}", repositoryId);

        // Validate repository exists
        Repository repository = getExistingRepository(repositoryId);
        
        // Check that project structure analysis was completed first
        if (!configurationRepository.hasConfigurations(repositoryId)) {
            logger.info("No prior configuration analysis found for repository: {}", repositoryId);
        }

        Path workspacePath = Path.of(repository.getWorkspacePath());
        
        logger.info("Analyzing configurations in workspace: {}", workspacePath);

        // Analyze configuration files
        List<ConfigurationFile> configurationFiles = configurationAnalyzer.analyzeConfigurations(
                repositoryId, workspacePath);

        // Clear existing configurations and save new ones
        configurationRepository.deleteByRepositoryId(repositoryId);
        configurationRepository.saveAll(configurationFiles);

        // Calculate and store statistics
        Map<String, Object> stats = configurationAnalyzer.calculateStatistics(
                repositoryId, configurationFiles);
        configurationRepository.saveStatistics(repositoryId, stats);

        logger.info("Configuration analysis completed for repository: {}. Found {} configuration files.",
                repositoryId, configurationFiles.size());

        return configurationFiles;
    }

    /**
     * Get configuration inventory for a repository.
     */
    public List<ConfigurationFile> getConfigurations(String repositoryId) {
        validateRepositoryExists(repositoryId);
        return configurationRepository.findByRepositoryId(repositoryId);
    }

    /**
     * Get configuration files filtered by type.
     */
    public List<ConfigurationFile> getConfigurationsByType(String repositoryId, ConfigurationType type) {
        validateRepositoryExists(repositoryId);
        return configurationRepository.findByRepositoryIdAndType(repositoryId, type);
    }

    /**
     * Get configuration statistics for a repository.
     */
    public Map<String, Object> getStatistics(String repositoryId) {
        validateRepositoryExists(repositoryId);
        
        Map<String, Object> stats = configurationRepository.getStatistics(repositoryId);
        if (stats.isEmpty()) {
            if (configurationRepository.hasConfigurations(repositoryId)) {
                return stats;
            }
            throw new IllegalStateException("Configuration analysis not completed for repository: " + repositoryId);
        }
        return stats;
    }

    /**
     * Check if a repository has been analyzed for configurations.
     */
    public boolean hasConfigurations(String repositoryId) {
        return configurationRepository.hasConfigurations(repositoryId);
    }

    // ==================== Private Methods ====================

    private Repository getExistingRepository(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException(
                    "Repository not found: " + repositoryId);
        }
        return repository;
    }

    private void validateRepositoryExists(String repositoryId) {
        if (!repositoryRepository.existsByRepositoryId(repositoryId)) {
            throw new IllegalArgumentException(
                    "Invalid repository ID: " + repositoryId);
        }
    }
}