package com.projectiq.indexerlocal.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Spring component detected in indexed classes.
 * Stores repository-scoped Spring framework components with full annotation metadata.
 */
public class SpringComponent {

    private Long id;
    private String repositoryId;
    private Long classId;
    private Long fileIndexId;
    private String componentName;
    private String componentType; // COMPONENT, SERVICE, REPOSITORY, CONTROLLER, REST_CONTROLLER, CONFIGURATION, BEAN
    private String className;
    private String packageName;
    private String sourceFile;
    private LocalDateTime detectedAt;

    // Annotation flags
    private boolean isComponent;
    private boolean isService;
    private boolean isRepository;
    private boolean isController;
    private boolean isRestController;
    private boolean isConfiguration;
    private boolean isBean;
    private boolean isConfigurationProperties;
    private boolean isPropertySource;
    private boolean isImport;

    // Dependency injection annotations
    private boolean hasAutowired;
    private boolean hasInject;
    private boolean hasResource;
    private boolean hasConstructorInjection;
    private boolean hasSetterInjection;

    // Web annotations
    private boolean isControllerAdvice;
    private boolean isRestControllerAdvice;
    private boolean hasCrossOrigin;
    private boolean hasResponseBody;

    // Transaction annotations
    private boolean hasTransactional;
    private String transactionPropagation;
    private String transactionIsolation;

    // Scheduling annotations
    private boolean hasEnableScheduling;
    private boolean hasScheduled;

    // Async annotations
    private boolean hasEnableAsync;
    private boolean hasAsync;

    // Caching annotations
    private boolean hasEnableCaching;
    private boolean hasCacheable;
    private boolean hasCachePut;
    private boolean hasCacheEvict;

    // Security annotations
    private boolean hasEnableWebSecurity;
    private boolean hasEnableMethodSecurity;
    private boolean hasPreAuthorize;
    private boolean hasPostAuthorize;
    private boolean hasRolesAllowed;
    private boolean hasSecured;

    // Event and messaging annotations
    private boolean hasEventListener;
    private boolean hasKafkaListener;
    private boolean hasRabbitListener;
    private boolean hasJmsListener;

    // Bean metadata
    private String beanName;
    private List<AnnotationInfo> allAnnotations = new ArrayList<>();

    public SpringComponent() {
        this.detectedAt = LocalDateTime.now();
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getFileIndexId() {
        return fileIndexId;
    }

    public void setFileIndexId(Long fileIndexId) {
        this.fileIndexId = fileIndexId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    // Annotation flag getters and setters

    public boolean isComponent() {
        return isComponent;
    }

    public void setComponent(boolean component) {
        isComponent = component;
    }

    public boolean isService() {
        return isService;
    }

    public void setService(boolean service) {
        isService = service;
    }

    public boolean isRepository() {
        return isRepository;
    }

    public void setRepository(boolean repository) {
        isRepository = repository;
    }

    public boolean isController() {
        return isController;
    }

    public void setController(boolean controller) {
        isController = controller;
    }

    public boolean isRestController() {
        return isRestController;
    }

    public void setRestController(boolean restController) {
        isRestController = restController;
    }

    public boolean isConfiguration() {
        return isConfiguration;
    }

    public void setConfiguration(boolean configuration) {
        isConfiguration = configuration;
    }

    public boolean isBean() {
        return isBean;
    }

    public void setBean(boolean bean) {
        isBean = bean;
    }

    public boolean isConfigurationProperties() {
        return isConfigurationProperties;
    }

    public void setConfigurationProperties(boolean configurationProperties) {
        isConfigurationProperties = configurationProperties;
    }

    public boolean isPropertySource() {
        return isPropertySource;
    }

    public void setPropertySource(boolean propertySource) {
        isPropertySource = propertySource;
    }

    public boolean isImport() {
        return isImport;
    }

    public void setImport(boolean anImport) {
        isImport = anImport;
    }

    public boolean hasAutowired() {
        return hasAutowired;
    }

    public void setAutowired(boolean autowired) {
        hasAutowired = autowired;
    }

    public boolean hasInject() {
        return hasInject;
    }

    public void setInject(boolean inject) {
        hasInject = inject;
    }

    public boolean hasResource() {
        return hasResource;
    }

    public void setResource(boolean resource) {
        hasResource = resource;
    }

    public boolean isHasConstructorInjection() {
        return hasConstructorInjection;
    }

    public void setHasConstructorInjection(boolean hasConstructorInjection) {
        this.hasConstructorInjection = hasConstructorInjection;
    }

    public boolean isHasSetterInjection() {
        return hasSetterInjection;
    }

    public void setHasSetterInjection(boolean hasSetterInjection) {
        this.hasSetterInjection = hasSetterInjection;
    }

    public boolean isControllerAdvice() {
        return isControllerAdvice;
    }

    public void setControllerAdvice(boolean controllerAdvice) {
        isControllerAdvice = controllerAdvice;
    }

    public boolean isRestControllerAdvice() {
        return isRestControllerAdvice;
    }

    public void setRestControllerAdvice(boolean restControllerAdvice) {
        isRestControllerAdvice = restControllerAdvice;
    }

    public boolean hasCrossOrigin() {
        return hasCrossOrigin;
    }

    public void setCrossOrigin(boolean crossOrigin) {
        hasCrossOrigin = crossOrigin;
    }

    public boolean hasResponseBody() {
        return hasResponseBody;
    }

    public void setResponseBody(boolean responseBody) {
        hasResponseBody = responseBody;
    }

    public boolean hasTransactional() {
        return hasTransactional;
    }

    public void setTransactional(boolean transactional) {
        hasTransactional = transactional;
    }

    public String getTransactionPropagation() {
        return transactionPropagation;
    }

    public void setTransactionPropagation(String transactionPropagation) {
        this.transactionPropagation = transactionPropagation;
    }

    public String getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(String transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }

    public boolean isHasEnableScheduling() {
        return hasEnableScheduling;
    }

    public void setHasEnableScheduling(boolean hasEnableScheduling) {
        this.hasEnableScheduling = hasEnableScheduling;
    }

    public boolean hasScheduled() {
        return hasScheduled;
    }

    public void setScheduled(boolean scheduled) {
        hasScheduled = scheduled;
    }

    public boolean isHasEnableAsync() {
        return hasEnableAsync;
    }

    public void setHasEnableAsync(boolean hasEnableAsync) {
        this.hasEnableAsync = hasEnableAsync;
    }

    public boolean hasAsync() {
        return hasAsync;
    }

    public void setAsync(boolean async) {
        hasAsync = async;
    }

    public boolean isHasEnableCaching() {
        return hasEnableCaching;
    }

    public void setHasEnableCaching(boolean hasEnableCaching) {
        this.hasEnableCaching = hasEnableCaching;
    }

    public boolean hasCacheable() {
        return hasCacheable;
    }

    public void setCacheable(boolean cacheable) {
        hasCacheable = cacheable;
    }

    public boolean hasCachePut() {
        return hasCachePut;
    }

    public void setCachePut(boolean cachePut) {
        hasCachePut = cachePut;
    }

    public boolean hasCacheEvict() {
        return hasCacheEvict;
    }

    public void setCacheEvict(boolean cacheEvict) {
        hasCacheEvict = cacheEvict;
    }

    public boolean isHasEnableWebSecurity() {
        return hasEnableWebSecurity;
    }

    public void setHasEnableWebSecurity(boolean hasEnableWebSecurity) {
        this.hasEnableWebSecurity = hasEnableWebSecurity;
    }

    public boolean isHasEnableMethodSecurity() {
        return hasEnableMethodSecurity;
    }

    public void setHasEnableMethodSecurity(boolean hasEnableMethodSecurity) {
        this.hasEnableMethodSecurity = hasEnableMethodSecurity;
    }

    public boolean hasPreAuthorize() {
        return hasPreAuthorize;
    }

    public void setPreAuthorize(boolean preAuthorize) {
        hasPreAuthorize = preAuthorize;
    }

    public boolean hasPostAuthorize() {
        return hasPostAuthorize;
    }

    public void setPostAuthorize(boolean postAuthorize) {
        hasPostAuthorize = postAuthorize;
    }

    public boolean hasRolesAllowed() {
        return hasRolesAllowed;
    }

    public void setRolesAllowed(boolean rolesAllowed) {
        hasRolesAllowed = rolesAllowed;
    }

    public boolean hasSecured() {
        return hasSecured;
    }

    public void setSecured(boolean secured) {
        hasSecured = secured;
    }

    public boolean hasEventListener() {
        return hasEventListener;
    }

    public void setEventListener(boolean eventListener) {
        hasEventListener = eventListener;
    }

    public boolean hasKafkaListener() {
        return hasKafkaListener;
    }

    public void setKafkaListener(boolean kafkaListener) {
        hasKafkaListener = kafkaListener;
    }

    public boolean hasRabbitListener() {
        return hasRabbitListener;
    }

    public void setRabbitListener(boolean rabbitListener) {
        hasRabbitListener = rabbitListener;
    }

    public boolean hasJmsListener() {
        return hasJmsListener;
    }

    public void setJmsListener(boolean jmsListener) {
        hasJmsListener = jmsListener;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public List<AnnotationInfo> getAllAnnotations() {
        return allAnnotations;
    }

    public void setAllAnnotations(List<AnnotationInfo> allAnnotations) {
        this.allAnnotations = allAnnotations;
    }

    public void addAnnotation(AnnotationInfo annotation) {
        if (this.allAnnotations == null) {
            this.allAnnotations = new ArrayList<>();
        }
        this.allAnnotations.add(annotation);
    }

    @Override
    public String toString() {
        return "SpringComponent{" +
                "repositoryId='" + repositoryId + '\'' +
                ", componentName='" + componentName + '\'' +
                ", componentType='" + componentType + '\'' +
                ", className='" + className + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}