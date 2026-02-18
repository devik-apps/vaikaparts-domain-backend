package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {ValueObjectMapper.class, ImageUrlMapper.class})
public interface ResearcherMapper {

  @Mapping(target = "profileImgKey", ignore = true)
  JResearcher toPersistence(Researcher researcher);

  @Mapping(target = "profileImgUrl", source = "profileImgKey", qualifiedByName = "getPresignedUrl")
  Researcher toResearcher(JResearcher jResearcher);
}
