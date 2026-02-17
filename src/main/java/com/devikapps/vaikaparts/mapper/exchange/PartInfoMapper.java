package com.devikapps.vaikaparts.mapper.exchange;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.exchange.PartInfo;
import com.devikapps.vaikaparts.repository.model.exchange.JPartInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {ValueObjectMapper.class, ImageUrlMapper.class})
public interface PartInfoMapper {

  @Mapping(source = "partImageBuckets", target = "part.imageUrls", qualifiedByName = "toUrlList")
  @Mapping(source = "id", target = "part.id")
  @Mapping(source = "partName", target = "part.name")
  @Mapping(source = "carBrand", target = "part.carBrand")
  @Mapping(source = "carModel", target = "part.carModel")
  @Mapping(source = "carYear", target = "part.carYear")
  @Mapping(source = "partCategory", target = "part.partCategory")
  PartInfo toDomain(JPartInfo jPartInfo);

  @Mapping(target = "partImageBuckets", ignore = true)
  @Mapping(target = "offer", ignore = true)
  @Mapping(source = "part.name", target = "partName")
  @Mapping(source = "part.carBrand", target = "carBrand")
  @Mapping(source = "part.carModel", target = "carModel")
  @Mapping(source = "part.carYear", target = "carYear")
  @Mapping(source = "part.partCategory", target = "partCategory")
  JPartInfo toPersistence(PartInfo partInfo);
}
