package com.devops.vault.service;

public interface StorageService {
    String getProviderName();
    String getStorageLocationDescription();
    void store(String documentId, String filename, byte[] content, String contentType) throws Exception;
    byte[] load(String documentId, String filename) throws Exception;
    boolean delete(String documentId, String filename) throws Exception;
    String getResourceUrl(String documentId, String filename);
}
