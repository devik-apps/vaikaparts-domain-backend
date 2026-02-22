package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.NotificationRequested;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.service.event.DemandPublishedRequestedService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemandPublishedRequestedServiceTest {

  private static final String TEST_EVENT_ID = "event-123";
  private static final String TEST_DEMAND_ID = "demand-456";
  private static final String TEST_SELLER_1_ID = "seller-1";
  private static final String TEST_SELLER_2_ID = "seller-2";

  @Mock private DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  @Mock private DemandRepository demandRepository;
  @Mock private SellerService sellerService;
  @Mock private EventProducer<NotificationRequested> notificationRequestedProducer;

  @InjectMocks private DemandPublishedRequestedService service;

  private DemandPublishedRequested testEvent;
  private JDemand testDemand;
  private Seller testSeller1;
  private Seller testSeller2;
  private JDemandPublishedRequested testEventLog;

  @BeforeEach
  void setUp() {
    testEvent =
        DemandPublishedRequested.builder().id(TEST_EVENT_ID).demandId(TEST_DEMAND_ID).build();

    testDemand = mock(JDemand.class);
    testSeller1 = mock(Seller.class);
    testSeller2 = mock(Seller.class);

    testEventLog =
        JDemandPublishedRequested.builder()
            .id(TEST_EVENT_ID)
            .demand(testDemand)
            .status(ProcessStatus.PENDING)
            .attemptNb(0)
            .totalSellersToNotify(0)
            .notificationsSentCount(0)
            .build();
  }

  @Test
  void should_create_event_log_if_not_exists() {
    when(demandPublishedRequestedRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.empty());
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testDemand));
    when(demandPublishedRequestedRepository.save(any(JDemandPublishedRequested.class)))
        .thenReturn(testEventLog);
    when(sellerService.getAllActiveSellers()).thenReturn(List.of(testSeller1, testSeller2));
    when(testDemand.getId()).thenReturn(TEST_DEMAND_ID);
    when(testSeller1.getId()).thenReturn(TEST_SELLER_1_ID);
    when(testSeller2.getId()).thenReturn(TEST_SELLER_2_ID);

    service.accept(testEvent);

    ArgumentCaptor<JDemandPublishedRequested> captor =
        ArgumentCaptor.forClass(JDemandPublishedRequested.class);
    verify(demandPublishedRequestedRepository, atLeastOnce()).save(captor.capture());

    var savedLog = captor.getValue();
    assertEquals(TEST_EVENT_ID, savedLog.getId());
    assertEquals(testDemand, savedLog.getDemand());
  }

  @Test
  void should_fetch_active_sellers() {
    when(demandPublishedRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testDemand));
    when(demandPublishedRequestedRepository.save(any(JDemandPublishedRequested.class)))
        .thenReturn(testEventLog);
    when(sellerService.getAllActiveSellers()).thenReturn(List.of(testSeller1, testSeller2));

    service.accept(testEvent);

    verify(sellerService, times(1)).getAllActiveSellers();
  }

  @Test
  void should_publish_notification_events_for_each_seller() {
    when(demandPublishedRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(testDemand.getId()).thenReturn(TEST_DEMAND_ID);
    when(testSeller1.getId()).thenReturn(TEST_SELLER_1_ID);
    when(testSeller2.getId()).thenReturn(TEST_SELLER_2_ID);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testDemand));
    when(demandPublishedRequestedRepository.save(any(JDemandPublishedRequested.class)))
        .thenReturn(testEventLog);
    when(sellerService.getAllActiveSellers()).thenReturn(List.of(testSeller1, testSeller2));

    service.accept(testEvent);

    ArgumentCaptor<List<NotificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(notificationRequestedProducer, times(1)).accept(captor.capture());

    var publishedEvents = captor.getValue();
    assertEquals(2, publishedEvents.size());
    assertTrue(publishedEvents.stream().anyMatch(e -> e.getSellerId().equals(TEST_SELLER_1_ID)));
    assertTrue(publishedEvents.stream().anyMatch(e -> e.getSellerId().equals(TEST_SELLER_2_ID)));
    assertTrue(publishedEvents.stream().allMatch(e -> e.getDemandId().equals(TEST_DEMAND_ID)));
    assertTrue(
        publishedEvents.stream()
            .allMatch(e -> e.getDemandPublishedRequestedId().equals(TEST_EVENT_ID)));
  }

  @Test
  void should_update_event_log_status_to_success() {
    when(demandPublishedRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testDemand));
    when(demandPublishedRequestedRepository.save(any(JDemandPublishedRequested.class)))
        .thenReturn(testEventLog);
    when(sellerService.getAllActiveSellers()).thenReturn(List.of(testSeller1));
    when(testDemand.getId()).thenReturn(TEST_DEMAND_ID);
    when(testSeller1.getId()).thenReturn(TEST_SELLER_1_ID);

    service.accept(testEvent);

    ArgumentCaptor<JDemandPublishedRequested> captor =
        ArgumentCaptor.forClass(JDemandPublishedRequested.class);
    verify(demandPublishedRequestedRepository, atLeastOnce()).save(captor.capture());

    var finalLog = captor.getAllValues().getLast();
    assertEquals(ProcessStatus.SUCCESS, finalLog.getStatus());
    assertEquals(1, finalLog.getTotalSellersToNotify());
    assertEquals(1, finalLog.getNotificationsSentCount());
    assertNotNull(finalLog.getCompletedAt());
  }

  @Test
  void should_handle_zero_active_sellers() {
    when(demandPublishedRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testDemand));
    when(demandPublishedRequestedRepository.save(any(JDemandPublishedRequested.class)))
        .thenReturn(testEventLog);
    when(sellerService.getAllActiveSellers()).thenReturn(List.of());

    service.accept(testEvent);

    verify(notificationRequestedProducer, never()).accept(any());

    ArgumentCaptor<JDemandPublishedRequested> captor =
        ArgumentCaptor.forClass(JDemandPublishedRequested.class);
    verify(demandPublishedRequestedRepository, atLeastOnce()).save(captor.capture());

    var finalLog = captor.getAllValues().getLast();
    assertEquals(ProcessStatus.SUCCESS, finalLog.getStatus());
    assertEquals(0, finalLog.getTotalSellersToNotify());
    assertEquals(0, finalLog.getNotificationsSentCount());
  }

  @Test
  void should_set_event_log_to_failed_on_exception() {
    when(demandPublishedRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenThrow(new RuntimeException("Database error"));
    when(demandPublishedRequestedRepository.save(any(JDemandPublishedRequested.class)))
        .thenReturn(testEventLog);

    assertThrows(RuntimeException.class, () -> service.accept(testEvent));

    ArgumentCaptor<JDemandPublishedRequested> captor =
        ArgumentCaptor.forClass(JDemandPublishedRequested.class);
    verify(demandPublishedRequestedRepository, atLeastOnce()).save(captor.capture());

    var finalLog = captor.getAllValues().getLast();
    assertEquals(ProcessStatus.FAILED, finalLog.getStatus());
    assertNotNull(finalLog.getErrorMessage());
    assertNotNull(finalLog.getCompletedAt());
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    when(demandPublishedRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID)).thenReturn(Optional.empty());
    when(demandPublishedRequestedRepository.save(any(JDemandPublishedRequested.class)))
        .thenReturn(testEventLog);

    assertThrows(RuntimeException.class, () -> service.accept(testEvent));
  }
}
