package com.projectiq.indexerlocal.model;

import com.github.javaparser.ast.expr.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents metadata extracted from a Java annotation.
 */
public class AnnotationInfo {
    
    private Long id;
    private String annotationName;
    private String fullName;
    private Map<String, Object> values;
    private String targetType; // CLASS, INTERFACE, ENUM, RECORD, METHOD, FIELD, PARAMETER, CONSTRUCTOR
    private Long targetId;

    public static AnnotationInfo from(AnnotationExpr annotation) {
        if (annotation == null) {
            return null;
        }
        AnnotationInfo info = new AnnotationInfo();
        String name = annotation.getNameAsString();
        // Remove @ prefix if present
        if (name.startsWith("@")) {
            name = name.substring(1);
        }
        info.setAnnotationName(name);
        info.setFullName(annotation.getNameAsString());

        if (annotation instanceof SingleMemberAnnotationExpr single) {
            String value = extractExpressionString(single.getMemberValue());
            info.setValues(Map.of("value", value));
        } else if (annotation instanceof NormalAnnotationExpr normal) {
            Map<String, Object> valuesMap = new HashMap<>();
            for (MemberValuePair pair : normal.getPairs()) {
                String val = extractExpressionString(pair.getValue());
                valuesMap.put(pair.getName().getId(), val);
            }
            info.setValues(valuesMap.isEmpty() ? null : valuesMap);
        }
        // MarkerAnnotationExpr has no extra data
        return info;
    }

    private static String extractExpressionString(Expression expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof StringLiteralExpr str) {
            return str.getValue();
        } else if (expr instanceof ClassExpr cls) {
            return cls.getType().asString() + ".class";
        } else if (expr instanceof IntegerLiteralExpr integer) {
            return integer.getValue().toString();
        } else if (expr instanceof LongLiteralExpr longLit) {
            return longLit.getValue().toString();
        } else if (expr instanceof com.github.javaparser.ast.nodeTypes.NodeWithName<?> named) {
            return named.getNameAsString();
        } else if (expr instanceof ArrayInitializerExpr array) {
            List<String> elements = new ArrayList<>();
            for (Expression e : array.getValues()) {
                elements.add(extractExpressionString(e));
            }
            return "{" + String.join(", ", elements) + "}";
        }
        return expr.toString();
    }
    
    public AnnotationInfo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public void setAnnotationName(String annotationName) {
        this.annotationName = annotationName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
