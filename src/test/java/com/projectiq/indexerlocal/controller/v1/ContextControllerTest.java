package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.service.context.ContextBuilderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextControllerTest {

    @Mock
    private ContextBuilderService contextBuilderService;

    @InjectMocks
    private ContextController contextController;

    @Test
    void testBuildContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setQuery("UserService");
        request.setContextType("symbol");

        ContextResponse response = new ContextResponse();
        response.setContextType("symbol");
        response.setQuery("UserService");

        when(contextBuilderService.buildContext(any())).thenReturn(response);

        // Act
        ResponseEntity<?> result = contextController.buildContext(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testBuildSymbolContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setSymbolName("UserService");
        request.setSymbolType("CLASS");

        ContextResponse response = new ContextResponse();
        response.setContextType("symbol");
        response.setSymbolName("UserService");

        when(contextBuilderService.buildSymbolContext(any())).thenReturn(response);

        // Act
        ResponseEntity<?> result = contextController.buildSymbolContext(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testBuildFileContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setFilePath("/src/UserService.java");

        ContextResponse response = new ContextResponse();
        response.setContextType("file");

        when(contextBuilderService.buildFileContext(any())).thenReturn(response);

        // Act
        ResponseEntity<?> result = contextController.buildFileContext(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testBuildModuleContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setModuleName("core");

        ContextResponse response = new ContextResponse();
        response.setContextType("module");

        when(contextBuilderService.buildModuleContext(any())).thenReturn(response);

        // Act
        ResponseEntity<?> result = contextController.buildModuleContext(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testBuildRepositoryContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setRepositoryId("test-repo");

        ContextResponse response = new ContextResponse();
        response.setContextType("repository");

        when(contextBuilderService.buildRepositoryContext(any())).thenReturn(response);

        // Act
        ResponseEntity<?> result = contextController.buildRepositoryContext(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testBuildPromptContext() {
        // Arrange
        ContextRequest request = new ContextRequest();
        request.setQuery("UserService");
        request.setContextType("symbol");

        ContextResponse response = new ContextResponse();
        response.setContextType("symbol");
        response.setOptimized(true);

        when(contextBuilderService.buildContext(any())).thenReturn(response);

        // Act
        ResponseEntity<?> result = contextController.buildPromptContext(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testBuildContextWithNullQuery() {
        // Arrange
        ContextRequest request = new ContextRequest();

        ContextResponse response = new ContextResponse();
        response.setContextType("symbol");

        when(contextBuilderService.buildContext(any())).thenReturn(response);

        // Act
        ResponseEntity<?> result = contextController.buildContext(request);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
    }
}