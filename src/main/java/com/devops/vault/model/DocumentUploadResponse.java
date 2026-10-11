package com.devops.vault.model;

public class DocumentUploadResponse {
    private boolean success;
    private String message;
    private Document document;

    public DocumentUploadResponse() {
    }

    public DocumentUploadResponse(boolean success, String message, Document document) {
        this.success = success;
        this.message = message;
        this.document = document;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
