package com.projectiq.indexerlocal.model.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for indexing a single file or batch of files.
 */
@Schema(description = "Request body for file indexing operations")
public class FileIndexRequest {

    @Schema(
        description = "Java file(s) to index", 
        example = "File data",
        required = true
    )
    private MultipartFile file;

    @Schema(
        description = "Optional file path for identification", 
        example = "/path/to/File.java"
    )
    private String filePath;

    @Schema(
        description = "Maximum number of files to process in a single request",
        example = "100",
        minimum = "1",
        maximum = "1000"
    )
    private Integer maxItems;

    /**
     * Default maximum file size (10MB).
     */
    public static final int DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

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

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }
}