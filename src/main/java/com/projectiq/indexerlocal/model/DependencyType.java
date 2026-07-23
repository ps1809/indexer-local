package com.projectiq.indexerlocal.model;

/**
 * Enum representing the classification of dependencies by scope/type.
 */
public enum DependencyType {
    COMPILE,
    RUNTIME,
    TEST,
    PROVIDED,
    OPTIONAL,
    ANNOTATION_PROCESSOR,
    DEVELOPMENT_ONLY
}