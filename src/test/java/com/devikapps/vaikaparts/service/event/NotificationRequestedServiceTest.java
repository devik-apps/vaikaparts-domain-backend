package com.devikapps.vaikaparts.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.NotificationRequested;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.event.JNotificationRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRequestedServiceTest {

  private static final String TEST_EVENT_ID = "event-123";
  private static final String TEST_PARENT_ID = "parent-456";
  private static final String TEST_SELLER_ID = "seller-789";
  private static final String TEST_DEMAND_ID = "demand-012";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;

  @Mock private NotificationRequestedRepository notificationRequestedRepository;
  @Mock private DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  @Mock private DemandRepository demandRepository;
  @Mock private UserRepository userRepository;
  @Mock private SellerMapper sellerMapper;
  @Mock private NotificationService notificationService;

  @InjectMocks private NotificationRequestedService service;

  private NotificationRequested testEvent;
  private JNotificationRequested testEventLog;
  private JDemandPublishedRequested testParent;
  private JDemand testDemand;
  private JUser testSeller;

  @BeforeEach
  void setUp() {
    testEvent =
        NotificationRequested.builder()
            .id(TEST_EVENT_ID)
            .demandPublishedRequestedId(TEST_PARENT_ID)
            .sellerId(TEST_SELLER_ID)
            .demandId(TEST_DEMAND_ID)
            .build();

    testSeller = JSeller.builder().id(TEST_SELLER_ID).name("Test Seller").build();

    var testPart =
        JPart.builder()
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .build();

    testDemand = JDemand.builder().id(TEST_DEMAND_ID).part(testPart).build();

    testParent = JDemandPublishedRequested.builder().id(TEST_PARENT_ID).demand(testDemand).build();

    testEventLog =
        JNotificationRequested.builder()
            .id(TEST_EVENT_ID)
            .demandPublishedRequested(testParent)
            .seller(JSeller.builder().id(TEST_SELLER_ID).build())
            .demand(testDemand)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .status(ProcessStatus.PENDING)
            .attemptNb(0)
            .build();
  }

  @Test
  void should_create_event_log_if_not_exists() {
    when(notificationRequestedRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.empty());
    when(demandPublishedRequestedRepository.findById(TEST_PARENT_ID))
        .thenReturn(Optional.of(testParent));
    when(userRepository.findJUserById(TEST_SELLER_ID)).thenReturn(Optional.of(testSeller));
    when(demandRepository.findById(TEST_DEMAND_ID)).thenReturn(Optional.of(testDemand));
    when(notificationRequestedRepository.save(any(JNotificationRequested.class)))
        .thenReturn(testEventLog);
    when(notificationService.createAndSendNotification(any(NotificationRequest.class)))
        .thenReturn(mock(Notification.class));

    service.accept(testEvent);

    ArgumentCaptor<JNotificationRequested> captor =
        ArgumentCaptor.forClass(JNotificationRequested.class);
    verify(notificationRequestedRepository, atLeastOnce()).save(captor.capture());

    var createdLog = captor.getAllValues().getFirst();
    assertEquals(TEST_EVENT_ID, createdLog.getId());
    assertEquals(testParent, createdLog.getDemandPublishedRequested());
    assertEquals(testDemand, createdLog.getDemand());
    assertEquals(NotificationType.DEMAND_PUBLISHED, createdLog.getNotificationType());
    assertEquals(ProcessStatus.SUCCESS, createdLog.getStatus());
    assertNotNull(createdLog.getCreatedAt());
    assertNotNull(createdLog.getUpdatedAt());
  }

  @Test
  void should_call_notification_service_with_correct_request() {
    when(notificationRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(notificationRequestedRepository.save(any(JNotificationRequested.class)))
        .thenReturn(testEventLog);
    when(notificationService.createAndSendNotification(any(NotificationRequest.class)))
        .thenReturn(mock(Notification.class));

    service.accept(testEvent);

    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(notificationService, times(1)).createAndSendNotification(captor.capture());

    var request = captor.getValue();
    assertEquals(TEST_SELLER_ID, request.getSellerId());
    assertEquals(TEST_DEMAND_ID, request.getDemandId());
    assertEquals(NotificationType.DEMAND_PUBLISHED, request.getNotificationType());
    assertTrue(request.getMessage().contains(TEST_CAR_BRAND));
    assertTrue(request.getMessage().contains(TEST_CAR_MODEL));
    assertTrue(request.getMessage().contains(TEST_PART_NAME));
    assertTrue(request.getMessage().contains(String.valueOf(TEST_CAR_YEAR)));
    assertNotNull(request.getClickAction());
    assertTrue(request.getClickAction().contains(TEST_DEMAND_ID));
  }

  @Test
  void should_update_status_to_success_when_processing_completes() {
    when(notificationRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(notificationRequestedRepository.save(any(JNotificationRequested.class)))
        .thenReturn(testEventLog);
    when(notificationService.createAndSendNotification(any(NotificationRequest.class)))
        .thenReturn(mock(Notification.class));

    service.accept(testEvent);

    ArgumentCaptor<JNotificationRequested> captor =
        ArgumentCaptor.forClass(JNotificationRequested.class);
    verify(notificationRequestedRepository, atLeastOnce()).save(captor.capture());

    var finalLog = captor.getAllValues().getLast();
    assertEquals(ProcessStatus.SUCCESS, finalLog.getStatus());
    assertNotNull(finalLog.getCompletedAt());
    assertNotNull(finalLog.getUpdatedAt());
  }

  @Test
  void should_set_status_to_failed_on_exception() {
    when(notificationRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(notificationRequestedRepository.save(any(JNotificationRequested.class)))
        .thenReturn(testEventLog);
    when(notificationService.createAndSendNotification(any(NotificationRequest.class)))
        .thenThrow(new RuntimeException("Notification failed"));

    assertThrows(RuntimeException.class, () -> service.accept(testEvent));

    ArgumentCaptor<JNotificationRequested> captor =
        ArgumentCaptor.forClass(JNotificationRequested.class);
    verify(notificationRequestedRepository, atLeastOnce()).save(captor.capture());

    var finalLog = captor.getAllValues().getLast();
    assertEquals(ProcessStatus.FAILED, finalLog.getStatus());
    assertNotNull(finalLog.getErrorMessage());
    assertTrue(finalLog.getErrorMessage().contains("Notification failed"));
    assertNotNull(finalLog.getCompletedAt());
    assertEquals(0, finalLog.getAttemptNb());
  }

  @Test
  void should_throw_exception_when_parent_not_found() {
    when(notificationRequestedRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.empty());
    when(demandPublishedRequestedRepository.findById(TEST_PARENT_ID)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> service.accept(testEvent));
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    when(notificationRequestedRepository.findById(TEST_EVENT_ID)).thenReturn(Optional.empty());
    when(demandPublishedRequestedRepository.findById(TEST_PARENT_ID))
        .thenReturn(Optional.of(testParent));
    when(userRepository.findJUserById(TEST_SELLER_ID)).thenReturn(Optional.of(testSeller));

    assertThrows(RuntimeException.class, () -> service.accept(testEvent));
  }

  @Test
  void should_update_existing_event_log_on_retry() {
    testEvent.setAttemptNb(1);
    testEventLog.setStatus(ProcessStatus.FAILED);
    testEventLog.setAttemptNb(0);

    when(notificationRequestedRepository.findById(TEST_EVENT_ID))
        .thenReturn(Optional.of(testEventLog));
    when(notificationRequestedRepository.save(any(JNotificationRequested.class)))
        .thenReturn(testEventLog);
    when(notificationService.createAndSendNotification(any(NotificationRequest.class)))
        .thenReturn(mock(Notification.class));

    service.accept(testEvent);

    ArgumentCaptor<JNotificationRequested> captor =
        ArgumentCaptor.forClass(JNotificationRequested.class);
    verify(notificationRequestedRepository, atLeastOnce()).save(captor.capture());

    var finalLog = captor.getAllValues().getLast();
    assertEquals(ProcessStatus.SUCCESS, finalLog.getStatus());
    assertNotNull(finalLog.getCompletedAt());
  }
}
