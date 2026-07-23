package com.projectiq.indexerlocal.model.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for indexing a single file.
 */
@Schema(description = "Request body for file indexing operations")
public class FileIndexRequest {

    @Schema(description = "Java file to index", example = "File data")
    private MultipartFile file;

    @Schema(description = "Optional file path for identification", example = "/path/to/File.java")
    private String filePath;

    public FileIndexRequest() {
    }

    public FileIndexRequest(MultipartFile file, String filePath) {
        this.file = file;
        this.filePath = filePath;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}