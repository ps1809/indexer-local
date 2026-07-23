package com.projectiq.indexerlocal.model.testsupport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Request model for generating structured unit test context.
 */
@Schema(description = "Request for generating test context")
public class TestSupportContextRequest {

    @Schema(description = "List of class IDs to include in context")
    private List<Long> classIds;

    @Schema(description = "List of method IDs to include in context")
    private List<Long> methodIds;

    @Schema(description = "Whether to include Spring context")
    private boolean includeSpringContext = true;

    @Schema(description = "Whether to include dependency context")
    private boolean includeDependencyContext = true;

    @Schema(description = "Whether to include only public methods")
    private boolean publicMethodsOnly = false;

    public TestSupportContextRequest() {
    }

    // Getters and Setters

    public List<Long> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<Long> classIds) {
        this.classIds = classIds;
    }

    public List<Long> getMethodIds() {
        return methodIds;
    }

    public void setMethodIds(List<Long> methodIds) {
        this.methodIds = methodIds;
    }

    public boolean isIncludeSpringContext() {
        return includeSpringContext;
    }

    public void setIncludeSpringContext(boolean includeSpringContext) {
        this.includeSpringContext = includeSpringContext;
    }

    public boolean isIncludeDependencyContext() {
        return includeDependencyContext;
    }

    public void setIncludeDependencyContext(boolean includeDependencyContext) {
        this.includeDependencyContext = includeDependencyContext;
    }

    public boolean isPublicMethodsOnly() {
        return publicMethodsOnly;
    }

    public void setPublicMethodsOnly(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
    }
}