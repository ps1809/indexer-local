package com.projectiq.indexerlocal.model;

/**
 * Represents a REST API parameter (path variable, request param, request body, header, cookie).
 */
public class RestApiParameter {

    private Long id;
    private String name;
    private String type;
    private boolean required;
    private String defaultValue;
    private String description;

    public RestApiParameter() {
    }

    public RestApiParameter(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}