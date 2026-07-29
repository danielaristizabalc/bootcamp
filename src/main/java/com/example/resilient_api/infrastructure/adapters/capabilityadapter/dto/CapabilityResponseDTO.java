package com.example.resilient_api.infrastructure.adapters.capabilityadapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapabilityResponseDTO {
    private List<CapabilityDTO> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CapabilityDTO {
        private Long id;
        @JsonProperty("nombre")
        private String nombre;
        @JsonProperty("descripcion")
        private String descripcion;
        @JsonProperty("cantidadTecnologias")    
        private Integer cantidadTecnologias;
        private List<TecnologiaDTO> tecnologias;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TecnologiaDTO {
            private Long id;
            private String name;
        }
    }
}
