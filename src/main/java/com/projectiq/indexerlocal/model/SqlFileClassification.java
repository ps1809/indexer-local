package com.projectiq.indexerlocal.model;

/**
 * Enumeration of SQL file types for classifying SQL artifacts.
 */
public enum SqlFileClassification {
    DDL,           /* Data Definition Language - CREATE, ALTER, DROP */
    DML,           /* Data Manipulation Language - INSERT, UPDATE, DELETE */
    DCL,           /* Data Control Language - GRANT, REVOKE */
    TCL,           /* Transaction Control Language - COMMIT, ROLLBACK */
    STORED_PROCEDURE,
    TRIGGER,
    VIEW,
    FUNCTION,
    MIGRATION,
    SEED_DATA,
    UTILITY,       /* Other SQL scripts */
    UNKNOWN
}