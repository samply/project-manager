package de.samply.db.utils;

import de.samply.db.model.Query;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface QueryMapper {

    @Mapping(target = "outputs", ignore = true)
    void updateQuery(Query source, @MappingTarget Query target);

}
