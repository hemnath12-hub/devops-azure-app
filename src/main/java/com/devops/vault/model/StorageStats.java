package com.devops.vault.model;

public class StorageStats {
    private long totalDocuments;
    private long totalSizeBytes;
    private String formattedSize;
    private String activeStorageProvider;
    private String storageLocation;
    private long uptimeSeconds;

    public StorageStats() {
    }

    public StorageStats(long totalDocuments, long totalSizeBytes, String formattedSize,
                        String activeStorageProvider, String storageLocation, long uptimeSeconds) {
        this.totalDocuments = totalDocuments;
        this.totalSizeBytes = totalSizeBytes;
        this.formattedSize = formattedSize;
        this.activeStorageProvider = activeStorageProvider;
        this.storageLocation = storageLocation;
        this.uptimeSeconds = uptimeSeconds;
    }

    public long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long size) {
        this.totalSizeBytes = size;
    }

    public String getFormattedSize() {
        return formattedSize;
    }

    public void setFormattedSize(String formattedSize) {
        this.formattedSize = formattedSize;
    }

    public String getActiveStorageProvider() {
        return activeStorageProvider;
    }

    public void setActiveStorageProvider(String provider) {
        this.activeStorageProvider = provider;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }
}
