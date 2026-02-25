package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.event.JNotification;
import org.mapstruct.Mapper;

@Mapper(
    componentModel = "spring",
    uses = {SellerMapper.class, DemandMapper.class, ValueObjectMapper.class})
public interface NotificationMapper {
  Notification toDomain(JNotification jNotification);

  JNotification toPersistence(Notification notification);
}
