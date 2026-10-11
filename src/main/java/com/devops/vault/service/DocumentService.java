package com.devops.vault.service;

import com.devops.vault.model.Document;
import com.devops.vault.model.StorageStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final StorageService storageService;
    private final Map<String, Document> documentRepository = new ConcurrentHashMap<>();

    public DocumentService(StorageService storageService) {
        this.storageService = storageService;
        log.info("DocumentService active with Storage Provider: [{}]", storageService.getProviderName());
    }

    public Document uploadDocument(MultipartFile file, List<String> tags) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        byte[] bytes = file.getBytes();
        String checksum = calculateSha256(bytes);
        String id = UUID.randomUUID().toString().substring(0, 8);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        storageService.store(id, originalFilename, bytes, contentType);
        String resourceUrl = storageService.getResourceUrl(id, originalFilename);

        List<String> cleanTags = tags != null ? tags.stream()
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList()) : new ArrayList<>();

        Document doc = new Document(
                id,
                originalFilename,
                contentType,
                file.getSize(),
                checksum,
                cleanTags,
                Instant.now(),
                storageService.getProviderName(),
                resourceUrl
        );

        documentRepository.put(id, doc);
        log.info("Document stored successfully: ID={}, Filename={}, SHA-256={}", id, originalFilename, checksum);
        return doc;
    }

    public List<Document> getAllDocuments(String search, String tag) {
        return documentRepository.values().stream()
                .filter(doc -> {
                    boolean matchesSearch = search == null || search.trim().isEmpty() ||
                            doc.getFilename().toLowerCase().contains(search.toLowerCase()) ||
                            doc.getId().equalsIgnoreCase(search);
                    boolean matchesTag = tag == null || tag.trim().isEmpty() ||
                            (doc.getTags() != null && doc.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(tag)));
                    return matchesSearch && matchesTag;
                })
                .sorted(Comparator.comparing(Document::getUploadedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Document> getDocumentById(String id) {
        return Optional.ofNullable(documentRepository.get(id));
    }

    public byte[] downloadDocumentContent(String id) throws Exception {
        Document doc = documentRepository.get(id);
        if (doc == null) {
            throw new NoSuchElementException("Document not found with ID: " + id);
        }
        return storageService.load(doc.getId(), doc.getFilename());
    }

    public boolean deleteDocument(String id) throws Exception {
        Document doc = documentRepository.remove(id);
        if (doc != null) {
            storageService.delete(doc.getId(), doc.getFilename());
            log.info("Deleted document: ID={}, Filename={}", id, doc.getFilename());
            return true;
        }
        return false;
    }

    public StorageStats getStats() {
        long totalDocs = documentRepository.size();
        long totalBytes = documentRepository.values().stream().mapToLong(Document::getSizeBytes).sum();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        return new StorageStats(
                totalDocs,
                totalBytes,
                formatBytes(totalBytes),
                storageService.getProviderName(),
                storageService.getStorageLocationDescription(),
                uptime
        );
    }

    public StorageService getStorageService() {
        return storageService;
    }

    private String calculateSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
