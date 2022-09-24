package com.example.oslash.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


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

    private FileMeta(Builder builder) {
        setId(builder.id);
        setMimeType(builder.mimeType);
        setContent(builder.content);
    }

    public static final class Builder {
        private String id;
        private String mimeType;
        private Object content;

        public Builder() {
        }

        public Builder id(String val) {
            id = val;
            return this;
        }

        public Builder email(String val) {
            mimeType = val;
            return this;
        }

        public Builder content(Object val) {
            content = val;
            return this;
        }

        public FileMeta build() {
            return new FileMeta(this);
        }
    }
}