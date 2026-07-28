package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.config.WorkspaceProperties;
import com.projectiq.indexerlocal.model.Repository;
import com.projectiq.indexerlocal.model.RepositoryStatus;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RepositoryService.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceProperties workspaceProperties;

    @Mock
    private IndexRepository indexRepository;

    private RepositoryService repositoryService;

    @TempDir
    Path tempDir;

    private Path sourceDir;
    private Path workspaceDir;

    @BeforeEach
    void setUp() throws IOException {
        repositoryService = new RepositoryService(
                repositoryRepository, workspaceService, workspaceProperties, indexRepository);

        // Create a source directory with some test files
        sourceDir = tempDir.resolve("source-repo");
        Files.createDirectories(sourceDir);
        Files.write(sourceDir.resolve("pom.xml"), List.of("<project></project>"));
        Files.write(sourceDir.resolve("README.md"), List.of("# Test Repository"));

        // Create a subdirectory with files
        Path srcDir = sourceDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.write(srcDir.resolve("App.java"), List.of("public class App {}"));
        Files.write(srcDir.resolve("Util.java"), List.of("public class Util {}"));

        // Create a subdirectory with a nested file
        Path resourcesDir = sourceDir.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Files.write(resourcesDir.resolve("application.properties"), List.of("app.name=test"));

        // Workspace directory
        workspaceDir = tempDir.resolve("workspace/repo_test123");
    }

    @Test
    void testRegisterRepository_Success() throws IOException {
        String sourcePath = sourceDir.toAbsolutePath().toString();

        when(repositoryRepository.findByOriginalPath(sourcePath)).thenReturn(null);
        when(workspaceService.createRepositoryWorkspace(anyString(), eq("source-repo")))
                .thenReturn(workspaceDir);

        Repository savedRepo = new Repository();
        savedRepo.setId(1L);
        savedRepo.setRepositoryId("repo_test123");
        savedRepo.setRepositoryName("source-repo");
        savedRepo.setOriginalPath(sourcePath);
        savedRepo.setWorkspacePath(workspaceDir.toString());
        savedRepo.setRegistrationTimestamp(LocalDateTime.now());
        savedRepo.setLastUpdatedTimestamp(LocalDateTime.now());
        savedRepo.setStatus(RepositoryStatus.REGISTERED);
        savedRepo.setBuildSystem("Unknown");
        savedRepo.setTechnologyStack("Unknown");

        when(repositoryRepository.save(any(Repository.class))).thenReturn(1L);

        Repository result = repositoryService.registerRepository(sourcePath);

        assertNotNull(result);
        assertEquals("source-repo", result.getRepositoryName());
        assertEquals(sourcePath, result.getOriginalPath());
        assertEquals(workspaceDir.toString(), result.getWorkspacePath());
        assertEquals(RepositoryStatus.REGISTERED, result.getStatus());

        // Verify workspace directory was created and files were copied
        assertTrue(Files.exists(workspaceDir));
        assertTrue(Files.exists(workspaceDir.resolve("pom.xml")));
        assertTrue(Files.exists(workspaceDir.resolve("README.md")));
        assertTrue(Files.exists(workspaceDir.resolve("src/main/java/com/example/App.java")));
        assertTrue(Files.exists(workspaceDir.resolve("src/main/java/com/example/Util.java")));
        assertTrue(Files.exists(workspaceDir.resolve("src/main/resources/application.properties")));

        // Verify file contents match
        assertEquals(List.of("<project></project>"), Files.readAllLines(workspaceDir.resolve("pom.xml")));
        assertEquals(List.of("# Test Repository"), Files.readAllLines(workspaceDir.resolve("README.md")));
        assertEquals(List.of("public class App {}"), Files.readAllLines(workspaceDir.resolve("src/main/java/com/example/App.java")));

        verify(repositoryRepository).save(any(Repository.class));
    }

    @Test
    void testRegisterRepository_NullPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.registerRepository(null);
        });
    }

    @Test
    void testRegisterRepository_EmptyPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.registerRepository("  ");
        });
    }

    @Test
    void testRegisterRepository_NonExistentPath() {
        String nonExistentPath = tempDir.resolve("does-not-exist").toString();
        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.registerRepository(nonExistentPath);
        });
    }

    @Test
    void testRegisterRepository_PathIsFile() throws IOException {
        Path filePath = tempDir.resolve("test-file.txt");
        Files.write(filePath, List.of("content"));

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.registerRepository(filePath.toString());
        });
    }

    @Test
    void testRegisterRepository_DuplicatePath() {
        String sourcePath = sourceDir.toAbsolutePath().toString();

        Repository existingRepo = new Repository();
        existingRepo.setRepositoryId("repo_existing");
        existingRepo.setOriginalPath(sourcePath);

        when(repositoryRepository.findByOriginalPath(sourcePath)).thenReturn(existingRepo);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.registerRepository(sourcePath);
        });

        verify(repositoryRepository, never()).save(any(Repository.class));
    }

    @Test
    void testRegisterRepository_WorkspaceContainsExactCopy() throws IOException {
        String sourcePath = sourceDir.toAbsolutePath().toString();

        when(repositoryRepository.findByOriginalPath(sourcePath)).thenReturn(null);
        when(workspaceService.createRepositoryWorkspace(anyString(), eq("source-repo")))
                .thenReturn(workspaceDir);
        when(repositoryRepository.save(any(Repository.class))).thenReturn(1L);

        repositoryService.registerRepository(sourcePath);

        // Count files in source
        long sourceFileCount = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .count();

        // Count files in workspace
        long workspaceFileCount = Files.walk(workspaceDir)
                .filter(Files::isRegularFile)
                .count();

        assertEquals(sourceFileCount, workspaceFileCount, "Workspace should contain the same number of files as source");
    }

    @Test
    void testListRepositories() {
        Repository repo1 = new Repository();
        repo1.setId(1L);
        repo1.setRepositoryId("repo_001");
        repo1.setRepositoryName("Repo One");

        Repository repo2 = new Repository();
        repo2.setId(2L);
        repo2.setRepositoryId("repo_002");
        repo2.setRepositoryName("Repo Two");

        when(repositoryRepository.findAll()).thenReturn(List.of(repo1, repo2));

        List<Repository> result = repositoryService.listRepositories();

        assertEquals(2, result.size());
        assertEquals("Repo One", result.get(0).getRepositoryName());
        assertEquals("Repo Two", result.get(1).getRepositoryName());
    }

    @Test
    void testGetRepository_Success() {
        Repository repo = new Repository();
        repo.setId(1L);
        repo.setRepositoryId("repo_001");
        repo.setRepositoryName("Test Repo");

        when(repositoryRepository.findById(1L)).thenReturn(repo);

        Repository result = repositoryService.getRepository(1L);

        assertNotNull(result);
        assertEquals("Test Repo", result.getRepositoryName());
    }

    @Test
    void testGetRepository_NotFound() {
        when(repositoryRepository.findById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.getRepository(99L);
        });
    }

    @Test
    void testGetRepositoryByRepositoryId_Success() {
        Repository repo = new Repository();
        repo.setRepositoryId("repo_001");
        repo.setRepositoryName("Test Repo");

        when(repositoryRepository.findByRepositoryId("repo_001")).thenReturn(repo);

        Repository result = repositoryService.getRepositoryByRepositoryId("repo_001");

        assertNotNull(result);
        assertEquals("Test Repo", result.getRepositoryName());
    }

    @Test
    void testGetRepositoryByRepositoryId_NotFound() {
        when(repositoryRepository.findByRepositoryId("nonexistent")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.getRepositoryByRepositoryId("nonexistent");
        });
    }

    @Test
    void testUpdateStatus() {
        Repository repo = new Repository();
        repo.setId(1L);
        repo.setStatus(RepositoryStatus.REGISTERED);

        when(repositoryRepository.findById(1L)).thenReturn(repo);

        repositoryService.updateStatus(1L, RepositoryStatus.INDEXING);

        assertEquals(RepositoryStatus.INDEXING, repo.getStatus());
        verify(repositoryRepository).update(repo);
    }

    @Test
    void testUpdateStatus_NotFound() {
        when(repositoryRepository.findById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.updateStatus(99L, RepositoryStatus.INDEXING);
        });
    }

    @Test
    void testDeleteRepository() throws IOException {
        // Create a workspace directory to be deleted
        Files.createDirectories(workspaceDir);
        Files.write(workspaceDir.resolve("test.txt"), List.of("content"));

        Repository repo = new Repository();
        repo.setId(1L);
        repo.setRepositoryId("repo_test123");
        repo.setRepositoryName("Test Repo");
        repo.setWorkspacePath(workspaceDir.toString());

        when(repositoryRepository.findById(1L)).thenReturn(repo);

        repositoryService.deleteRepository(1L);

        // Verify workspace was deleted
        assertFalse(Files.exists(workspaceDir));

        verify(repositoryRepository).deleteById(1L);
    }

    @Test
    void testDeleteRepository_NotFound() {
        when(repositoryRepository.findById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.deleteRepository(99L);
        });
    }

    @Test
    void testUpdateRepository_Success() {
        Repository repo = new Repository();
        repo.setId(1L);
        repo.setRepositoryName("Old Name");
        repo.setWorkspacePath("/old/workspace");

        when(repositoryRepository.findById(1L)).thenReturn(repo);

        repositoryService.updateRepository(1L, "New Name", null, null);

        assertEquals("New Name", repo.getRepositoryName());
        verify(repositoryRepository).update(repo);
    }

    @Test
    void testUpdateRepository_NotFound() {
        when(repositoryRepository.findById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.updateRepository(99L, "New Name", null, null);
        });
    }

    @Test
    void testGetRepositoryStatistics() {
        when(repositoryRepository.countAll()).thenReturn(10L);
        when(repositoryRepository.countByStatus(RepositoryStatus.READY)).thenReturn(5L);
        when(repositoryRepository.countByStatus(RepositoryStatus.FAILED)).thenReturn(1L);
        when(repositoryRepository.countByStatus(RepositoryStatus.INDEXING)).thenReturn(2L);
        when(repositoryRepository.countByStatus(RepositoryStatus.REGISTERED)).thenReturn(2L);

        var stats = repositoryService.getRepositoryStatistics();

        assertEquals(10L, stats.get("totalRepositories"));
        assertEquals(5L, stats.get("readyRepositories"));
        assertEquals(1L, stats.get("failedRepositories"));
        assertEquals(2L, stats.get("indexingRepositories"));
        assertEquals(2L, stats.get("registeredRepositories"));
    }

    @Test
    void testDeleteRepositoryWithAllData_Success() {
        Repository repo = new Repository();
        repo.setRepositoryId("repo_test123");
        repo.setRepositoryName("Test Repo");
        repo.setStatus(RepositoryStatus.REGISTERED);

        when(repositoryRepository.findByRepositoryId("repo_test123")).thenReturn(repo);

        repositoryService.deleteRepositoryWithAllData("repo_test123");

        verify(repositoryRepository).deleteAllDataByRepositoryId("repo_test123");
    }

    @Test
    void testDeleteRepositoryWithAllData_NotFound() {
        when(repositoryRepository.findByRepositoryId("nonexistent")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.deleteRepositoryWithAllData("nonexistent");
        });
    }

    @Test
    void testDeleteRepositoryWithAllData_IndexingInProgress() {
        Repository repo = new Repository();
        repo.setRepositoryId("repo_test123");
        repo.setStatus(RepositoryStatus.INDEXING);

        when(repositoryRepository.findByRepositoryId("repo_test123")).thenReturn(repo);

        assertThrows(IllegalStateException.class, () -> {
            repositoryService.deleteRepositoryWithAllData("repo_test123");
        });
    }

    @Test
    void testDeleteRepositoryWithAllData_RefreshingInProgress() {
        Repository repo = new Repository();
        repo.setRepositoryId("repo_test123");
        repo.setStatus(RepositoryStatus.REFRESHING);

        when(repositoryRepository.findByRepositoryId("repo_test123")).thenReturn(repo);

        assertThrows(IllegalStateException.class, () -> {
            repositoryService.deleteRepositoryWithAllData("repo_test123");
        });
    }

    @Test
    void testMarkRepositoryAsIndexed_Success() {
        Repository repo = new Repository();
        repo.setId(1L);
        repo.setRepositoryId("repo_test123");
        repo.setRepositoryName("Test Repo");
        repo.setStatus(RepositoryStatus.REGISTERED);
        repo.setLastIndexingTimestamp(null);

        when(repositoryRepository.findByRepositoryId("repo_test123")).thenReturn(repo);

        repositoryService.markRepositoryAsIndexed("repo_test123");

        assertEquals(RepositoryStatus.INDEXED, repo.getStatus());
        assertNotNull(repo.getLastIndexingTimestamp());
        assertNotNull(repo.getLastUpdatedTimestamp());
        verify(repositoryRepository).update(repo);
    }

    @Test
    void testMarkRepositoryAsIndexed_NotFound() {
        when(repositoryRepository.findByRepositoryId("nonexistent")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            repositoryService.markRepositoryAsIndexed("nonexistent");
        });
    }

    @Test
    void testMarkRepositoryAsIndexed_StatusPersistence() {
        Repository repo = new Repository();
        repo.setId(1L);
        repo.setRepositoryId("repo_test123");
        repo.setStatus(RepositoryStatus.REGISTERED);
        repo.setLastIndexingTimestamp(null);

        when(repositoryRepository.findByRepositoryId("repo_test123")).thenReturn(repo);

        repositoryService.markRepositoryAsIndexed("repo_test123");

        // Verify status is persisted as INDEXED
        assertEquals(RepositoryStatus.INDEXED, repo.getStatus());
        verify(repositoryRepository).update(repo);
    }

    @Test
    void testMarkRepositoryAsIndexed_TimestampPersistence() {
        Repository repo = new Repository();
        repo.setId(1L);
        repo.setRepositoryId("repo_test123");
        repo.setStatus(RepositoryStatus.REGISTERED);
        repo.setLastIndexingTimestamp(null);

        when(repositoryRepository.findByRepositoryId("repo_test123")).thenReturn(repo);

        repositoryService.markRepositoryAsIndexed("repo_test123");

        // Verify timestamps are set
        assertNotNull(repo.getLastIndexingTimestamp());
        assertNotNull(repo.getLastUpdatedTimestamp());
        assertTrue(repo.getLastIndexingTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(repo.getLastUpdatedTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(repositoryRepository).update(repo);
    }
}
