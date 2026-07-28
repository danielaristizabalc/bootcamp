package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.constants.Constants;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.domain.model.EmailValidationResult;
import com.example.resilient_api.domain.spi.EmailValidatorGateway;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import com.example.resilient_api.domain.api.UserServicePort;
import reactor.core.publisher.Mono;

public class UserUseCase implements UserServicePort {

    private final UserPersistencePort userPersistencePort;
    private final EmailValidatorGateway validatorGateway;

    public UserUseCase(UserPersistencePort userPersistencePort, EmailValidatorGateway validatorGateway) {
        this.userPersistencePort = userPersistencePort;
        this.validatorGateway = validatorGateway;
    }

    @Override
    public Mono<User> registerUser(User user, String messageId) {
        return userPersistencePort.existByEmail(user.email())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.USER_ALREADY_EXISTS)))
                .flatMap(exists -> validateEmail(user.email(), messageId))
                .flatMap(validationResult -> validationResult.deliverability().equals(Constants.DELIVERABLE)
                        ? userPersistencePort.save(user)
                        : Mono.error(new BusinessException(TechnicalMessage.INVALID_EMAIL)))
                ;
    }

    private Mono<EmailValidationResult> validateEmail(String email, String messageId) {
        return validatorGateway.validateEmail(email, messageId)
                .filter(validationResult -> validationResult.deliverability().equals(Constants.DELIVERABLE))
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_EMAIL)));
    }

}
