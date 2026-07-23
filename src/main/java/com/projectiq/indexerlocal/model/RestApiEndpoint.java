package com.projectiq.indexerlocal.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single REST API endpoint discovered in a Spring controller.
 */
public class RestApiEndpoint {

    private Long id;
    private String repositoryId;
    
    // Controller information
    private String controllerName;
    private String controllerPackageName;
    private String basepath;
    
    // Endpoint information
    private String endpointPath;
    private String httpMethod;
    private String methodName;
    private String className;
    
    // Request metadata
    private List<RestApiParameter> pathVariables = new ArrayList<>();
    private List<RestApiParameter> requestParams = new ArrayList<>();
    private RestApiParameter requestBody;
    private List<RestApiParameter> requestHeaders = new ArrayList<>();
    private RestApiParameter cookieValue;
    
    // Response metadata
    private String returnType;
    private boolean responseEntityUsed;
    private String producesMediaType;
    private String consumesMediaType;
    
    // Validation
    private boolean validAnnotation;
    private boolean validatedAnnotation;
    
    // Security
    private boolean preAuthorize;
    private boolean postAuthorize;
    private boolean rolesAllowed;
    private boolean secured;

    public RestApiEndpoint() {
    }

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

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public String getControllerPackageName() {
        return controllerPackageName;
    }

    public void setControllerPackageName(String controllerPackageName) {
        this.controllerPackageName = controllerPackageName;
    }

    public String getBasepath() {
        return basepath;
    }

    public void setBasepath(String basepath) {
        this.basepath = basepath;
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<RestApiParameter> getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(List<RestApiParameter> pathVariables) {
        this.pathVariables = pathVariables;
    }

    public List<RestApiParameter> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(List<RestApiParameter> requestParams) {
        this.requestParams = requestParams;
    }

    public RestApiParameter getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(RestApiParameter requestBody) {
        this.requestBody = requestBody;
    }

    public List<RestApiParameter> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(List<RestApiParameter> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public RestApiParameter getCookieValue() {
        return cookieValue;
    }

    public void setCookieValue(RestApiParameter cookieValue) {
        this.cookieValue = cookieValue;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public boolean isResponseEntityUsed() {
        return responseEntityUsed;
    }

    public void setResponseEntityUsed(boolean responseEntityUsed) {
        this.responseEntityUsed = responseEntityUsed;
    }

    public String getProducesMediaType() {
        return producesMediaType;
    }

    public void setProducesMediaType(String producesMediaType) {
        this.producesMediaType = producesMediaType;
    }

    public String getConsumesMediaType() {
        return consumesMediaType;
    }

    public void setConsumesMediaType(String consumesMediaType) {
        this.consumesMediaType = consumesMediaType;
    }

    public boolean isValidAnnotation() {
        return validAnnotation;
    }

    public void setValidAnnotation(boolean validAnnotation) {
        this.validAnnotation = validAnnotation;
    }

    public boolean isValidatedAnnotation() {
        return validatedAnnotation;
    }

    public void setValidatedAnnotation(boolean validatedAnnotation) {
        this.validatedAnnotation = validatedAnnotation;
    }

    public boolean isPreAuthorize() {
        return preAuthorize;
    }

    public void setPreAuthorize(boolean preAuthorize) {
        this.preAuthorize = preAuthorize;
    }

    public boolean isPostAuthorize() {
        return postAuthorize;
    }

    public void setPostAuthorize(boolean postAuthorize) {
        this.postAuthorize = postAuthorize;
    }

    public boolean isRolesAllowed() {
        return rolesAllowed;
    }

    public void setRolesAllowed(boolean rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    /**
     * Check if the endpoint requires authentication.
     */
    public boolean isSecure() {
        return preAuthorize || postAuthorize || rolesAllowed || secured;
    }

    /**
     * Get the full URL path for this endpoint.
     */
    public String getFullUrlPath() {
        String basePath = (basepath != null && !basepath.isEmpty()) ? basepath : "";
        if (basePath.startsWith("/") && endpointPath != null && endpointPath.startsWith("/")) {
            return basePath + endpointPath;
        } else if (basePath.isEmpty()) {
            return "/" + (endpointPath != null ? endpointPath : "");
        } else {
            return basePath + "/" + (endpointPath != null ? endpointPath : "");
        }
    }

    /**
     * Get the uppercase HTTP method for Swagger documentation.
     */
    public String getHttpMethodLabel() {
        if (httpMethod != null) {
            return httpMethod.toUpperCase();
        }
        return "UNKNOWN";
    }
}