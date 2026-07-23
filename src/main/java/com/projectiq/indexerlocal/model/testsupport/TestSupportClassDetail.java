package com.projectiq.indexerlocal.model.testsupport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Represents complete class detail including all methods, fields, constructors,
 * and Spring context for unit test generation support.
 */
@Schema(description = "Complete class detail for test generation")
public class TestSupportClassDetail {

    @Schema(description = "Class context metadata")
    private TestSupportClassContext classContext;

    @Schema(description = "Constructor methods")
    private List<TestSupportMethodContext> constructors;

    @Schema(description = "Regular methods")
    private List<TestSupportMethodContext> methods;

    @Schema(description = "Fields")
    private List<TestSupportFieldContext> fields;

    @Schema(description = "Spring component context if applicable")
    private TestSupportSpringContext springContext;

    public TestSupportClassDetail() {
    }

    // Getters and Setters

    public TestSupportClassContext getClassContext() {
        return classContext;
    }

    public void setClassContext(TestSupportClassContext classContext) {
        this.classContext = classContext;
    }

    public List<TestSupportMethodContext> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<TestSupportMethodContext> constructors) {
        this.constructors = constructors;
    }

    public List<TestSupportMethodContext> getMethods() {
        return methods;
    }

    public void setMethods(List<TestSupportMethodContext> methods) {
        this.methods = methods;
    }

    public List<TestSupportFieldContext> getFields() {
        return fields;
    }

    public void setFields(List<TestSupportFieldContext> fields) {
        this.fields = fields;
    }

    public TestSupportSpringContext getSpringContext() {
        return springContext;
    }

    public void setSpringContext(TestSupportSpringContext springContext) {
        this.springContext = springContext;
    }
}