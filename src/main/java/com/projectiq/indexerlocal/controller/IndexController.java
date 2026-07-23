package com.projectiq.indexerlocal.controller;

import com.projectiq.indexerlocal.model.IndexResult;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.service.IndexerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoint to trigger indexing of a local Spring Boot project.
 */
@RestController
@RequestMapping("/api/index")
public class IndexController {

    private final IndexerService indexerService;
    private final IndexRepository indexRepository;

    public IndexController(IndexerService indexerService, IndexRepository indexRepository) {
        this.indexerService = indexerService;
        this.indexRepository = indexRepository;
    }

    /**
     * POST /api/index
     * Triggers indexing of the given project path.
     */
    @PostMapping
    public ResponseEntity<IndexResult> index(@RequestBody Map<String, String> request) {
        String projectPath = request.get("projectPath");
        
        if (projectPath == null || projectPath.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        IndexResult result = indexerService.index(projectPath);
        
        // Persist the result to SQLite
        indexRepository.saveIndexResult(result);

        return ResponseEntity.ok(result);
    }
}