package com.oslash.integration.repository;

import com.oslash.integration.models.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface User reactive repository.
 */
@Repository
public interface UserReactiveRepository extends ReactiveMongoRepository<User, String> {

}