package com.example.resilient_api.infrastructure.adapters.capabilityadapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeleteCapabilityRequestDTO(@JsonProperty("capacidadIds") List<Long> capabilityIds) {
}