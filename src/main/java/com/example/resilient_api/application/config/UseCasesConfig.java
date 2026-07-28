package com.example.resilient_api.application.config;

import com.example.resilient_api.domain.api.BootcampServicePort;
import com.example.resilient_api.domain.api.BootcampListServicePort;
import com.example.resilient_api.domain.api.UserServicePort;
import com.example.resilient_api.domain.spi.BootcampPersistencePort;
import com.example.resilient_api.domain.spi.CapabilityGateway;
import com.example.resilient_api.domain.spi.EmailValidatorGateway;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import com.example.resilient_api.domain.usecase.BootcampUseCase;
import com.example.resilient_api.domain.usecase.BootcampListUseCase;
import com.example.resilient_api.domain.usecase.UserUseCase;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.BootcampPersistenceAdapter;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.UserPersistenceAdapter;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.BootcampEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.UserEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampCapabilityRepository;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.BootcampRepository;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@RequiredArgsConstructor
public class UseCasesConfig {
        private final UserRepository userRepository;
        private final UserEntityMapper userEntityMapper;
        private final BootcampRepository bootcampRepository;
        private final BootcampCapabilityRepository bootcampCapabilityRepository;
        private final BootcampEntityMapper bootcampEntityMapper;
        private final DatabaseClient databaseClient;
        private final TransactionalOperator transactionalOperator;

        @Bean
        public UserPersistencePort usersPersistencePort() {
                return new UserPersistenceAdapter(userRepository,userEntityMapper);
        }

        @Bean
        public UserServicePort usersServicePort(UserPersistencePort usersPersistencePort, EmailValidatorGateway emailValidatorGateway){
                return new UserUseCase(usersPersistencePort, emailValidatorGateway);
        }

        @Bean
        public BootcampPersistencePort bootcampPersistencePort() {
                return new BootcampPersistenceAdapter(
                    bootcampRepository, 
                    bootcampCapabilityRepository, 
                    bootcampEntityMapper,
                                        databaseClient,
                    transactionalOperator
                );
        }

        @Bean
        public BootcampServicePort bootcampServicePort(BootcampPersistencePort bootcampPersistencePort,
                                                        CapabilityGateway capabilityGateway) {
                return new BootcampUseCase(bootcampPersistencePort, capabilityGateway);
        }

        @Bean
        public BootcampListServicePort bootcampListServicePort(BootcampPersistencePort bootcampPersistencePort,
                                                               CapabilityGateway capabilityGateway) {
                return new BootcampListUseCase(bootcampPersistencePort, capabilityGateway);
        }
}
