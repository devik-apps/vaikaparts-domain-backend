package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.event.JNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {SellerMapper.class, DemandMapper.class, ValueObjectMapper.class})
public interface NotificationMapper {
  @Mapping(target = "notificationRequestedId", source = "jNotification")
  Notification toDomain(JNotification jNotification);

  @Mapping(target = "notificationRequested", ignore = true)
  JNotification toPersistence(Notification notification);
}
