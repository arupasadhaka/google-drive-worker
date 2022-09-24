package com.example.oslash.repository;

import com.example.oslash.models.FileMeta;
import com.example.oslash.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface FileMetaRepository extends ReactiveCrudRepository<FileMeta, String> {
    Flux<User> findAllByMimeType(String value);
    Flux<User> findAllByUserId(String value);
}