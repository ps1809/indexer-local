package com.projectiq.indexerlocal.exception;

/**
 * Exception thrown when attempting to index a repository workspace that contains no Java source files.
 * This prevents successful indexing of empty workspaces and provides a meaningful error message.
 */
public class NoJavaFilesException extends RuntimeException {

    private final String repositoryId;
    private final String workspacePath;

    /**
     * Creates a new NoJavaFilesException for the specified repository and workspace.
     *
     * @param repositoryId the ID of the repository being indexed
     * @param workspacePath the path to the workspace directory
     */
    public NoJavaFilesException(String repositoryId, String workspacePath) {
        super("Repository workspace contains no Java source files.");
        this.repositoryId = repositoryId;
        this.workspacePath = workspacePath;
    }

    /**
     * Gets the repository ID that was being indexed.
     *
     * @return the repository ID
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * Gets the workspace path that was searched.
     *
     * @return the workspace path
     */
    public String getWorkspacePath() {
        return workspacePath;
    }
}