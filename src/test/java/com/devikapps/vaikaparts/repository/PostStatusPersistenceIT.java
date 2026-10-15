package com.devikapps.vaikaparts.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PostStatusPersistenceIT extends FacadeIT {

  @Autowired private DemandRepository demandRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper valueObjectMapper;
  @Autowired private EntityManager entityManager;

  @Test
  void should_round_trip_satisfied_demand_and_offer_and_closed_offer() {
    var researcher = userRepository.save(newResearcher());
    var seller = userRepository.save(newSeller());
    var satisfiedDemand = demandRepository.saveAndFlush(newDemand("satisfied-demand", researcher));
    offerRepository.saveAndFlush(
        newOffer("satisfied-offer", satisfiedDemand, seller, PostStatus.SATISFIED));
    offerRepository.saveAndFlush(
        newOffer("closed-offer", satisfiedDemand, seller, PostStatus.CLOSED));
    entityManager.clear();

    assertEquals(
        PostStatus.SATISFIED,
        demandRepository.findById("satisfied-demand").orElseThrow().getStatus());
    assertEquals(
        PostStatus.SATISFIED,
        offerRepository.findById("satisfied-offer").orElseThrow().getStatus());
    assertEquals(
        PostStatus.CLOSED, offerRepository.findById("closed-offer").orElseThrow().getStatus());
  }

  private JResearcher newResearcher() {
    var now = OffsetDateTime.now();
    return JResearcher.builder()
        .id("status-researcher")
        .supabaseUserId("status-researcher-supabase")
        .name("Status Researcher")
        .email("researcher@example.com")
        .phoneNumber("+261340000001")
        .profileImgKey("")
        .location(valueObjectMapper.map(Location.getDefault()))
        .userType(UserType.RESEARCHER)
        .status(UserStatus.ENABLED)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private JSeller newSeller() {
    var now = OffsetDateTime.now();
    return JSeller.builder()
        .id("status-seller")
        .supabaseUserId("status-seller-supabase")
        .name("Status Seller")
        .email("seller@example.com")
        .phoneNumber("+261340000002")
        .profileImgKey("")
        .garageName("Status Garage")
        .location(valueObjectMapper.map(Location.getDefault()))
        .userType(UserType.SELLER)
        .status(UserStatus.ENABLED)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private JDemand newDemand(String id, JResearcher researcher) {
    var now = LocalDateTime.now();
    return JDemand.builder()
        .id(id)
        .description("Status persistence demand")
        .attachedPhotoBucketKeys(new ArrayList<>())
        .researcher(researcher)
        .status(PostStatus.SATISFIED)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private JOffer newOffer(String id, JDemand demand, JSeller seller, PostStatus status) {
    var now = LocalDateTime.now();
    return JOffer.builder()
        .id(id)
        .description("Status persistence offer")
        .attachedPhotoBucketKeys(new ArrayList<>())
        .demand(demand)
        .seller(seller)
        .status(status)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
