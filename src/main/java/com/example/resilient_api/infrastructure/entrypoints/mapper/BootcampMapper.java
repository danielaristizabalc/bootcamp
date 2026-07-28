package com.example.resilient_api.infrastructure.entrypoints.mapper;

import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.infrastructure.entrypoints.dto.BootcampDTO;
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
}
