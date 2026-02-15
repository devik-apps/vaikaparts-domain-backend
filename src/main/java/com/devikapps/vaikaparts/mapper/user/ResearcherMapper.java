package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface ResearcherMapper {

  @Mapping(target = "profileImgKey", ignore = true)
  JResearcher toPersistence(Researcher researcher);
}
