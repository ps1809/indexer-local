package com.projectiq.indexerlocal.model.relationshipsearch;

/**
 * Defines the types of relationships that can be traversed by the Relationship Search Engine.
 */
public enum RelationshipType {
    INHERITANCE,
    IMPLEMENTATION,
    REFERENCE,
    DEPENDENCY,
    CALL,
    PACKAGE,
    MODULE
}