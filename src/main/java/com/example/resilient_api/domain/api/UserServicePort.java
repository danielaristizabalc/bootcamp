package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.User;
import reactor.core.publisher.Mono;

public interface UserServicePort {
    Mono<User> registerUser(User user, String messageId);
}
