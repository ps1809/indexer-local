package com.projectiq.indexerlocal.extractor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.projectiq.indexerlocal.model.AnnotationInfo;
import com.projectiq.indexerlocal.model.ClassInfo;
import com.projectiq.indexerlocal.model.FileIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Extracts class/interface/enum/record metadata from Java source files.
 */
public class ClassExtractor implements Extractor {

    @Override
    public String getName() {
        return "ClassExtractor";
    }

    @Override
    public void extract(CompilationUnit cu, FileIndex fileIndex) {
        List<ClassInfo> classes = new ArrayList<>();

        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            ClassInfo classInfo = new ClassInfo();
            classInfo.setClassName(classDecl.getNameAsString());
            classInfo.setClassType(detectClassType(classDecl));
            classInfo.setVisibility(detectVisibility(classDecl.getModifiers()));
            classInfo.setSuperClass(extractSuperClass(classDecl));
            classInfo.setInterfaces(extractInterfaces(classDecl));
            classInfo.setAnnotations(toAnnotationInfos(classDecl.getAnnotations()));

            classes.add(classInfo);
        }

        for (EnumDeclaration enumDecl : cu.findAll(EnumDeclaration.class)) {
            ClassInfo classInfo = new ClassInfo();
            classInfo.setClassName(enumDecl.getNameAsString());
            classInfo.setClassType("ENUM");
            classInfo.setVisibility(detectVisibility(enumDecl.getModifiers()));
            classInfo.setAnnotations(toAnnotationInfos(enumDecl.getAnnotations()));

            classes.add(classInfo);
        }

        for (AnnotationDeclaration annDecl : cu.findAll(AnnotationDeclaration.class)) {
            ClassInfo classInfo = new ClassInfo();
            classInfo.setClassName(annDecl.getNameAsString());
            classInfo.setClassType("ANNOTATION");
            classInfo.setVisibility(detectVisibility(annDecl.getModifiers()));
            classInfo.setAnnotations(toAnnotationInfos(annDecl.getAnnotations()));

            classes.add(classInfo);
        }

        fileIndex.setClasses(classes);
        fileIndex.setClassCount(classes.size());
    }

    private String detectClassType(ClassOrInterfaceDeclaration decl) {
        String modStr = decl.getModifiers().toString().toLowerCase();
        if (modStr.contains("record")) {
            return "RECORD";
        } else if (decl.isInterface()) {
            return "INTERFACE";
        }
        return "CLASS";
    }

    private String detectVisibility(Object modifiers) {
        if (modifiers == null) {
            return "PACKAGE_PRIVATE";
        }
        String modStr = modifiers.toString().toLowerCase();
        if (modStr.contains("public")) {
            return "PUBLIC";
        } else if (modStr.contains("protected")) {
            return "PROTECTED";
        } else if (modStr.contains("private")) {
            return "PRIVATE";
        }
        return "PACKAGE_PRIVATE";
    }

    private String extractSuperClass(ClassOrInterfaceDeclaration decl) {
        return decl.getExtendedTypes().isEmpty() ? null : decl.getExtendedTypes().get(0).getNameAsString();
    }

    private List<String> extractInterfaces(ClassOrInterfaceDeclaration decl) {
        List<String> interfaces = new ArrayList<>();
        for (var type : decl.getImplementedTypes()) {
            interfaces.add(type.getNameAsString());
        }
        return interfaces;
    }

    private List<AnnotationInfo> toAnnotationInfos(List<AnnotationExpr> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return null;
        }
        List<AnnotationInfo> result = new ArrayList<>();
        for (AnnotationExpr annotation : annotations) {
            AnnotationInfo info = AnnotationInfo.from(annotation);
            if (info != null) {
                result.add(info);
            }
        }
        return result.isEmpty() ? null : result;
    }
}