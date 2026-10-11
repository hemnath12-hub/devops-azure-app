package com.devops.vault;

import com.devops.vault.model.Document;
import com.devops.vault.model.StorageStats;
import com.devops.vault.service.DocumentService;
import com.devops.vault.service.LocalStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentServiceTest {

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        LocalStorageService storageService = new LocalStorageService("build/test-storage");
        documentService = new DocumentService(storageService);
    }

    @Test
    void testUploadAndRetrieveDocument() throws Exception {
        byte[] content = "Top Secret Financial Report 2026".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.txt",
                "text/plain",
                content
        );

        Document doc = documentService.uploadDocument(file, List.of("finance", "confidential"));

        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertEquals("report.txt", doc.getFilename());
        assertEquals(content.length, doc.getSizeBytes());
        assertNotNull(doc.getChecksumSha256());
        assertTrue(doc.getTags().contains("finance"));

        // Retrieve by ID
        assertTrue(documentService.getDocumentById(doc.getId()).isPresent());

        // Download Content
        byte[] downloaded = documentService.downloadDocumentContent(doc.getId());
        assertArrayEquals(content, downloaded);
    }

    @Test
    void testSearchAndFilter() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file", "budget_2026.pdf", "application/pdf", "data1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "server_config.yaml", "text/yaml", "data2".getBytes());

        documentService.uploadDocument(file1, List.of("budget", "annual"));
        documentService.uploadDocument(file2, List.of("devops", "infra"));

        List<Document> budgetDocs = documentService.getAllDocuments("budget", null);
        assertEquals(1, budgetDocs.size());
        assertEquals("budget_2026.pdf", budgetDocs.get(0).getFilename());

        List<Document> devopsDocs = documentService.getAllDocuments(null, "infra");
        assertEquals(1, devopsDocs.size());
        assertEquals("server_config.yaml", devopsDocs.get(0).getFilename());
    }

    @Test
    void testDeleteDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "temp.log", "text/plain", "logs".getBytes());
        Document doc = documentService.uploadDocument(file, List.of("temp"));

        assertTrue(documentService.getDocumentById(doc.getId()).isPresent());
        boolean deleted = documentService.deleteDocument(doc.getId());
        assertTrue(deleted);
        assertFalse(documentService.getDocumentById(doc.getId()).isPresent());
    }

    @Test
    void testStorageStats() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "1234567890".getBytes());
        documentService.uploadDocument(file, List.of("test"));

        StorageStats stats = documentService.getStats();
        assertEquals(1, stats.getTotalDocuments());
        assertEquals(10, stats.getTotalSizeBytes());
        assertNotNull(stats.getActiveStorageProvider());
    }
}
