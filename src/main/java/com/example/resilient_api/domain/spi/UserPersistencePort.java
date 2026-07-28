package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.User;
import reactor.core.publisher.Mono;

public interface UserPersistencePort {
    Mono<User> save(User user);
    Mono<Boolean> existByEmail(String email);
}
