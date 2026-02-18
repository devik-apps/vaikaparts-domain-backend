package com.devikapps.vaikaparts.mapper.exchange;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = "spring",
    uses = {
      ImageUrlMapper.class,
      PartInfoMapper.class,
      DemandMapper.class,
      ValueObjectMapper.class
    })
public interface OfferMapper {

  @Mapping(
      source = "attachedPhotoBucketKeys",
      target = "attachedPhotosUrls",
      qualifiedByName = "toUrlList")
  @Mapping(source = "seller.id", target = "sellerId")
  @Mapping(source = "partInfo", target = "partsInfo")
  @Mapping(source = "demand", target = "demand")
  Offer toDomain(JOffer jOffer);

  @Mapping(target = "attachedPhotoBucketKeys", ignore = true)
  @Mapping(source = "sellerId", target = "seller")
  @Mapping(target = "demand", ignore = true)
  @Mapping(source = "partsInfo", target = "partInfo")
  JOffer toPersistence(Offer offer);

  @AfterMapping
  default void linkPartInfo(@MappingTarget JOffer jOffer) {
    if (jOffer.getPartInfo() != null) jOffer.getPartInfo().setOffer(jOffer);
  }
}
