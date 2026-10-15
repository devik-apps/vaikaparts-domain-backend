package com.devikapps.vaikaparts.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PartCondition;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.exchange.JPartInfo;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

class DemandOutcomeControllerIT extends FacadeIT {

  private static final String RESEARCHER_SUPABASE_ID = "outcome-researcher-supabase";

  @Autowired private MockMvc mockMvc;
  @Autowired private DemandRepository demandRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper valueObjectMapper;

  private JResearcher researcher;
  private JSeller seller;

  @BeforeEach
  void setUp() {
    researcher = createResearcher(RESEARCHER_SUPABASE_ID);
    seller = createSeller();
    authenticate(RESEARCHER_SUPABASE_ID);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_close_offer_through_researcher_outcome_endpoint() throws Exception {
    var demand = createDemand(researcher);
    var offer = createOffer(demand);

    mockMvc
        .perform(
            patch("/v1/demands/{demandId}/offers/{offerId}/status", demand.getId(), offer.getId())
                .param("status", PostStatus.CLOSED.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(offer.getId()))
        .andExpect(jsonPath("$.status").value(PostStatus.CLOSED.name()));
  }

  @Test
  void should_hide_foreign_demand_offer_outcome_endpoint() throws Exception {
    var otherResearcher = createResearcher("other-" + randomUUID());
    var demand = createDemand(otherResearcher);
    var offer = createOffer(demand);

    mockMvc
        .perform(
            patch("/v1/demands/{demandId}/offers/{offerId}/status", demand.getId(), offer.getId())
                .param("status", PostStatus.CLOSED.name()))
        .andExpect(status().isNotFound());

    org.junit.jupiter.api.Assertions.assertEquals(
        PostStatus.PUBLISHED, offerRepository.findById(offer.getId()).orElseThrow().getStatus());
  }

  private void authenticate(String supabaseId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                supabaseId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  private JResearcher createResearcher(String supabaseId) {
    return userRepository.save(
        JResearcher.builder()
            .id("researcher-" + randomUUID())
            .supabaseUserId(supabaseId)
            .name("Researcher")
            .email(randomUUID() + "@example.com")
            .phoneNumber(
                "+261"
                    + String.format("%09d", Math.floorMod(randomUUID().hashCode(), 1_000_000_000)))
            .profileImgKey("")
            .location(valueObjectMapper.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createSeller() {
    return userRepository.save(
        JSeller.builder()
            .id("seller-" + randomUUID())
            .supabaseUserId("seller-supabase-" + randomUUID())
            .name("Seller")
            .email(randomUUID() + "@example.com")
            .phoneNumber("+261340000002")
            .profileImgKey("")
            .location(valueObjectMapper.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createDemand(JResearcher owner) {
    var part =
        JPart.builder()
            .id("part-" + randomUUID())
            .partName("Headlight")
            .carBrand("Toyota")
            .carModel("Corolla")
            .carYear(2015)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();
    var demand =
        JDemand.builder()
            .id("demand-" + randomUUID())
            .description("Need a headlight")
            .attachedPhotoBucketKeys(new ArrayList<>())
            .part(part)
            .researcher(owner)
            .status(PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    part.setDemand(demand);
    return demandRepository.save(demand);
  }

  private JOffer createOffer(JDemand demand) {
    var partInfo =
        JPartInfo.builder()
            .id("part-info-" + randomUUID())
            .partName("Headlight")
            .carBrand("Toyota")
            .carModel("Corolla")
            .carYear(2015)
            .partCategory(PartCategory.FOG_LIGHTS)
            .condition(PartCondition.USED)
            .price(150.0)
            .partImageBuckets(new ArrayList<>())
            .build();
    var offer =
        JOffer.builder()
            .id("offer-" + randomUUID())
            .demand(demand)
            .description("Available headlight")
            .attachedPhotoBucketKeys(new ArrayList<>())
            .partInfo(partInfo)
            .seller(seller)
            .status(PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    partInfo.setOffer(offer);
    return offerRepository.save(offer);
  }
}
