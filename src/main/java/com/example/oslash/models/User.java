package com.example.oslash.models;

import com.google.api.client.json.GenericJson;
import com.google.api.services.people.v1.model.Person;
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
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String email;
    private Object content;

    /**
     * TODO mask this
     */
    private String refreshToken;

    public User(String id, String email, Object content) {
        this.id = id;
        this.email = email;
        this.content = content;
    }

    private User(Builder builder) {
        setId(builder.id);
        setEmail(builder.email);
        setContent(builder.content);
        setRefreshToken(builder.refreshToken);
    }

    public static final class Builder {
        private String id;
        private String email;
        private Object content;
        private String refreshToken;

        public Builder() {
        }

        public Builder id(String val) {
            id = val;
            return this;
        }

        public Builder email(String val) {
            email = val;
            return this;
        }

        public Builder content(Object val) {
            content = val;
            return this;
        }

        /**
         * mask this value
         */
        public Builder refreshToken(String val) {
            refreshToken = val;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}