package com.devikapps.vaikaparts.mapper.exchange;

import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = "spring",
    uses = {ImageUrlMapper.class, PartMapper.class, ResearcherMapper.class})
public interface DemandMapper {

  @Mapping(
      source = "attachedPhotoBucketKeys",
      target = "attachedPhotosUrls",
      qualifiedByName = "toUrlList")
  Demand toDomain(JDemand jDemand);

  @AfterMapping
  default void linkPart(@MappingTarget JDemand jDemand) {
    if (jDemand.getPart() != null) jDemand.getPart().setDemand(jDemand);
  }

  @Mapping(target = "attachedPhotoBucketKeys", ignore = true)
  @Mapping(target = "publishedRequestedLogs", ignore = true)
  @Mapping(target = "offers", ignore = true)
  JDemand toPersistence(Demand demand);
}
