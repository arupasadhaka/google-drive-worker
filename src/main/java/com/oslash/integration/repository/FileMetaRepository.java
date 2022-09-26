package com.oslash.integration.repository;

import com.oslash.integration.models.FileMeta;
import com.oslash.integration.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * The interface File meta repository.
 */
@Repository
public interface FileMetaRepository extends ReactiveCrudRepository<FileMeta, String> {
    /**
     * Find all by mime type flux.
     *
     * @param value the value
     * @return the flux
     */
    Flux<User> findAllByMimeType(String value);

    /**
     * Find all by user id flux.
     *
     * @param value the value
     * @return the flux
     */
    Flux<User> findAllByUserId(String value);
}