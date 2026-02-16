package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {ValueObjectMapper.class, ImageUrlMapper.class})
public interface SellerMapper {

  @Mapping(target = "profileImgKey", ignore = true)
  JSeller toPersistence(Seller seller);

  @Mapping(target = "profileImgUrl", source = "profileImgKey", qualifiedByName = "getPresignedUrl")
  Seller toSeller(JSeller jSeller);
}
