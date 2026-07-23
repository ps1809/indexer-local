package com.projectiq.indexerlocal.model;

/**
 * Enumeration of configuration file types for project configuration analysis.
 */
public enum ConfigurationType {
    APPLICATION_CONFIGURATION,
    LOGGING_CONFIGURATION,
    BUILD_CONFIGURATION,
    DEPLOYMENT_CONFIGURATION,
    CONTAINER_CONFIGURATION,
    KUBERNETES_CONFIGURATION,
    CICD_CONFIGURATION,
    ENVIRONMENT_CONFIGURATION,
    UNKNOWN_CONFIGURATION
}