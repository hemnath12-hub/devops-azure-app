package com.devops.vault.model;

import java.time.Instant;
import java.util.List;

public class Document {
    private String id;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private String checksumSha256;
    private List<String> tags;
    private Instant uploadedAt;
    private String storageProvider; // "LOCAL" or "AZURE_BLOB"
    private String blobUrl;

    public Document() {
    }

    public Document(String id, String filename, String contentType, long sizeBytes,
                    String checksumSha256, List<String> tags, Instant uploadedAt,
                    String storageProvider, String blobUrl) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.tags = tags;
        this.uploadedAt = uploadedAt;
        this.storageProvider = storageProvider;
        this.blobUrl = blobUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }

    public String getBlobUrl() {
        return blobUrl;
    }

    public void setBlobUrl(String blobUrl) {
        this.blobUrl = blobUrl;
    }
}
