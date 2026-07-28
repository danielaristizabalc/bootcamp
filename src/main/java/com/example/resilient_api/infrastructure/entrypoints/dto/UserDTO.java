package com.example.resilient_api.infrastructure.entrypoints.dto;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
public class UserDTO {
    private Long id;
    private String name;
    private String email;
}
