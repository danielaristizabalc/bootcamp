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
public class BootcampPageDTO {
    private List<BootcampListItemDTO> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}