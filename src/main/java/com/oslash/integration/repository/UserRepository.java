package com.oslash.integration.repository;

import com.oslash.integration.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository  extends ReactiveCrudRepository<User, String> {
    Flux<User> findAllByEmail(String value);
    Mono<User> findFirstByRefreshToken(Mono<String> refreshToken);
}