package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.FileHashRecord;
import com.projectiq.indexerlocal.model.HashProcessingStatistics;
import com.projectiq.indexerlocal.repository.FileHashRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileHashService (Intelligent Hash Engine).
 */
@ExtendWith(MockitoExtension.class)
class FileHashServiceTest {

    @Mock
    private FileHashRepository fileHashRepository;

    @Captor
    private ArgumentCaptor<List<FileHashRecord>> batchSaveCaptor;

    private FileHashService fileHashService;

    @TempDir
    Path tempDir;

    private static final String TEST_REPOSITORY_ID = "test-repo-1";

    @BeforeEach
    void setUp() {
        fileHashService = new FileHashService(fileHashRepository);
    }

    // ==================== SHA-256 Hash Computation ====================

    @Test
    @DisplayName("Should compute SHA-256 hash for an existing file")
    void testComputeHash_ExistingFile() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("TestFile.java");
        Files.writeString(testFile, "public class TestFile { }");

        // Act
        String hash = fileHashService.computeHash(testFile.toAbsolutePath().toString());

        // Assert
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64); // SHA-256 produces 64 hex chars
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("Should return null for non-existent file")
    void testComputeHash_NonExistentFile() {
        // Act
        String hash = fileHashService.computeHash("/nonexistent/File.java");

        // Assert
        assertThat(hash).isNull();
    }

    @Test
    @DisplayName("Should return consistent hash for same file content")
    void testComputeHash_ConsistentHash() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("TestFile.java");
        Files.writeString(testFile, "public class TestFile { }");

        // Act
        String hash1 = fileHashService.computeHash(testFile.toAbsolutePath().toString());
        String hash2 = fileHashService.computeHash(testFile.toAbsolutePath().toString());

        // Assert
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Should produce different hash for different content")
    void testComputeHash_DifferentContent() throws IOException {
        // Arrange
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");
        Files.writeString(file1, "public class File1 { }");
        Files.writeString(file2, "public class File2 { }");

        // Act
        String hash1 = fileHashService.computeHash(file1.toAbsolutePath().toString());
        String hash2 = fileHashService.computeHash(file2.toAbsolutePath().toString());

        // Assert
        assertThat(hash1).isNotEqualTo(hash2);
    }

    // ==================== Hash With Metadata ====================

    @Test
    @DisplayName("Should compute hash with metadata for existing file")
    void testComputeHashWithMetadata_ExistingFile() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("TestFile.java");
        Files.writeString(testFile, "public class TestFile { }");

        // Act
        Optional<FileHashRecord> result = fileHashService.computeHashWithMetadata(
                TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());

        // Assert
        assertThat(result).isPresent();
        FileHashRecord record = result.get();
        assertThat(record.getRepositoryId()).isEqualTo(TEST_REPOSITORY_ID);
        assertThat(record.getFilePath()).isEqualTo(testFile.toAbsolutePath().toString());
        assertThat(record.getSha256Hash()).hasSize(64);
        assertThat(record.getFileSize()).isGreaterThan(0);
        assertThat(record.getLastModified()).isNotNull();
        assertThat(record.getLastIndexedAt()).isNotNull();
        assertThat(record.getProcessingStatus()).isEqualTo("INDEXED");
    }

    @Test
    @DisplayName("Should return empty for non-existent file in hash with metadata")
    void testComputeHashWithMetadata_NonExistentFile() {
        // Act
        Optional<FileHashRecord> result = fileHashService.computeHashWithMetadata(
                TEST_REPOSITORY_ID, "/nonexistent/File.java");

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== Change Detection ====================

    @Test
    @DisplayName("Should detect all new files when no previous hashes exist")
    void testDetectChanges_InitialIndexing() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("NewFile.java");
        Files.writeString(testFile, "public class NewFile { }");

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(Collections.emptyList());

        Set<String> currentFiles = Set.of(testFile.toAbsolutePath().toString());

        // Act
        Map<String, String> changes = fileHashService.detectChanges(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(changes).hasSize(1);
        assertThat(changes.get(testFile.toAbsolutePath().toString())).isEqualTo("NEW");
    }

    @Test
    @DisplayName("Should detect unchanged files when hash matches")
    void testDetectChanges_UnchangedFiles() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("Unchanged.java");
        Files.writeString(testFile, "public class Unchanged { }");

        String hash = fileHashService.computeHash(testFile.toAbsolutePath().toString());
        File file = testFile.toFile();

        FileHashRecord previousRecord = new FileHashRecord(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());
        previousRecord.setSha256Hash(hash);
        previousRecord.setFileSize(file.length());
        previousRecord.setLastModified(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(file.lastModified()),
                java.time.ZoneId.systemDefault()));

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(previousRecord));

        Set<String> currentFiles = Set.of(testFile.toAbsolutePath().toString());

        // Act
        Map<String, String> changes = fileHashService.detectChanges(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(changes).hasSize(1);
        assertThat(changes.get(testFile.toAbsolutePath().toString())).isEqualTo("UNCHANGED");
    }

    @Test
    @DisplayName("Should detect modified files when content changes")
    void testDetectChanges_ModifiedFile() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("Modified.java");
        Files.writeString(testFile, "public class Modified { }");

        FileHashRecord previousRecord = new FileHashRecord(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());
        previousRecord.setSha256Hash("0000000000000000000000000000000000000000000000000000000000000000");
        previousRecord.setFileSize(0);
        previousRecord.setLastModified(LocalDateTime.now().minusDays(1));

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(previousRecord));

        Set<String> currentFiles = Set.of(testFile.toAbsolutePath().toString());

        // Act
        Map<String, String> changes = fileHashService.detectChanges(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(changes).hasSize(1);
        assertThat(changes.get(testFile.toAbsolutePath().toString())).isEqualTo("PENDING_HASH_CHECK");
    }

    @Test
    @DisplayName("Should detect deleted files")
    void testDetectChanges_DeletedFile() throws IOException {
        // Arrange
        Path deletedFilePath = tempDir.resolve("Deleted.java");

        FileHashRecord previousRecord = new FileHashRecord(TEST_REPOSITORY_ID, deletedFilePath.toAbsolutePath().toString());
        previousRecord.setSha256Hash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        previousRecord.setFileSize(100);
        previousRecord.setLastModified(LocalDateTime.now().minusDays(1));

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(previousRecord));

        Set<String> currentFiles = Collections.emptySet();

        // Act
        Map<String, String> changes = fileHashService.detectChanges(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(changes).hasSize(1);
        assertThat(changes.get(deletedFilePath.toAbsolutePath().toString())).isEqualTo("DELETED");
    }

    // ==================== Single File Validation ====================

    @Test
    @DisplayName("Should return UNCHANGED for validated file with matching hash")
    void testValidateSingleFile_Unchanged() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("ValidateTest.java");
        Files.writeString(testFile, "public class ValidateTest { }");

        String hash = fileHashService.computeHash(testFile.toAbsolutePath().toString());
        FileHashRecord storedRecord = new FileHashRecord(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());
        storedRecord.setSha256Hash(hash);

        when(fileHashRepository.findByRepositoryIdAndFilePath(
                eq(TEST_REPOSITORY_ID), eq(testFile.toAbsolutePath().toString())))
                .thenReturn(Optional.of(storedRecord));

        // Act
        String result = fileHashService.validateSingleFile(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());

        // Assert
        assertThat(result).isEqualTo("UNCHANGED");
    }

    @Test
    @DisplayName("Should return MODIFIED for validated file with different hash")
    void testValidateSingleFile_Modified() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("ModifiedValidate.java");
        Files.writeString(testFile, "public class ModifiedValidate { }");

        FileHashRecord storedRecord = new FileHashRecord(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());
        storedRecord.setSha256Hash("0000000000000000000000000000000000000000000000000000000000000000");

        when(fileHashRepository.findByRepositoryIdAndFilePath(
                eq(TEST_REPOSITORY_ID), eq(testFile.toAbsolutePath().toString())))
                .thenReturn(Optional.of(storedRecord));

        // Act
        String result = fileHashService.validateSingleFile(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());

        // Assert
        assertThat(result).isEqualTo("MODIFIED");
    }

    @Test
    @DisplayName("Should return NEW for validated file with no previous hash")
    void testValidateSingleFile_New() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("NewValidate.java");
        Files.writeString(testFile, "public class NewValidate { }");

        when(fileHashRepository.findByRepositoryIdAndFilePath(
                eq(TEST_REPOSITORY_ID), eq(testFile.toAbsolutePath().toString())))
                .thenReturn(Optional.empty());

        // Act
        String result = fileHashService.validateSingleFile(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());

        // Assert
        assertThat(result).isEqualTo("NEW");
    }

    @Test
    @DisplayName("Should return DELETED for validated file that does not exist")
    void testValidateSingleFile_Deleted() {
        // Act
        String result = fileHashService.validateSingleFile(
                TEST_REPOSITORY_ID, "/nonexistent/File.java");

        // Assert
        assertThat(result).isEqualTo("DELETED");
    }

    // ==================== Full Repository Processing ====================

    @Test
    @DisplayName("Should process initial repository indexing")
    void testProcessRepository_InitialIndexing() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("InitialIndex.java");
        Files.writeString(testFile, "public class InitialIndex { }");

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(Collections.emptyList());
        doNothing().when(fileHashRepository).saveAll(batchSaveCaptor.capture());

        Set<String> currentFiles = Set.of(testFile.toAbsolutePath().toString());

        // Act
        HashProcessingStatistics stats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getFilesProcessed()).isEqualTo(1);
        assertThat(stats.getFilesChanged()).isEqualTo(1);
        assertThat(stats.getFilesSkipped()).isEqualTo(0);
        assertThat(stats.getHashCalculations()).isEqualTo(1);

        // Verify persistence
        List<FileHashRecord> savedRecords = batchSaveCaptor.getValue();
        assertThat(savedRecords).hasSize(1);
        assertThat(savedRecords.get(0).getFilePath()).isEqualTo(testFile.toAbsolutePath().toString());
        assertThat(savedRecords.get(0).getSha256Hash()).hasSize(64);
    }

    @Test
    @DisplayName("Should process repeat indexing without changes")
    void testProcessRepository_RepeatIndexingNoChanges() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("RepeatNoChange.java");
        Files.writeString(testFile, "public class RepeatNoChange { }");

        File file = testFile.toFile();
        String hash = fileHashService.computeHash(testFile.toAbsolutePath().toString());

        FileHashRecord previousRecord = new FileHashRecord(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());
        previousRecord.setSha256Hash(hash);
        previousRecord.setFileSize(file.length());
        previousRecord.setLastModified(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(file.lastModified()),
                java.time.ZoneId.systemDefault()));

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(previousRecord));
        doNothing().when(fileHashRepository).saveAll(any());

        Set<String> currentFiles = Set.of(testFile.toAbsolutePath().toString());

        // Act
        HashProcessingStatistics stats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getFilesProcessed()).isEqualTo(0);
        assertThat(stats.getFilesChanged()).isEqualTo(0);
        assertThat(stats.getFilesSkipped()).isEqualTo(1);
        assertThat(stats.getHashCalculations()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should detect single file modification")
    void testProcessRepository_SingleFileModification() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("SingleMod.java");
        Files.writeString(testFile, "public class SingleMod { }");

        FileHashRecord previousRecord = new FileHashRecord(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());
        previousRecord.setSha256Hash("0000000000000000000000000000000000000000000000000000000000000000");
        previousRecord.setFileSize(0);
        previousRecord.setLastModified(LocalDateTime.now().minusDays(1));

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(previousRecord));
        doNothing().when(fileHashRepository).saveAll(batchSaveCaptor.capture());

        Set<String> currentFiles = Set.of(testFile.toAbsolutePath().toString());

        // Act
        HashProcessingStatistics stats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getFilesProcessed()).isEqualTo(1);
        assertThat(stats.getFilesChanged()).isEqualTo(1);
        assertThat(stats.getFilesSkipped()).isEqualTo(0);
        assertThat(stats.getHashCalculations()).isEqualTo(1);

        List<FileHashRecord> savedRecords = batchSaveCaptor.getValue();
        assertThat(savedRecords).hasSize(1);
        assertThat(savedRecords.get(0).getSha256Hash()).hasSize(64);
        assertThat(savedRecords.get(0).getSha256Hash()).isNotEqualTo("0000000000000000000000000000000000000000000000000000000000000000");
    }

    @Test
    @DisplayName("Should handle multiple file modifications")
    void testProcessRepository_MultipleFileModifications() throws IOException {
        // Arrange
        Path file1 = tempDir.resolve("MultiMod1.java");
        Path file2 = tempDir.resolve("MultiMod2.java");
        Files.writeString(file1, "public class MultiMod1 { }");
        Files.writeString(file2, "public class MultiMod2 { }");

        FileHashRecord prev1 = new FileHashRecord(TEST_REPOSITORY_ID, file1.toAbsolutePath().toString());
        prev1.setSha256Hash("0000000000000000000000000000000000000000000000000000000000000000");
        prev1.setFileSize(0);
        prev1.setLastModified(LocalDateTime.now().minusDays(1));

        FileHashRecord prev2 = new FileHashRecord(TEST_REPOSITORY_ID, file2.toAbsolutePath().toString());
        prev2.setSha256Hash("1111111111111111111111111111111111111111111111111111111111111111");
        prev2.setFileSize(0);
        prev2.setLastModified(LocalDateTime.now().minusDays(1));

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(prev1, prev2));
        doNothing().when(fileHashRepository).saveAll(batchSaveCaptor.capture());

        Set<String> currentFiles = Set.of(
                file1.toAbsolutePath().toString(),
                file2.toAbsolutePath().toString());

        // Act
        HashProcessingStatistics stats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getFilesProcessed()).isEqualTo(2);
        assertThat(stats.getFilesChanged()).isEqualTo(2);
        assertThat(stats.getHashCalculations()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle deleted file")
    void testProcessRepository_DeletedFile() throws IOException {
        // Arrange
        Path deletedFilePath = tempDir.resolve("DeletedFile.java");
        Path remainingFile = tempDir.resolve("RemainingFile.java");
        Files.writeString(remainingFile, "public class RemainingFile { }");

        FileHashRecord deletedRecord = new FileHashRecord(TEST_REPOSITORY_ID, deletedFilePath.toAbsolutePath().toString());
        deletedRecord.setSha256Hash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        deletedRecord.setFileSize(100);
        deletedRecord.setLastModified(LocalDateTime.now().minusDays(1));

        doNothing().when(fileHashRepository).deleteByRepositoryIdAndFilePath(
                eq(TEST_REPOSITORY_ID), eq(deletedFilePath.toAbsolutePath().toString()));
        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(deletedRecord));
        doNothing().when(fileHashRepository).saveAll(any());

        Set<String> currentFiles = Set.of(remainingFile.toAbsolutePath().toString());

        // Act
        HashProcessingStatistics stats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getFilesChanged()).isEqualTo(2); // deleted + new remaining file

        // Verify deletion was called
        verify(fileHashRepository).deleteByRepositoryIdAndFilePath(
                eq(TEST_REPOSITORY_ID), eq(deletedFilePath.toAbsolutePath().toString()));
    }

    @Test
    @DisplayName("Should handle newly created file")
    void testProcessRepository_NewlyCreatedFile() throws IOException {
        // Arrange
        Path newFile = tempDir.resolve("NewlyCreated.java");
        Files.writeString(newFile, "public class NewlyCreated { }");

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(Collections.emptyList());
        doNothing().when(fileHashRepository).saveAll(batchSaveCaptor.capture());

        Set<String> currentFiles = Set.of(newFile.toAbsolutePath().toString());

        // Act
        HashProcessingStatistics stats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getFilesProcessed()).isEqualTo(1);
        assertThat(stats.getFilesChanged()).isEqualTo(1);
        assertThat(stats.getFilesSkipped()).isEqualTo(0);
        assertThat(stats.getHashCalculations()).isEqualTo(1);

        List<FileHashRecord> savedRecords = batchSaveCaptor.getValue();
        assertThat(savedRecords).hasSize(1);
        assertThat(savedRecords.get(0).getProcessingStatus()).isEqualTo("INDEXED");
    }

    // ==================== Module Validation ====================

    @Test
    @DisplayName("Should validate module directory")
    void testValidateModule() throws IOException {
        // Arrange
        Path moduleDir = tempDir.resolve("module");
        Files.createDirectories(moduleDir);
        Path moduleFile = moduleDir.resolve("ModuleClass.java");
        Files.writeString(moduleFile, "public class ModuleClass { }");

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(Collections.emptyList());
        doNothing().when(fileHashRepository).saveAll(any());

        // Act
        HashProcessingStatistics stats = fileHashService.validateModule(
                TEST_REPOSITORY_ID, moduleDir.toAbsolutePath().toString());

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getFilesProcessed()).isEqualTo(1);
    }

    // ==================== Hash Persistence ====================

    @Test
    @DisplayName("Should persist hash records correctly")
    void testHashPersistence() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("PersistTest.java");
        Files.writeString(testFile, "public class PersistTest { }");

        // First pass - initial indexing
        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());
        doNothing().when(fileHashRepository).saveAll(batchSaveCaptor.capture());

        Set<String> currentFiles = Set.of(testFile.toAbsolutePath().toString());

        // Act - First run (initial indexing)
        HashProcessingStatistics firstStats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Simulate what would be persisted
        List<FileHashRecord> firstSaved = batchSaveCaptor.getValue();
        assertThat(firstSaved).hasSize(1);
        String persistedHash = firstSaved.get(0).getSha256Hash();

        // Prepare mock for second run
        FileHashRecord persistedRecord = new FileHashRecord(TEST_REPOSITORY_ID, testFile.toAbsolutePath().toString());
        persistedRecord.setSha256Hash(persistedHash);
        persistedRecord.setFileSize(firstSaved.get(0).getFileSize());
        persistedRecord.setLastModified(firstSaved.get(0).getLastModified());

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(persistedRecord));
        doNothing().when(fileHashRepository).saveAll(any());

        // Act - Second run (no changes)
        HashProcessingStatistics secondStats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(firstStats.getStatus()).isEqualTo("SUCCESS");
        assertThat(firstStats.getFilesProcessed()).isEqualTo(1);
        assertThat(firstStats.getHashCalculations()).isEqualTo(1);

        assertThat(secondStats.getStatus()).isEqualTo("SUCCESS");
        assertThat(secondStats.getFilesProcessed()).isEqualTo(0);
        assertThat(secondStats.getFilesSkipped()).isEqualTo(1);
        assertThat(secondStats.getHashCalculations()).isEqualTo(0);
    }

    // ==================== Statistics ====================

    @Test
    @DisplayName("Should generate correct statistics")
    void testStatisticsGeneration() throws IOException {
        // Arrange
        Path unchangedFile = tempDir.resolve("UnchangedStats.java");
        Path modifiedFile = tempDir.resolve("ModifiedStats.java");
        Files.writeString(unchangedFile, "public class UnchangedStats { }");
        Files.writeString(modifiedFile, "public class ModifiedStats { }");

        String unchangedHash = fileHashService.computeHash(unchangedFile.toAbsolutePath().toString());
        File unchangedFileOnDisk = unchangedFile.toFile();

        FileHashRecord unchangedRecord = new FileHashRecord(TEST_REPOSITORY_ID, unchangedFile.toAbsolutePath().toString());
        unchangedRecord.setSha256Hash(unchangedHash);
        unchangedRecord.setFileSize(unchangedFileOnDisk.length());
        unchangedRecord.setLastModified(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(unchangedFileOnDisk.lastModified()),
                java.time.ZoneId.systemDefault()));

        FileHashRecord modifiedRecord = new FileHashRecord(TEST_REPOSITORY_ID, modifiedFile.toAbsolutePath().toString());
        modifiedRecord.setSha256Hash("0000000000000000000000000000000000000000000000000000000000000000");
        modifiedRecord.setFileSize(0);
        modifiedRecord.setLastModified(LocalDateTime.now().minusDays(1));

        when(fileHashRepository.findByRepositoryId(TEST_REPOSITORY_ID))
                .thenReturn(List.of(unchangedRecord, modifiedRecord));
        doNothing().when(fileHashRepository).saveAll(any());

        Set<String> currentFiles = Set.of(
                unchangedFile.toAbsolutePath().toString(),
                modifiedFile.toAbsolutePath().toString());

        // Act
        HashProcessingStatistics stats = fileHashService.processRepository(TEST_REPOSITORY_ID, currentFiles);

        // Assert
        assertThat(stats.getStatus()).isEqualTo("SUCCESS");
        assertThat(stats.getRepositoryId()).isEqualTo(TEST_REPOSITORY_ID);
        assertThat(stats.getFilesSkipped()).isEqualTo(1);
        assertThat(stats.getFilesProcessed()).isEqualTo(1);
        assertThat(stats.getFilesChanged()).isEqualTo(1);
        assertThat(stats.getHashCalculations()).isEqualTo(1);
        assertThat(stats.getTimeSavedMs()).isGreaterThan(0);
        assertThat(stats.getStartTime()).isNotNull();
        assertThat(stats.getEndTime()).isNotNull();
        assertThat(stats.getProcessingDuration()).isNotNull();
        assertThat(stats.getTotalFilesScanned()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should get stored hash for existing file")
    void testGetStoredHash() {
        // Arrange
        String filePath = "/test/File.java";
        FileHashRecord stored = new FileHashRecord(TEST_REPOSITORY_ID, filePath);
        stored.setSha256Hash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");

        when(fileHashRepository.findByRepositoryIdAndFilePath(TEST_REPOSITORY_ID, filePath))
                .thenReturn(Optional.of(stored));

        // Act
        Optional<FileHashRecord> result = fileHashService.getStoredHash(TEST_REPOSITORY_ID, filePath);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getSha256Hash()).isEqualTo("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
    }

    @Test
    @DisplayName("Should delete hash record")
    void testDeleteHash() {
        // Act
        fileHashService.deleteHash(TEST_REPOSITORY_ID, "/test/File.java");

        // Assert
        verify(fileHashRepository).deleteByRepositoryIdAndFilePath(TEST_REPOSITORY_ID, "/test/File.java");
    }

    @Test
    @DisplayName("Should handle corrupted file gracefully")
    void testComputeHash_CorruptedFile() throws IOException {
        // Arrange - Create a binary/non-readable file
        Path corruptFile = tempDir.resolve("corrupt.bin");
        byte[] randomBytes = new byte[100];
        new Random().nextBytes(randomBytes);
        Files.write(corruptFile, randomBytes);

        // Act - SHA-256 should still work on binary data
        String hash = fileHashService.computeHash(corruptFile.toAbsolutePath().toString());

        // Assert
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
    }
}