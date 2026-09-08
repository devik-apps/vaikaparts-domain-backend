package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.OfferNotificationRequested;
import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.util.ImageUploader;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OfferNotificationPublicationTest {
  @Mock OfferRepository offerRepository;
  @Mock DemandRepository demandRepository;
  @Mock OfferMapper offerMapper;
  @Mock DemandMapper demandMapper;
  @Mock SellerService sellerService;
  @Mock Paginator paginator;
  @Mock BucketComponent bucketComponent;
  @Mock ImageUploader imageUploader;
  @Mock EventProducer<OfferNotificationRequested> producer;
  @InjectMocks OfferService service;
  @Captor ArgumentCaptor<List<OfferNotificationRequested>> events;
  JOffer offer;
  TransactionTemplate transaction =
      new TransactionTemplate(
          new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
              return new Object();
            }

            @Override
            protected void doBegin(Object tx, TransactionDefinition definition) {}

            @Override
            protected void doCommit(DefaultTransactionStatus status) {}

            @Override
            protected void doRollback(DefaultTransactionStatus status) {}
          });

  @BeforeEach
  void setUp() {
    offer =
        JOffer.builder()
            .id("offer")
            .status(PostStatus.DRAFT)
            .seller(JSeller.builder().id("seller").build())
            .demand(
                JDemand.builder()
                    .id("demand")
                    .researcher(JResearcher.builder().id("researcher").build())
                    .build())
            .build();
    when(sellerService.getCurrentSeller()).thenReturn(Seller.builder().id("seller").build());
    when(offerRepository.findByIdWithRelations("offer")).thenReturn(Optional.of(offer));
  }

  @Test
  void publishes_only_after_commit_to_the_demand_owner() {
    when(offerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    transaction.executeWithoutResult(
        status -> {
          service.updateOfferStatus("offer", PostStatus.PUBLISHED);
          verifyNoInteractions(producer);
        });
    verify(producer).accept(events.capture());
    assertEquals(1, events.getValue().size());
    var event = events.getValue().getFirst();
    assertEquals("offer", event.getOfferId());
    assertEquals("researcher", event.getResearcherId());
    assertNotNull(event.getId());
  }

  @Test
  void rollback_does_not_publish() {
    when(offerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    transaction.executeWithoutResult(
        status -> {
          service.updateOfferStatus("offer", PostStatus.PUBLISHED);
          status.setRollbackOnly();
        });
    verifyNoInteractions(producer);
  }

  @Test
  void canceling_published_offer_does_not_publish() {
    offer.setStatus(PostStatus.PUBLISHED);
    when(offerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service.updateOfferStatus("offer", PostStatus.CANCELED);
    verifyNoInteractions(producer);
  }

  @Test
  void already_published_offer_does_not_publish_again() {
    offer.setStatus(PostStatus.PUBLISHED);
    assertThrows(
        IllegalStateException.class,
        () -> service.updateOfferStatus("offer", PostStatus.PUBLISHED));
    verifyNoInteractions(producer);
    verify(offerRepository, never()).save(any());
  }

  @Test
  void publishes_immediately_without_transaction_synchronization() {
    when(offerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service.updateOfferStatus("offer", PostStatus.PUBLISHED);
    verify(producer).accept(any());
  }
}
