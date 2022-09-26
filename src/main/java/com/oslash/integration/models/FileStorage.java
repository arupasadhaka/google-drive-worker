package com.oslash.integration.models;

import com.oslash.integration.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

import static java.util.Objects.nonNull;

/**
 * The type File storage.
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "file_storage")
public class FileStorage {
    @Id
    private String id;
    private String fileId;
    private String userId;
    private String sourceUrl;
    private String bucket;
    private String mimeType;
    private String fileName;

    /**
     * Instantiates a new File storage.
     *
     * @param builder the builder
     */
    private FileStorage(Builder builder) {
        setId(builder.id);
        setFileId(builder.fileId);
        setUserId(builder.userId);
        setSourceUrl(builder.sourceUrl);
        setBucket(builder.bucket);
        setMimeType(builder.mimeType);
        setFileName(builder.fileName);
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
        private String id;
        private String fileId;

        private String fileName;
        private String userId;
        private String sourceUrl;
        private String bucket;
        private String mimeType;

        /**
         * Instantiates a new Builder.
         */
        public Builder() {
        }

        /**
         * Id builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder id(String val) {
            id = val;
            return this;
        }

        /**
         * File id builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder fileId(String val) {
            fileId = val;
            return this;
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
         * Source url builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder sourceUrl(String val) {
            sourceUrl = val;
            return this;
        }

        /**
         * Bucket builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder bucket(String val) {
            bucket = val;
            return this;
        }

        /**
         * Mime type builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder mimeType(String val) {
            mimeType = val;
            return this;
        }

        /**
         * File builder.
         *
         * @param item the item
         * @return the builder
         */
        public Builder file(Map item) {
            if (nonNull(item.get(Constants.FILE_ID))) {
                this.fileId = String.valueOf(item.get(Constants.FILE_ID));
            }
            if (nonNull(item.get(Constants.FILE_NAME))) {
                this.fileName = String.valueOf(item.get(Constants.FILE_NAME));
            }
            if (nonNull(item.get(Constants.MIME_TYPE))) {
                this.mimeType = String.valueOf(item.get(Constants.MIME_TYPE));
            }
            if (nonNull(item.get(Constants.USER_ID))) {
                this.userId = String.valueOf(item.get(Constants.USER_ID));
            }
            return this;
        }

        /**
         * Build file storage.
         *
         * @return the file storage
         */
        public FileStorage build() {
            return new FileStorage(this);
        }
    }
}
