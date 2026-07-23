package com.projectiq.indexerlocal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for workspace settings.
 */
@Component
@ConfigurationProperties(prefix = "indexer.workspace")
public class WorkspaceProperties {

    /**
     * Root directory for storing managed workspaces.
     */
    private String rootDir = "./workspace";

    /**
     * Whether to auto-create the workspace directory on startup.
     */
    private boolean autoCreate = true;

    /**
     * Whether to verify the workspace is writable on startup.
     */
    private boolean verifyWritable = true;

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    public boolean isVerifyWritable() {
        return verifyWritable;
    }

    public void setVerifyWritable(boolean verifyWritable) {
        this.verifyWritable = verifyWritable;
    }
}