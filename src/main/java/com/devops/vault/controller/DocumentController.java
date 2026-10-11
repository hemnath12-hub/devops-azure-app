package com.devops.vault.controller;

import com.devops.vault.model.Document;
import com.devops.vault.model.DocumentUploadResponse;
import com.devops.vault.model.StorageStats;
import com.devops.vault.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tags", required = false) String tagsCsv) {
        try {
            List<String> tags = tagsCsv != null && !tagsCsv.trim().isEmpty() ?
                    Arrays.stream(tagsCsv.split(","))
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .collect(Collectors.toList())
                    : Collections.emptyList();

            Document doc = documentService.uploadDocument(file, tags);
            return ResponseEntity.ok(new DocumentUploadResponse(true, "Document securely stored.", doc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new DocumentUploadResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> listDocuments(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "tag", required = false) String tag) {
        return ResponseEntity.ok(documentService.getAllDocuments(search, tag));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable("id") String id) {
        return documentService.getDocumentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") String id) {
        try {
            Document doc = documentService.getDocumentById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found"));

            byte[] data = documentService.downloadDocumentContent(id);
            ByteArrayResource resource = new ByteArrayResource(data);

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            try {
                if (doc.getContentType() != null) {
                    mediaType = MediaType.parseMediaType(doc.getContentType());
                }
            } catch (Exception ignored) {
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(data.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFilename() + "\"")
                    .header("X-Document-Checksum-SHA256", doc.getChecksumSha256())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") String id) {
        try {
            boolean deleted = documentService.deleteDocument(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<StorageStats> getStats() {
        return ResponseEntity.ok(documentService.getStats());
    }
}
