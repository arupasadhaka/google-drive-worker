package com.oslash.integration.models;

import com.google.api.services.drive.model.File;
import com.oslash.integration.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

import static java.util.Objects.nonNull;


@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "file_meta")
public class FileMeta {
    @Id
    private String id;
    private String mimeType;

    private String userId;
    private Object content;

    public FileMeta(String id, String mimeType, Object content) {
        this.id = id;
        this.mimeType = mimeType;
        this.content = content;
    }

    public FileMeta(Builder builder) {
        setId(builder.id);
        setUserId(builder.userId);
        setMimeType(builder.mimeType);
        setContent(builder.content);
    }

    public static final class Builder {
        private String id;
        private String mimeType;
        private Object content;

        private String userId;

        private File file;

        public Builder() {
        }

        public Builder id(String val) {
            id = val;
            return this;
        }

        public Builder mimeType(String val) {
            mimeType = val;
            return this;
        }

        public Builder userId(String val) {
            userId = val;
            return this;
        }

        public Builder content(Object val) {
            content = val;
            return this;
        }

        public Builder file(Map item) {
            this.content = item;
            if (nonNull(item.get(Constants.FILE_ID))) {
                this.id = String.valueOf(item.get(Constants.FILE_ID));
            }
            if (nonNull(item.get(Constants.MIME_TYPE))) {
                this.mimeType = String.valueOf(item.get(Constants.MIME_TYPE));
            }
            if (nonNull(item.get(Constants.USER_ID))) {
                this.userId = String.valueOf(item.get(Constants.USER_ID));
            }
            if (nonNull(item.get(Constants.CONTENT))) {
                this.content = String.valueOf(item.get(Constants.CONTENT));
            }
            return this;
        }

        public FileMeta build() {
            return new FileMeta(this);
        }
    }
}