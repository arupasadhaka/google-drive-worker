package com.oslash.integration.service;


import com.oslash.integration.models.User;
import com.oslash.integration.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The type User service.
 */
@Service
public class UserService {
    /**
     * The User repository.
     */
    @Autowired
    UserRepository userRepository;

    /**
     * The Reactive mongo template.
     */
    @Autowired
    ReactiveMongoTemplate reactiveMongoTemplate;

    /**
     * Find by id mono.
     *
     * @param id the id
     * @return the mono
     */
    public Mono<User> findById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Find all flux.
     *
     * @return the flux
     */
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Save mono.
     *
     * @param user the user
     * @return the mono
     */
    public Mono<User> save(User user) {
        return userRepository.save(user);
    }

}
