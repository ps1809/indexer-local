package com.projectiq.indexerlocal.model;

/**
 * Statistics for REST API endpoints in a repository.
 */
public class RestApiStatistics {

    private String repositoryId;
    private int totalRestControllers;
    private int totalEndpoints;
    private int secureEndpoints;
    private int publicEndpoints;
    private int endpointsByGetMapping;
    private int endpointsByPostMapping;
    private int endpointsByPutMapping;
    private int endpointsByDeleteMapping;
    private int endpointsByPatchMapping;
    private int endpointsByRequestMapping;
    
    // Controllers breakdown
    private java.util.Map<String, Integer> endpointsByController = new java.util.HashMap<>();

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public int getTotalRestControllers() {
        return totalRestControllers;
    }

    public void setTotalRestControllers(int totalRestControllers) {
        this.totalRestControllers = totalRestControllers;
    }

    public int getTotalEndpoints() {
        return totalEndpoints;
    }

    public void setTotalEndpoints(int totalEndpoints) {
        this.totalEndpoints = totalEndpoints;
    }

    public int getSecureEndpoints() {
        return secureEndpoints;
    }

    public void setSecureEndpoints(int secureEndpoints) {
        this.secureEndpoints = secureEndpoints;
    }

    public int getPublicEndpoints() {
        return publicEndpoints;
    }

    public void setPublicEndpoints(int publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }

    public int getEndpointsByGetMapping() {
        return endpointsByGetMapping;
    }

    public void setEndpointsByGetMapping(int endpointsByGetMapping) {
        this.endpointsByGetMapping = endpointsByGetMapping;
    }

    public int getEndpointsByPostMapping() {
        return endpointsByPostMapping;
    }

    public void setEndpointsByPostMapping(int endpointsByPostMapping) {
        this.endpointsByPostMapping = endpointsByPostMapping;
    }

    public int getEndpointsByPutMapping() {
        return endpointsByPutMapping;
    }

    public void setEndpointsByPutMapping(int endpointsByPutMapping) {
        this.endpointsByPutMapping = endpointsByPutMapping;
    }

    public int getEndpointsByDeleteMapping() {
        return endpointsByDeleteMapping;
    }

    public void setEndpointsByDeleteMapping(int endpointsByDeleteMapping) {
        this.endpointsByDeleteMapping = endpointsByDeleteMapping;
    }

    public int getEndpointsByPatchMapping() {
        return endpointsByPatchMapping;
    }

    public void setEndpointsByPatchMapping(int endpointsByPatchMapping) {
        this.endpointsByPatchMapping = endpointsByPatchMapping;
    }

    public int getEndpointsByRequestMapping() {
        return endpointsByRequestMapping;
    }

    public void setEndpointsByRequestMapping(int endpointsByRequestMapping) {
        this.endpointsByRequestMapping = endpointsByRequestMapping;
    }

    public java.util.Map<String, Integer> getEndpointsByController() {
        return endpointsByController;
    }

    public void setEndpointsByController(java.util.Map<String, Integer> endpointsByController) {
        this.endpointsByController = endpointsByController;
    }
}