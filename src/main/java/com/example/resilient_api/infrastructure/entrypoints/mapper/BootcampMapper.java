package com.example.resilient_api.infrastructure.entrypoints.mapper;

import com.example.resilient_api.domain.model.BootcampListItem;
import com.example.resilient_api.domain.model.BootcampListResult;
import com.example.resilient_api.domain.model.BootcampBasicInfo;
import com.example.resilient_api.domain.model.BootcampValidationResult;
import com.example.resilient_api.domain.model.Capability;
import com.example.resilient_api.domain.model.Technology;
import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampBasicInfoDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampListItemDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampPageDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampValidationResponseDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.CapabilityListDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.TechnologyDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BootcampMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "duration", target = "duration")
    @Mapping(source = "capabilityIds", target = "capabilityIds")
    Bootcamp bootcampDTOToBootcamp(BootcampDTO bootcampDTO);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "duration", target = "duration")
    @Mapping(source = "capabilityIds", target = "capabilityIds")
    BootcampDTO bootcampToBootcampDTO(Bootcamp bootcamp);

    default BootcampPageDTO bootcampListResultToBootcampPageDTO(BootcampListResult page) {
        return BootcampPageDTO.builder()
                .content(page.content().stream().map(this::bootcampListItemToDto).toList())
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages())
                .build();
    }

    default BootcampListItemDTO bootcampListItemToDto(BootcampListItem bootcamp) {
        return BootcampListItemDTO.builder()
                .id(bootcamp.id())
                .name(bootcamp.name())
                .description(bootcamp.description())
                .releaseDate(bootcamp.releaseDate())
                .duration(bootcamp.duration())
                .capabilities(bootcamp.capabilities().stream().map(this::capabilityToDto).toList())
                .build();
    }

    default CapabilityListDTO capabilityToDto(Capability capability) {
        return CapabilityListDTO.builder()
                .id(capability.id())
                .nombre(capability.name())
                .tecnologias(capability.technologies().stream().map(this::technologyToDto).toList())
                .build();
    }

    default TechnologyDTO technologyToDto(Technology technology) {
        return TechnologyDTO.builder()
                .id(technology.id())
                .name(technology.name())
                .build();
    }

    default BootcampValidationResponseDTO bootcampValidationResultToDto(BootcampValidationResult result) {
        return BootcampValidationResponseDTO.builder()
                .bootcamps(result.bootcamps().stream().map(this::bootcampBasicInfoToDto).toList())
                .build();
    }

    default BootcampBasicInfoDTO bootcampBasicInfoToDto(BootcampBasicInfo bootcamp) {
        return BootcampBasicInfoDTO.builder()
                .id(bootcamp.id())
                .name(bootcamp.name())
                .releaseDate(bootcamp.releaseDate())
                .duration(bootcamp.duration())
                .build();
    }
}
