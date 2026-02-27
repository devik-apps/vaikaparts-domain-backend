package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {SellerMapper.class, DemandMapper.class, ValueObjectMapper.class})
public interface NotificationMapper {
  @Mapping(target = "notificationRequestedId", source = "jDemandPublishedNotification")
  DemandPublishedNotification toDomain(JDemandPublishedNotification jDemandPublishedNotification);

  @Mapping(target = "notificationRequested", ignore = true)
  JDemandPublishedNotification toPersistence(
      DemandPublishedNotification demandPublishedNotification);
}
