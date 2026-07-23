package com.projectiq.indexerlocal.model.testsupport;

import com.projectiq.indexerlocal.model.AnnotationInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Represents field context metadata for unit test generation support.
 */
@Schema(description = "Field context metadata for test generation")
public class TestSupportFieldContext {

    @Schema(description = "Field ID in the index")
    private Long fieldId;

    @Schema(description = "Class ID this field belongs to")
    private Long classId;

    @Schema(description = "Field name")
    private String fieldName;

    @Schema(description = "Field type")
    private String fieldType;

    @Schema(description = "Visibility modifier (PUBLIC, PROTECTED, PRIVATE)")
    private String visibility;

    @Schema(description = "Whether the field is static")
    private boolean isStatic;

    @Schema(description = "Whether the field is final")
    private boolean isFinal;

    @Schema(description = "Default value if any")
    private String defaultValue;

    @Schema(description = "Field-level annotations")
    private List<AnnotationInfo> annotations;

    public TestSupportFieldContext() {
    }

    // Getters and Setters

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationInfo> annotations) {
        this.annotations = annotations;
    }
}