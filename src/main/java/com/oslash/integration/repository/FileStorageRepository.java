package com.oslash.integration.repository;

import com.oslash.integration.models.FileStorage;
import com.oslash.integration.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The interface File storage repository.
 */
@Repository
public interface FileStorageRepository extends ReactiveCrudRepository<FileStorage, String> {
    /**
     * Find all by mime type flux.
     *
     * @param value the value
     * @return the flux
     */
    Flux<FileStorage> findAllByMimeType(String value);

    /**
     * Find all by user id flux.
     *
     * @param value the value
     * @return the flux
     */
    Flux<FileStorage> findAllByUserId(String value);

    /**
     * Find distinct by file id.
     *
     * @param value the value
     * @return the flux
     */
    Mono<FileStorage> findDistinctByFileId(String value);
}