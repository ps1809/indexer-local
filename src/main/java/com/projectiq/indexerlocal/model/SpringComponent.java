package com.projectiq.indexerlocal.model;

/**
 * Represents a Spring component detected in indexed classes.
 * Tracks classes/fields/methods annotated with Spring stereotypes.
 */
public class SpringComponent {
    
    private Long id;
    private Long classId;
    private String componentType; // COMPONENT, SERVICE, REPOSITORY, CONTROLLER, REST_CONTROLLER, CONFIGURATION, BEAN, CONSUMER, PUBLISHER
    private String className;
    private Long fileIndexId;

    public SpringComponent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
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

    public Long getFileIndexId() {
        return fileIndexId;
    }

    public void setFileIndexId(Long fileIndexId) {
        this.fileIndexId = fileIndexId;
    }
}