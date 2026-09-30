package com.devikapps.vaikaparts.service.event;

import static com.devikapps.vaikaparts.model.classifier.PartCategory.BATTERY;
import static com.devikapps.vaikaparts.model.classifier.PartCategory.ENGINE_PART;
import static com.devikapps.vaikaparts.model.classifier.UserStatus.ENABLED;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.event.model.DemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SellerCategoryNotificationTest {
  static Stream<Arguments> categories() {
    return Stream.of(
        Arguments.of(true, List.of(), ENGINE_PART, true),
        Arguments.of(true, List.of(BATTERY), ENGINE_PART, true),
        Arguments.of(true, null, ENGINE_PART, true),
        Arguments.of(false, List.of(BATTERY, ENGINE_PART), ENGINE_PART, true),
        Arguments.of(false, List.of(ENGINE_PART, ENGINE_PART), ENGINE_PART, true),
        Arguments.of(false, List.of(BATTERY), ENGINE_PART, false),
        Arguments.of(false, List.of(), ENGINE_PART, false),
        Arguments.of(false, null, ENGINE_PART, false),
        Arguments.of(null, List.of(ENGINE_PART), ENGINE_PART, true),
        Arguments.of(null, null, ENGINE_PART, false),
        Arguments.of(false, List.of(ENGINE_PART), null, false),
        Arguments.of(true, List.of(), null, true));
  }

  @ParameterizedTest
  @MethodSource("categories")
  @SuppressWarnings("unchecked")
  void filters_before_creating_children_and_counts_only_matching_sellers(
      Boolean handleAll, List<PartCategory> categories, PartCategory partCategory, boolean matches) {
    var parents = mock(DemandPublishedRequestedRepository.class);
    var demands = mock(DemandRepository.class);
    var users = mock(UserRepository.class);
    var mapper = mock(SellerMapper.class);
    EventProducer<DemandPublishedNotificationRequested> producer = mock(EventProducer.class);
    var demand =
        JDemand.builder()
            .id("demand")
            .part(JPart.builder().partCategory(partCategory).build())
            .build();
    var seller =
        Seller.builder().id("seller").handleAllCategory(handleAll).categoryList(categories).build();
    var persistedSeller = JSeller.builder().id("seller").build();
    when(demands.findByIdWithRelations("demand")).thenReturn(Optional.of(demand));
    when(parents.save(any())).thenAnswer(i -> i.getArgument(0));
    when(users.findAllByUserTypeAndStatus(SELLER, ENABLED)).thenReturn(List.of(persistedSeller));
    when(mapper.toSeller(persistedSeller)).thenReturn(seller);
    var service =
        new DemandPublishedRequestedService(
            parents, demands, mock(OfferRepository.class), users, mapper, producer);

    TransactionSynchronizationManager.initSynchronization();
    try {
      service.accept(DemandPublishedRequested.builder().id("parent").demandId("demand").build());
      verifyNoInteractions(producer);
      TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
      if (matches) {
        ArgumentCaptor<Collection<DemandPublishedNotificationRequested>> events =
            ArgumentCaptor.forClass(Collection.class);
        verify(producer).accept(events.capture());
        assertEquals(1, events.getValue().size());
        var child = events.getValue().iterator().next();
        assertEquals("seller", child.getSellerId());
        assertEquals("parent", child.getDemandPublishedRequestedId());
        assertEquals("demand", child.getDemandId());
        assertNull(child.getOfferId());
      } else {
        verifyNoInteractions(producer);
      }
      var logs = ArgumentCaptor.forClass(JDemandPublishedRequested.class);
      verify(parents, atLeastOnce()).save(logs.capture());
      var finalLog = logs.getValue();
      assertEquals(ProcessStatus.SUCCESS, finalLog.getStatus());
      assertEquals(matches ? 1 : 0, finalLog.getTotalSellersToNotify());
      assertEquals(matches ? 1 : 0, finalLog.getNotificationsSentCount());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
