package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.model.user.Manager;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ManagerMapper {

  @Mapping(source = "role", target = "managerRole")
  @Mapping(target = "profileImgKey", ignore = true)
  JManager toPersistence(Manager manager);
}
