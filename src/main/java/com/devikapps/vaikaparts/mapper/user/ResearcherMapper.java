package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ValueObjectMapper.class)
public interface ResearcherMapper {

  Researcher toDomain(JResearcher jResearcher);

  JResearcher toPersistence(Researcher researcher);
}
