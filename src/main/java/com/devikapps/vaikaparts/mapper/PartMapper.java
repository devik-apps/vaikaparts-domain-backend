package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.Part;
import com.devikapps.vaikaparts.repository.model.JPart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {ImageUrlMapper.class, ValueObjectMapper.class})
public interface PartMapper {

  @Mapping(source = "imageBucket", target = "imageUrl", qualifiedByName = "getPresignedUrl")
  @Mapping(source = "partName", target = "name")
  Part toDomain(JPart jpart);

  @Mapping(target = "imageBucket", ignore = true)
  @Mapping(source = "name", target = "partName")
  JPart toPersistence(Part part);
}
