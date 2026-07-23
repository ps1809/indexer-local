package com.projectiq.indexerlocal.extractor;

import com.github.javaparser.ast.CompilationUnit;
import com.projectiq.indexerlocal.model.FileIndex;

/**
 * Interface for extractors that analyze Java source files.
 */
public interface Extractor {
    
    /**
     * Returns the name of this extractor.
     */
    String getName();
    
    /**
     * Extracts metadata from the given CompilationUnit and populates the FileIndex.
     */
    void extract(CompilationUnit cu, FileIndex fileIndex);
}