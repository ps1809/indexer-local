package com.projectiq.indexerlocal.model.springsearch;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a single Spring component search result.
 * Contains all relevant metadata for deterministic Spring artifact discovery.
 */
@Schema(description = "Spring component search result with full metadata")
public class SpringSearchResult {

    @Schema(description = "Component name (class simple name)", example = "UserController")
    private String componentName;

    @Schema(description = "Component type", example = "REST_CONTROLLER",
            allowableValues = {"COMPONENT", "SERVICE", "REPOSITORY", "CONTROLLER",
                               "REST_CONTROLLER", "CONFIGURATION", "BEAN", "REST_ENDPOINT"})
    private String componentType;

    @Schema(description = "Spring annotation", example = "@RestController")
    private String annotation;

    @Schema(description = "Fully qualified package name", example = "com.example.controller")
    private String packageName;

    @Schema(description = "Repository ID", example = "my-repo")
    private String repositoryId;

    @Schema(description = "Module name derived from file path", example = "my-module")
    private String module;

    @Schema(description = "Source file path", example = "/workspace/my-repo/src/main/java/com/example/controller/UserController.java")
    private String filePath;

    @Schema(description = "Bean name if applicable", example = "userService")
    private String beanName;

    @Schema(description = "REST endpoint path if applicable", example = "/api/users")
    private String restPath;

    @Schema(description = "HTTP method if applicable", example = "GET")
    private String httpMethod;

    @Schema(description = "Line number in source file", example = "15")
    private Integer lineNumber;

    @Schema(description = "Class name", example = "UserController")
    private String className;

    public SpringSearchResult() {
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

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getRestPath() {
        return restPath;
    }

    public void setRestPath(String restPath) {
        this.restPath = restPath;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}