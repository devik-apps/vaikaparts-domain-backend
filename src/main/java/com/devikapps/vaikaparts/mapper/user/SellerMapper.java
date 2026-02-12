package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface SellerMapper {

  Seller toDomain(JSeller jSeller);

  JSeller toPersistence(Seller seller);
}
