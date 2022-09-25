package com.oslash.integration.worker.model;

import com.oslash.integration.models.FileStorage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FileStorageInfo {

    private FileStorage file;

    private String userId;

    private InputStream fileStream;

    private FileStorageInfo(Builder builder) {
        file = builder.file;
        userId = builder.userId;
        fileStream = builder.fileStream;
    }

    public FileStorage getFile() {
        return file;
    }

    public void setFile(FileStorage file) {
        this.file = file;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public InputStream getFileStream() {
        return fileStream;
    }

    public void setFileStream(InputStream fileStream) {
        this.fileStream = fileStream;
    }

    public static final class Builder {
        private FileStorage file;
        private String userId;
        private InputStream fileStream;

        public Builder() {
        }

        public Builder file(FileStorage val) {
            file = val;
            return this;
        }

        public Builder userId(String val) {
            userId = val;
            return this;
        }

        public Builder fileStream(InputStream val) {
            fileStream = val;
            return this;
        }

        public FileStorageInfo build() {
            return new FileStorageInfo(this);
        }
    }
}
