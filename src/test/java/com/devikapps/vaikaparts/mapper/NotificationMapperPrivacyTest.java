package com.devikapps.vaikaparts.mapper;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.mapper.user.ManagerMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import org.junit.jupiter.api.Test;

class NotificationMapperPrivacyTest {

  @Test
  void sellerNotificationsDoNotExposeResearcherContactDetailsInTheOfferResource() {
    var sellerMapper = mock(SellerMapper.class);
    var offerMapper = mock(OfferMapper.class);
    var researcher =
        Researcher.builder()
            .id("buyer-id")
            .supabaseUserId("buyer-auth-id")
            .name("Buyer Name")
            .email("buyer@example.com")
            .phoneNumber("0340000000")
            .build();
    var offer =
        Offer.builder()
            .id("offer-id")
            .demand(Demand.builder().id("demand-id").researcher(researcher).build())
            .build();
    when(sellerMapper.toSeller(any(JSeller.class)))
        .thenReturn(Seller.builder().id("seller-id").userType(UserType.SELLER).build());
    when(offerMapper.toDomain(any(JOffer.class))).thenReturn(offer);
    var mapper =
        new NotificationMapper(
            sellerMapper,
            mock(ResearcherMapper.class),
            mock(ManagerMapper.class),
            mock(DemandMapper.class),
            offerMapper);
    var notification =
        JDemandPublishedNotification.builder()
            .recipient(JSeller.builder().userType(UserType.SELLER).build())
            .offer(JOffer.builder().id("offer-id").build())
            .build();

    var result = mapper.toDomain(notification);
    var nestedResearcher = ((Offer) result.getResource()).getDemand().getResearcher();

    assertNull(nestedResearcher.getSupabaseUserId());
    assertNull(nestedResearcher.getName());
    assertNull(nestedResearcher.getEmail());
    assertNull(nestedResearcher.getPhoneNumber());
    assertNull(nestedResearcher.getProfileImgUrl());
  }
}
