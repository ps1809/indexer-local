package com.projectiq.indexerlocal.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata extracted from a Java field declaration.
 */
public class FieldInfo {
    
    private Long id;
    private Long classId;
    private String fieldName;
    private String fieldType;
    private String visibility;
    private boolean isStatic;
    private boolean isFinal;
    private List<AnnotationInfo> annotations;
    
    public FieldInfo() {
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

    public List<AnnotationInfo> getAnnotations() {
        if (annotations == null) {
            annotations = new ArrayList<>();
        }
        return annotations;
    }

    public void setAnnotations(List<AnnotationInfo> annotations) {
        this.annotations = annotations;
    }
}
