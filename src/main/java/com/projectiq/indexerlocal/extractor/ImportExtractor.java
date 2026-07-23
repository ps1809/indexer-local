package com.projectiq.indexerlocal.extractor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.projectiq.indexerlocal.model.FileIndex;
import com.projectiq.indexerlocal.model.ImportInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts import statements from Java source files.
 */
public class ImportExtractor implements Extractor {

    @Override
    public String getName() {
        return "ImportExtractor";
    }

    @Override
    public void extract(CompilationUnit cu, FileIndex fileIndex) {
        List<ImportInfo> imports = new ArrayList<>();

        for (ImportDeclaration imp : cu.getImports()) {
            ImportInfo info = new ImportInfo();
            info.setImportName(imp.getName().asString());
            info.setStatic(imp.isStatic());
            imports.add(info);
        }

        fileIndex.setImports(imports);
    }
}