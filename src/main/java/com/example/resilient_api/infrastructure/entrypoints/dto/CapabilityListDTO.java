package com.example.resilient_api.infrastructure.entrypoints.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityListDTO {
    private Long id;
    private String nombre;
    private List<TechnologyDTO> tecnologias;
}