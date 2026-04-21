package de.samply.db.utils;

import de.samply.db.model.QueryOutput;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface QueryOutputMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "query", ignore = true)
    QueryOutput toEntity(QueryOutput source);

}
