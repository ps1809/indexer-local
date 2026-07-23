package com.projectiq.indexerlocal.extractor;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.VoidType;
import com.projectiq.indexerlocal.model.AnnotationInfo;
import com.projectiq.indexerlocal.model.MethodInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts method metadata from Java source files.
 */
public class MethodInfoExtractor implements Extractor {

    @Override
    public String getName() {
        return "MethodInfoExtractor";
    }

    @Override
    public void extract(com.github.javaparser.ast.CompilationUnit cu, com.projectiq.indexerlocal.model.FileIndex fileIndex) {
        // Delegate to extractMethod for individual methods
    }

    public void extractMethod(MethodDeclaration methodDecl, MethodInfo methodInfo) {
        methodInfo.setMethodName(methodDecl.getName().getId());
        methodInfo.setReturnType(extractReturnType(methodDecl));
        methodInfo.setVisibility(detectVisibility(methodDecl));
        methodInfo.setStatic(detectStatic(methodDecl));
        methodInfo.setAbstract(detectAbstract(methodDecl));
        methodInfo.setParameters(extractParameters(methodDecl));
        methodInfo.setExceptions(extractThrownTypes(methodDecl));
        methodInfo.setAnnotations(extractAnnotations(methodDecl));
    }

    private String extractReturnType(MethodDeclaration decl) {
        return "void";
    }

    private String detectVisibility(MethodDeclaration decl) {
        String modStr = decl.getModifiers().toString().toLowerCase();
        if (modStr.contains("public")) {
            return "PUBLIC";
        } else if (modStr.contains("protected")) {
            return "PROTECTED";
        } else if (modStr.contains("private")) {
            return "PRIVATE";
        }
        return "PACKAGE_PRIVATE";
    }

    private boolean detectStatic(MethodDeclaration decl) {
        return decl.getModifiers().toString().toLowerCase().contains("static");
    }

    private boolean detectAbstract(MethodDeclaration decl) {
        return decl.getModifiers().toString().toLowerCase().contains("abstract");
    }

    private List<String> extractParameters(MethodDeclaration decl) {
        return decl.getParameters().stream()
                .map(p -> p.getName().getId() + " : " + p.getType().asString())
                .toList();
    }

    private List<String> extractThrownTypes(MethodDeclaration decl) {
        return new ArrayList<>();
    }

    private List<AnnotationInfo> extractAnnotations(MethodDeclaration decl) {
        List<AnnotationInfo> annotations = new ArrayList<>();
        for (var annotation : decl.getAnnotations()) {
            AnnotationInfo info = new AnnotationInfo();
            info.setAnnotationName(annotation.getNameAsString());
            info.setFullName(annotation.getNameAsString());
            annotations.add(info);
        }
        return annotations;
    }
}