package com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "bootcamp_capabilities")
@Getter
@Setter
@RequiredArgsConstructor
public class BootcampCapabilityEntity {
    @Id
    private Long id;
    private Long bootcampId;
    private Long capabilityId;
    private LocalDateTime createdAt;
}
