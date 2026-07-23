package com.projectiq.indexerlocal.model.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for registering a new repository.
 */
@Schema(description = "Request body for repository registration")
public class RepositoryRegisterRequest {

    @Schema(description = "Filesystem path to the repository", example = "/home/user/projects/my-app", required = true)
    private String path;

    public RepositoryRegisterRequest() {
    }

    public RepositoryRegisterRequest(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}