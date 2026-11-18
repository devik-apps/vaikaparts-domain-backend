package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.util.ImageUploader;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class OfferServicePrivacyTest {

  @Test
  void sellerOfferResponsesDoNotExposeResearcherContactDetails() {
    var offerRepository = mock(OfferRepository.class);
    var offerMapper = mock(OfferMapper.class);
    var userService = mock(UserService.class);
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
    when(userService.getCurrentUser())
        .thenReturn(JSeller.builder().id("seller-id").userType(UserType.SELLER).build());
    when(offerRepository.findBySellerIdWithRelations(eq("seller-id"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(JOffer.builder().id("offer-id").build())));
    when(offerMapper.toDomain(any(JOffer.class))).thenReturn(offer);

    var service =
        new OfferService(
            offerRepository,
            mock(EventProducer.class),
            mock(DemandRepository.class),
            offerMapper,
            mock(DemandMapper.class),
            mock(SellerService.class),
            userService,
            new Paginator(),
            mock(BucketComponent.class),
            mock(ImageUploader.class));

    var result = service.getAllSellerOffers(0, 10).getContent().getFirst();

    assertNull(result.getDemand().getResearcher().getSupabaseUserId());
    assertNull(result.getDemand().getResearcher().getName());
    assertNull(result.getDemand().getResearcher().getEmail());
    assertNull(result.getDemand().getResearcher().getPhoneNumber());
    assertNull(result.getDemand().getResearcher().getProfileImgUrl());
  }
}
