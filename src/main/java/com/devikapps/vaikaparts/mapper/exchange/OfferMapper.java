package com.devikapps.vaikaparts.mapper.exchange;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

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
  @Mapping(source = "seller", target = "sellerMaskedName", qualifiedByName = "maskSellerName")
  @Mapping(source = "seller", target = "sellerVerified", qualifiedByName = "sellerVerified")
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

  @Named("maskSellerName")
  default String maskSellerName(JSeller seller) {
    if (seller == null || seller.getName() == null) return "";

    var name = seller.getName().trim();
    if (name.isEmpty()) return "";

    var firstCharacterEnd = name.offsetByCodePoints(0, 1);
    var remainingCharacters = name.codePointCount(firstCharacterEnd, name.length());
    return name.substring(0, firstCharacterEnd) + "*".repeat(remainingCharacters);
  }

  @Named("sellerVerified")
  default boolean sellerVerified(JSeller seller) {
    return seller != null && Boolean.TRUE.equals(seller.getIsVerified());
  }
}
