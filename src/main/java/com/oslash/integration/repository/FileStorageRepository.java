package com.oslash.integration.repository;

import com.oslash.integration.models.FileMeta;
import com.oslash.integration.models.FileStorage;
import com.oslash.integration.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface FileStorageRepository extends ReactiveCrudRepository<FileStorage, String> {
    Flux<User> findAllByMimeType(String value);
    Flux<User> findAllByUserId(String value);
}