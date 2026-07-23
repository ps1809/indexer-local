package com.projectiq.indexerlocal.extractor;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.projectiq.indexerlocal.model.AnnotationInfo;
import com.projectiq.indexerlocal.model.FieldInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts field metadata from Java source files.
 */
public class FieldExtractor implements Extractor {

    @Override
    public String getName() {
        return "FieldExtractor";
    }

    @Override
    public void extract(com.github.javaparser.ast.CompilationUnit cu, com.projectiq.indexerlocal.model.FileIndex fileIndex) {
        // Delegate to extractField for individual fields
    }

    public void extractField(FieldDeclaration fieldDecl, FieldInfo fieldInfo) {
        if (fieldDecl.getVariables().isEmpty()) {
            return;
        }

        var var = fieldDecl.getVariables().get(0);
        fieldInfo.setFieldName(var.getName().getId());
        fieldInfo.setFieldType(fieldDecl.getElementType().asString());
        fieldInfo.setVisibility(detectVisibility(fieldDecl));
        fieldInfo.setStatic(detectStatic(fieldDecl));
        fieldInfo.setFinal(detectFinal(fieldDecl));
        fieldInfo.setAnnotations(extractFieldAnnotations(fieldDecl));
    }

    private String detectVisibility(FieldDeclaration fieldDecl) {
        String modStr = fieldDecl.getModifiers().toString().toLowerCase();
        if (modStr.contains("public")) {
            return "PUBLIC";
        } else if (modStr.contains("protected")) {
            return "PROTECTED";
        } else if (modStr.contains("private")) {
            return "PRIVATE";
        }
        return "PACKAGE_PRIVATE";
    }

    private boolean detectStatic(FieldDeclaration fieldDecl) {
        return fieldDecl.getModifiers().toString().toLowerCase().contains("static");
    }

    private boolean detectFinal(FieldDeclaration fieldDecl) {
        return fieldDecl.getModifiers().toString().toLowerCase().contains("final");
    }

    private List<AnnotationInfo> extractFieldAnnotations(FieldDeclaration fieldDecl) {
        List<AnnotationInfo> annotations = new ArrayList<>();
        for (var annotation : fieldDecl.getAnnotations()) {
            AnnotationInfo info = new AnnotationInfo();
            info.setAnnotationName(annotation.getNameAsString());
            if (annotation instanceof NormalAnnotationExpr normal) {
                info.setFullName(annotation.getNameAsString() + "(" + normal.getPairs().stream()
                    .map(p -> p.getName().getId() + "=" + p.getValue().toString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("") + ")");
            } else if (annotation instanceof SingleMemberAnnotationExpr single) {
                info.setFullName(annotation.getNameAsString() + "(" + single.getMemberValue().toString() + ")");
            } else {
                info.setFullName(annotation.getNameAsString());
            }
            annotations.add(info);
        }
        return annotations;
    }
}