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
  @Mapping(source = "partInfos", target = "partsInfos")
  Offer toDomain(JOffer jOffer);

  @Mapping(target = "attachedPhotoBucketKeys", ignore = true)
  @Mapping(target = "seller", ignore = true)
  @Mapping(source = "partsInfos", target = "partInfos")
  JOffer toPersistence(Offer offer);
}
