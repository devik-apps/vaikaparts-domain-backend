package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OfferDemandEventPublicationTest {
  @Mock OfferRepository offers;
  @Mock OfferMapper mapper;
  @Mock SellerService sellers;
  @Mock EventProducer<DemandPublishedRequested> producer;
  @InjectMocks OfferService service;

  private void prepare() {
    var offer =
        JOffer.builder()
            .id("offer")
            .status(PostStatus.DRAFT)
            .seller(JSeller.builder().id("seller").build())
            .demand(JDemand.builder().id("demand").build())
            .build();
    when(sellers.getCurrentSeller()).thenReturn(Seller.builder().id("seller").build());
    when(offers.findByIdWithRelations("offer")).thenReturn(Optional.of(offer));
    when(offers.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  @SuppressWarnings("unchecked")
  void publication_waits_for_commit() {
    prepare();
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.updateOfferStatus("offer", PostStatus.PUBLISHED);
      verifyNoInteractions(producer);
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      ArgumentCaptor<Collection<DemandPublishedRequested>> events =
          ArgumentCaptor.forClass(Collection.class);
      verify(producer).accept(events.capture());
      var event = events.getValue().iterator().next();
      assertEquals("offer", event.getOfferId());
      assertEquals("demand", event.getDemandId());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void rollback_does_not_publish() {
    prepare();
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.updateOfferStatus("offer", PostStatus.PUBLISHED);
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verifyNoInteractions(producer);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void cancel_does_not_publish() {
    prepare();
    service.updateOfferStatus("offer", PostStatus.CANCELED);
    verifyNoInteractions(producer);
  }
}
