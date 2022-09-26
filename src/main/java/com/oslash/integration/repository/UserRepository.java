package com.oslash.integration.repository;

import com.oslash.integration.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The interface User repository.
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, String> {
    /**
     * Find all by email flux.
     *
     * @param value the value
     * @return the flux
     */
    Flux<User> findAllByEmail(String value);

    /**
     * Find first by refresh token mono.
     *
     * @param refreshToken the refresh token
     * @return the mono
     */
    Mono<User> findFirstByRefreshToken(Mono<String> refreshToken);
}