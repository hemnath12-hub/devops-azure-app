package com.devops.vault.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "azure.storage.enabled", havingValue = "true")
public class AzureBlobStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @Value("${azure.storage.container-name:documents}")
    private String containerName;

    private BlobContainerClient containerClient;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Azure Blob Storage Client for container: {}", containerName);
            if (connectionString == null || connectionString.trim().isEmpty()) {
                log.warn("Azure Storage connection string is empty! Please configure azure.storage.connection-string.");
                return;
            }
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!this.containerClient.exists()) {
                this.containerClient.create();
                log.info("Created Azure Blob Container: {}", containerName);
            } else {
                log.info("Connected to existing Azure Blob Container: {}", containerName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Azure Blob Storage client. Check credentials and network. Error: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "AZURE_BLOB_STORAGE";
    }

    @Override
    public String getStorageLocationDescription() {
        return "Azure Container: " + containerName;
    }

    @Override
    public void store(String documentId, String filename, byte[] content, String contentType) throws Exception {
        if (containerClient == null) {
            throw new IllegalStateException("Azure Blob Container client is not initialized.");
        }
        String blobName = documentId + "/" + filename;
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        blobClient.upload(BinaryData.fromBytes(content), true);

        if (contentType != null && !contentType.isEmpty()) {
            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
            blobClient.setHttpHeaders(headers);
        }
        log.info("Successfully uploaded blob to Azure Storage: {}", blobName);
    }

    @Override
    public byte[] load(String documentId, String filename) throws Exception {
        if (containerClient == null) {
            throw new IllegalStateException("Azure Blob Container client is not initialized.");
        }
        String blobName = documentId + "/" + filename;
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new IllegalArgumentException("Blob does not exist in Azure Storage: " + blobName);
        }
        return blobClient.downloadContent().toBytes();
    }

    @Override
    public boolean delete(String documentId, String filename) throws Exception {
        if (containerClient == null) {
            throw new IllegalStateException("Azure Blob Container client is not initialized.");
        }
        String blobName = documentId + "/" + filename;
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        return blobClient.deleteIfExists();
    }

    @Override
    public String getResourceUrl(String documentId, String filename) {
        if (containerClient == null) {
            return "/api/v1/documents/" + documentId + "/download";
        }
        String blobName = documentId + "/" + filename;
        return containerClient.getBlobClient(blobName).getBlobUrl();
    }
}
