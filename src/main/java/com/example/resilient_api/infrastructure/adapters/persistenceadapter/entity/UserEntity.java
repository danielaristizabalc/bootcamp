package com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "users")
@Getter
@Setter
@RequiredArgsConstructor
public class UserEntity {
    @Id
    private Long id;
    private String name;
    private String email;
}
