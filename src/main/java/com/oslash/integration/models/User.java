package com.oslash.integration.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


/**
 * The type User.
 */
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

    /**
     * Instantiates a new User.
     *
     * @param id      the id
     * @param email   the email
     * @param content the content
     */
    public User(String id, String email, Object content) {
        this.id = id;
        this.email = email;
        this.content = content;
    }

    /**
     * Instantiates a new User.
     *
     * @param builder the builder
     */
    private User(Builder builder) {
        setId(builder.id);
        setEmail(builder.email);
        setContent(builder.content);
        setRefreshToken(builder.refreshToken);
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
        private String id;
        private String email;
        private Object content;
        private String refreshToken;

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
         * Email builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder email(String val) {
            email = val;
            return this;
        }

        /**
         * Content builder.
         *
         * @param val the val
         * @return the builder
         */
        public Builder content(Object val) {
            content = val;
            return this;
        }

        /**
         * mask this value
         *
         * @param val the val
         * @return the builder
         */
        public Builder refreshToken(String val) {
            refreshToken = val;
            return this;
        }

        /**
         * Build user.
         *
         * @return the user
         */
        public User build() {
            return new User(this);
        }
    }
}