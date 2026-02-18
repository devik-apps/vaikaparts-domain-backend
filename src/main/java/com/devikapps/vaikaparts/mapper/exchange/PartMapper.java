package com.devikapps.vaikaparts.mapper.exchange;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.exchange.Part;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {ImageUrlMapper.class, ValueObjectMapper.class})
public interface PartMapper {

  @Mapping(source = "imageBuckets", target = "imageUrls", qualifiedByName = "toUrlList")
  @Mapping(source = "partName", target = "name")
  Part toDomain(JPart jpart);

  @Mapping(target = "imageBuckets", ignore = true)
  @Mapping(target = "demand", ignore = true)
  @Mapping(source = "name", target = "partName")
  JPart toPersistence(Part part);
}
