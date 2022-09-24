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

        private String userId;

        private File file;

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

        public Builder file(Map item) {
            this.content = item;
            File file = new File();
            this.userId = String.valueOf(item.get(Constants.FILE_ID));
            this.mimeType = String.valueOf(item.get(Constants.MIME_TYPE));
            file.setId(String.valueOf(item.get(Constants.FILE_ID)));
            this.file = file;
            return this;
        }

        public FileMeta build() {
            return new FileMeta(this);
        }
    }
}