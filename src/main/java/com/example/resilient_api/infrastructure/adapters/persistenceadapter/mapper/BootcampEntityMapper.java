package com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper;

import com.example.resilient_api.domain.model.Bootcamp;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.BootcampEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BootcampEntityMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "duration", target = "duration")
    @Mapping(target = "capabilityIds", ignore = true)
    Bootcamp toModel(BootcampEntity entity);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "duration", target = "duration")
    BootcampEntity toEntity(Bootcamp bootcamp);
}
