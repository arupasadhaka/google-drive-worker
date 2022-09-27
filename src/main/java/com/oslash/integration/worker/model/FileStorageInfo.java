package com.oslash.integration.worker.model;

import com.google.api.services.drive.model.File;
import com.oslash.integration.models.FileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.InputStream;

/**
 * The type File storage info.
 */
public class FileStorageInfo {

    private FileStorage fileStorage;

    private File file;
    private String userId;

    private InputStream fileStream;

    /**
     * Instantiates a new File storage info.
     *
     * @param builder the builder
     */
    private FileStorageInfo(Builder builder) {
        setFileStorage(builder.fileStorage);
        setFile(builder.file);
        setUserId(builder.userId);
        setFileStream(builder.fileStream);
    }

    /**
     * Gets file.
     *
     * @return the file
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public FileStorage getFileStorage() {
        return fileStorage;
    }

    /**
     * Sets file.
     *
     * @param fileStorage the file
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setFileStorage(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * Gets user id.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets user id.
     *
     * @param userId the user id
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets file stream.
     *
     * @return the file stream
     */
    public InputStream getFileStream() {
        return fileStream;
    }

    /**
     * Sets file stream.
     *
     * @param fileStream the file stream
     */
    public void setFileStream(InputStream fileStream) {
        this.fileStream = fileStream;
    }

    /**
     * Gets file.
     *
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets file.
     *
     * @param file the file
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
        private FileStorage fileStorage;
        private File file;
        private String userId;
        private InputStream fileStream;

        /**
         * Instantiates a new Builder.
         */
        public Builder() {
        }




        /**
         * User id builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder userId(String val) {
            userId = val;
            return this;
        }

        /**
         * File stream builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder fileStream(InputStream val) {
            fileStream = val;
            return this;
        }

        /**
         * Build file storage info.
         *
         * @return the file storage info
         */
        public FileStorageInfo build() {
            return new FileStorageInfo(this);
        }

        /**
         * File storage builder.
         *
         * @param val the val
         * @return the builder
         */
        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder fileStorage(FileStorage val) {
            fileStorage = val;
            return this;
        }

        /**
         * File builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder file(File val) {
            file = val;
            return this;
        }
    }
}
