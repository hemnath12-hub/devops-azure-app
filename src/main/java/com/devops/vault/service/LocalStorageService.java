package com.devops.vault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "azure.storage.enabled", havingValue = "false", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path storageDirectory;
    private final ConcurrentHashMap<String, byte[]> inMemoryFallback = new ConcurrentHashMap<>();

    public LocalStorageService(@Value("${app.storage.local-dir:./vault-storage}") String localDir) {
        this.storageDirectory = Paths.get(localDir).toAbsolutePath();
        try {
            Files.createDirectories(this.storageDirectory);
            log.info("Initialized LocalStorageService at path: {}", this.storageDirectory);
        } catch (IOException e) {
            log.warn("Could not create local storage directory {}. Falling back to memory storage.", this.storageDirectory, e);
        }
    }

    @Override
    public String getProviderName() {
        return "LOCAL_DISK_FALLBACK";
    }

    @Override
    public String getStorageLocationDescription() {
        return storageDirectory.toString();
    }

    @Override
    public void store(String documentId, String filename, byte[] content, String contentType) throws Exception {
        String targetName = documentId + "_" + filename;
        try {
            Path targetPath = storageDirectory.resolve(targetName);
            Files.write(targetPath, content);
            log.debug("Saved file locally at: {}", targetPath);
        } catch (Exception e) {
            log.warn("Failed to write to disk. Storing in in-memory fallback. Reason: {}", e.getMessage());
            inMemoryFallback.put(targetName, content);
        }
    }

    @Override
    public byte[] load(String documentId, String filename) throws Exception {
        String targetName = documentId + "_" + filename;
        Path targetPath = storageDirectory.resolve(targetName);
        if (Files.exists(targetPath)) {
            return Files.readAllBytes(targetPath);
        }
        if (inMemoryFallback.containsKey(targetName)) {
            return inMemoryFallback.get(targetName);
        }
        throw new IllegalArgumentException("Document not found in storage: " + targetName);
    }

    @Override
    public boolean delete(String documentId, String filename) throws Exception {
        String targetName = documentId + "_" + filename;
        Path targetPath = storageDirectory.resolve(targetName);
        boolean deleted = false;
        if (Files.exists(targetPath)) {
            deleted = Files.deleteIfExists(targetPath);
        }
        if (inMemoryFallback.remove(targetName) != null) {
            deleted = true;
        }
        return deleted;
    }

    @Override
    public String getResourceUrl(String documentId, String filename) {
        return "/api/v1/documents/" + documentId + "/download";
    }
}
