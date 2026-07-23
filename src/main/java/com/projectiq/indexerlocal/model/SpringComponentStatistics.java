package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Aggregated statistics for Spring components in a repository.
 */
public class SpringComponentStatistics {

    private String repositoryId;
    private LocalDateTime generatedAt;

    // Component counts by stereotype
    private int totalComponents;
    private int serviceCount;
    private int repositoryCount;
    private int controllerCount;
    private int restControllerCount;
    private int componentBeanCount;
    private int configurationClassCount;
    private int beanMethodCount;

    // Dependency injection counts
    private int autowiredCount;
    private int injectCount;
    private int resourceCount;
    private int constructorInjectionCount;
    private int setterInjectionCount;

    // Web annotation counts
    private int controllerAdviceCount;
    private int restControllerAdviceCount;
    private int crossOriginCount;
    private int responseBodyCount;

    // Transaction counts
    private int transactionalCount;

    // Scheduling counts
    private int scheduledJobCount;

    // Async counts
    private int asyncMethodCount;

    // Caching counts
    private int cacheableCount;
    private int cachePutCount;
    private int cacheEvictCount;

    // Security counts
    private int preAuthorizeCount;
    private int postAuthorizeCount;
    private int rolesAllowedCount;
    private int securedCount;

    // Event and messaging counts
    private int eventListenerCount;
    private int kafkaListenerCount;
    private int rabbitListenerCount;
    private int jmsListenerCount;

    // Configuration file counts
    private int configurationPropertiesCount;
    private int propertySourceCount;
    private int importAnnotationCount;

    /**
     * Component type counts (stereotype breakdown).
     */
    private Map<String, Integer> componentTypeCounts = new HashMap<>();

    /**
     * Annotation usage counts across all components.
     */
    private Map<String, Integer> annotationCounts = new HashMap<>();

    public SpringComponentStatistics() {
        this.generatedAt = LocalDateTime.now();
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalComponents() {
        return totalComponents;
    }

    public void setTotalComponents(int totalComponents) {
        this.totalComponents = totalComponents;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public int getRepositoryCount() {
        return repositoryCount;
    }

    public void setRepositoryCount(int repositoryCount) {
        this.repositoryCount = repositoryCount;
    }

    public int getControllerCount() {
        return controllerCount;
    }

    public void setControllerCount(int controllerCount) {
        this.controllerCount = controllerCount;
    }

    public int getRestControllerCount() {
        return restControllerCount;
    }

    public void setRestControllerCount(int restControllerCount) {
        this.restControllerCount = restControllerCount;
    }

    public int getComponentBeanCount() {
        return componentBeanCount;
    }

    public void setComponentBeanCount(int componentBeanCount) {
        this.componentBeanCount = componentBeanCount;
    }

    public int getConfigurationClassCount() {
        return configurationClassCount;
    }

    public void setConfigurationClassCount(int configurationClassCount) {
        this.configurationClassCount = configurationClassCount;
    }

    public int getBeanMethodCount() {
        return beanMethodCount;
    }

    public void setBeanMethodCount(int beanMethodCount) {
        this.beanMethodCount = beanMethodCount;
    }

    public int getAutowiredCount() {
        return autowiredCount;
    }

    public void setAutowiredCount(int autowiredCount) {
        this.autowiredCount = autowiredCount;
    }

    public int getInjectCount() {
        return injectCount;
    }

    public void setInjectCount(int injectCount) {
        this.injectCount = injectCount;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(int resourceCount) {
        this.resourceCount = resourceCount;
    }

    public int getConstructorInjectionCount() {
        return constructorInjectionCount;
    }

    public void setConstructorInjectionCount(int constructorInjectionCount) {
        this.constructorInjectionCount = constructorInjectionCount;
    }

    public int getSetterInjectionCount() {
        return setterInjectionCount;
    }

    public void setSetterInjectionCount(int setterInjectionCount) {
        this.setterInjectionCount = setterInjectionCount;
    }

    public int getControllerAdviceCount() {
        return controllerAdviceCount;
    }

    public void setControllerAdviceCount(int controllerAdviceCount) {
        this.controllerAdviceCount = controllerAdviceCount;
    }

    public int getRestControllerAdviceCount() {
        return restControllerAdviceCount;
    }

    public void setRestControllerAdviceCount(int restControllerAdviceCount) {
        this.restControllerAdviceCount = restControllerAdviceCount;
    }

    public int getCrossOriginCount() {
        return crossOriginCount;
    }

    public void setCrossOriginCount(int crossOriginCount) {
        this.crossOriginCount = crossOriginCount;
    }

    public int getResponseBodyCount() {
        return responseBodyCount;
    }

    public void setResponseBodyCount(int responseBodyCount) {
        this.responseBodyCount = responseBodyCount;
    }

    public int getTransactionalCount() {
        return transactionalCount;
    }

    public void setTransactionalCount(int transactionalCount) {
        this.transactionalCount = transactionalCount;
    }

    public int getScheduledJobCount() {
        return scheduledJobCount;
    }

    public void setScheduledJobCount(int scheduledJobCount) {
        this.scheduledJobCount = scheduledJobCount;
    }

    public int getAsyncMethodCount() {
        return asyncMethodCount;
    }

    public void setAsyncMethodCount(int asyncMethodCount) {
        this.asyncMethodCount = asyncMethodCount;
    }

    public int getCacheableCount() {
        return cacheableCount;
    }

    public void setCacheableCount(int cacheableCount) {
        this.cacheableCount = cacheableCount;
    }

    public int getCachePutCount() {
        return cachePutCount;
    }

    public void setCachePutCount(int cachePutCount) {
        this.cachePutCount = cachePutCount;
    }

    public int getCacheEvictCount() {
        return cacheEvictCount;
    }

    public void setCacheEvictCount(int cacheEvictCount) {
        this.cacheEvictCount = cacheEvictCount;
    }

    public int getPreAuthorizeCount() {
        return preAuthorizeCount;
    }

    public void setPreAuthorizeCount(int preAuthorizeCount) {
        this.preAuthorizeCount = preAuthorizeCount;
    }

    public int getPostAuthorizeCount() {
        return postAuthorizeCount;
    }

    public void setPostAuthorizeCount(int postAuthorizeCount) {
        this.postAuthorizeCount = postAuthorizeCount;
    }

    public int getRolesAllowedCount() {
        return rolesAllowedCount;
    }

    public void setRolesAllowedCount(int rolesAllowedCount) {
        this.rolesAllowedCount = rolesAllowedCount;
    }

    public int getSecuredCount() {
        return securedCount;
    }

    public void setSecuredCount(int securedCount) {
        this.securedCount = securedCount;
    }

    public int getEventListenerCount() {
        return eventListenerCount;
    }

    public void setEventListenerCount(int eventListenerCount) {
        this.eventListenerCount = eventListenerCount;
    }

    public int getKafkaListenerCount() {
        return kafkaListenerCount;
    }

    public void setKafkaListenerCount(int kafkaListenerCount) {
        this.kafkaListenerCount = kafkaListenerCount;
    }

    public int getRabbitListenerCount() {
        return rabbitListenerCount;
    }

    public void setRabbitListenerCount(int rabbitListenerCount) {
        this.rabbitListenerCount = rabbitListenerCount;
    }

    public int getJmsListenerCount() {
        return jmsListenerCount;
    }

    public void setJmsListenerCount(int jmsListenerCount) {
        this.jmsListenerCount = jmsListenerCount;
    }

    public int getConfigurationPropertiesCount() {
        return configurationPropertiesCount;
    }

    public void setConfigurationPropertiesCount(int configurationPropertiesCount) {
        this.configurationPropertiesCount = configurationPropertiesCount;
    }

    public int getPropertySourceCount() {
        return propertySourceCount;
    }

    public void setPropertySourceCount(int propertySourceCount) {
        this.propertySourceCount = propertySourceCount;
    }

    public int getImportAnnotationCount() {
        return importAnnotationCount;
    }

    public void setImportAnnotationCount(int importAnnotationCount) {
        this.importAnnotationCount = importAnnotationCount;
    }

    public Map<String, Integer> getComponentTypeCounts() {
        return componentTypeCounts;
    }

    public void setComponentTypeCounts(Map<String, Integer> componentTypeCounts) {
        this.componentTypeCounts = componentTypeCounts;
    }

    public Map<String, Integer> getAnnotationCounts() {
        return annotationCounts;
    }

    public void setAnnotationCounts(Map<String, Integer> annotationCounts) {
        this.annotationCounts = annotationCounts;
    }

    /**
     * Get component types breakdown as a map.
     */
    public Map<String, Integer> getComponentTypeBreakdown() {
        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("COMPONENT", componentBeanCount);
        breakdown.put("SERVICE", serviceCount);
        breakdown.put("REPOSITORY", repositoryCount);
        breakdown.put("CONTROLLER", controllerCount);
        breakdown.put("REST_CONTROLLER", restControllerCount);
        breakdown.put("CONFIGURATION", configurationClassCount);
        return breakdown;
    }

    /**
     * Get summary as a map for easy serialization.
     */
    public Map<String, Object> toSummaryMap() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("repositoryId", repositoryId);
        summary.put("generatedAt", generatedAt.toString());
        summary.put("totalComponents", totalComponents);
        summary.put("componentTypes", getComponentTypeBreakdown());
        summary.put("diAnnotations", Map.of(
            "autowired", autowiredCount,
            "inject", injectCount,
            "resource", resourceCount
        ));
        summary.put("transactions", transactionalCount);
        summary.put("scheduledJobs", scheduledJobCount);
        summary.put("asyncMethods", asyncMethodCount);
        summary.put("cacheOperations", Map.of(
            "cacheable", cacheableCount,
            "cachePut", cachePutCount,
            "cacheEvict", cacheEvictCount
        ));
        summary.put("securityAnnotations", Map.of(
            "preAuthorize", preAuthorizeCount,
            "postAuthorize", postAuthorizeCount,
            "rolesAllowed", rolesAllowedCount,
            "secured", securedCount
        ));
        summary.put("eventListeners", eventListenerCount);
        summary.put("messagingListeners", Map.of(
            "kafka", kafkaListenerCount,
            "rabbit", rabbitListenerCount,
            "jms", jmsListenerCount
        ));
        return summary;
    }
}