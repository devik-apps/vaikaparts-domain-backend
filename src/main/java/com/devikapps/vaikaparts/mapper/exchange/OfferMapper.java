package com.devikapps.vaikaparts.mapper.exchange;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {ImageUrlMapper.class, PartInfoMapper.class})
public interface OfferMapper {

  @Mapping(
      source = "attachedPhotoBucketKeys",
      target = "attachedPhotosUrls",
      qualifiedByName = "toUrlList")
  @Mapping(source = "seller.id", target = "sellerId")
  @Mapping(source = "partInfo", target = "partsInfo")
  Offer toDomain(JOffer jOffer);

  @Mapping(target = "attachedPhotoBucketKeys", ignore = true)
  @Mapping(target = "seller", ignore = true)
  @Mapping(source = "partsInfo", target = "partInfo")
  JOffer toPersistence(Offer offer);
}
