/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.service.referencedata.PeriodReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.openlmis.requisition.domain.StatusLogEntry;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApproveRequisitionDto;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.fulfillment.ProofOfDeliveryFulfillmentService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.RequisitionGroupReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.PermissionString;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.domain.NotificationType;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.service.SiglusNotificationService.ViewableStatus;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusNotificationServiceTest {

  private static final int NOT_LONG = 5;

  @InjectMocks
  private SiglusNotificationService service;

  @Mock
  private NotificationMapper mapper;

  @Mock
  private NotificationRepository repo;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusRequisitionRequisitionService requisitionService;

  @Mock
  private ProofOfDeliveryFulfillmentService podService;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private RequisitionGroupReferenceDataService requisitionGroupService;

  @Mock
  private SiglusOrderService siglusOrderService;

  @Mock
  private PeriodReferenceDataService periodService;

  @Mock
  private RequisitionController requisitionController;

  @Mock
  private ProgramReferenceDataService programRefDataService;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  private UUID notificationId;

  private final UUID currentUserHomeFacilityId = randomUUID();

  private final String currentUserHomeFacilityName = random(NOT_LONG);

  private BasicRequisitionDto requisition;

  private UUID supervisoryNodeId;

  private UUID userId;

  @Test
  public void shouldCallRepoAndMapperWhenSearchNotifications() {
    // given
    UserDto user = new UserDto();
    user.setRoleAssignments(emptySet());
    RequisitionV2Dto requisition = new RequisitionV2Dto();
    requisition.getStatusChanges().put(RequisitionStatus.SUBMITTED.name(), new StatusLogEntry());
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    when(requisitionGroupService.findAll()).thenReturn(emptyList());
    when(periodService.findOne(any())).thenReturn(new ProcessingPeriodDto());
    when(requisitionService.searchRequisition(any())).thenReturn(requisition);
    when(facilityReferenceDataService.findOne((UUID) any())).thenReturn(new FacilityDto());
    when(programRefDataService.findOne(any()))
        .thenReturn(new org.openlmis.requisition.dto.ProgramDto());
    when(userReferenceDataService.findOne(any()))
        .thenReturn(new org.openlmis.requisition.dto.UserDto());
    Notification notification1 = new Notification();
    notification1.setProcessingPeriodId(randomUUID());
    notification1.setStatus(NotificationStatus.APPROVED);
    Notification notification2 = new Notification();
    notification2.setStatus(NotificationStatus.ORDERED);
    Notification notification3 = new Notification();
    notification3.setStatus(NotificationStatus.RECEIVED);
    Pageable pageable = new PageRequest(nextInt(), nextInt());
    when(repo.findViewable(eq(pageable), any(), any()))
        .thenReturn(new PageImpl<>(asList(notification1, notification2, notification3)));

    // when
    service.searchNotifications(pageable, NotificationType.TODO);

    // then
    verify(repo).findViewable(eq(pageable), any(), any());
    ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
    ArgumentCaptor<FacilityDto> facility = ArgumentCaptor.forClass(FacilityDto.class);
    ArgumentCaptor<org.openlmis.requisition.dto.ProgramDto> program = ArgumentCaptor.forClass(
        org.openlmis.requisition.dto.ProgramDto.class);
    ArgumentCaptor<ProcessingPeriodDto> period = ArgumentCaptor.forClass(ProcessingPeriodDto.class);
    ArgumentCaptor<ZonedDateTime> time = ArgumentCaptor.forClass(ZonedDateTime.class);
    ArgumentCaptor<String> author = ArgumentCaptor.forClass(String.class);
    verify(mapper, times(3)).from(notification.capture(), facility.capture(),
        program.capture(), period.capture(), time.capture(), author.capture());
    assertThat(notification.getAllValues(), hasItems(notification1, notification2, notification3));
  }

  @Test
  public void shouldSetViewedTrueWhenViewNotificationGivenNotViewedAndNotProcessedNotification() {
    // given
    mockNotification(false, false);
    mockAuthentication();

    // when
    service.viewNotification(notificationId);

    // then
    ArgumentCaptor<Notification> argument = ArgumentCaptor.forClass(Notification.class);
    verify(repo).save(argument.capture());
    Notification notification = argument.getValue();
    assertTrue(notification.getViewed());
    assertEquals(userId, notification.getViewedUserId());
    assertTrue(
        Duration.between(LocalDateTime.now(), notification.getViewedDate())
            .compareTo(Duration.ofSeconds(1)) < 0);
  }

  @Test
  public void shouldReturnNotViewedWhenViewNotificationGivenNotViewedAndNotProcessedNotification() {
    // given
    mockNotification(false, false);
    mockAuthentication();

    // when
    ViewableStatus status = service.viewNotification(notificationId);

    // then
    assertEquals(ViewableStatus.NOT_VIEWED, status);
  }

  @Test
  public void shouldReturnProcessedWhenViewNotificationGivenViewedAndProcessedNotification() {
    // given
    mockNotification(true, true);

    // when
    ViewableStatus status = service.viewNotification(notificationId);

    // then
    assertEquals(ViewableStatus.PROCESSED, status);
  }

  @Test
  public void shouldReturnViewedWhenViewNotificationGivenViewedAndNotProcessedNotification() {
    // given
    mockNotification(true, false);
    mockAuthentication();

    // when
    ViewableStatus status = service.viewNotification(notificationId);

    // then
    assertEquals(ViewableStatus.VIEWED, status);
  }

  @Test
  public void shouldReturnProcessedWhenViewNotificationGivenNotViewedAndProcessedNotification() {
    // given
    mockNotification(false, true);
    mockAuthentication();

    // when
    ViewableStatus status = service.viewNotification(notificationId);

    // then
    assertEquals(ViewableStatus.PROCESSED, status);
  }

  @Test
  public void shouldNotCallRepoWhenPostSubmitGivenSubmitAndAuthorizeInOneStep() {
    // given
    mockAuthentication();
    requisition = new BasicRequisitionDto();
    MinimalFacilityDto facility = new MinimalFacilityDto();
    facility.setId(randomUUID());
    requisition.setFacility(facility);
    BasicProgramDto program = new BasicProgramDto();
    program.setId(randomUUID());
    requisition.setProgram(program);
    mockSubmitAndAuthorizeInOneStep();

    // when
    service.postSubmit(requisition);

    // then
    verify(repo, never()).save(any(Notification.class));
  }

  @Test
  public void shouldCallRepoWhenPostSubmitGivenSubmitAndAuthorizeInTwoStep() {
    // given
    mockAuthentication();
    mockSubmitAndAuthorizeInTwoStep();
    mockBasicRequisition();

    // when
    service.postSubmit(requisition);

    // then
    Notification notification = verifySavedNotification();
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(requisition.getFacility().getId(), notification.getFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(NotificationStatus.SUBMITTED, notification.getStatus());
    assertNull(notification.getNotifySupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostAuthorize() {
    // given
    mockAuthentication();
    mockBasicRequisition();
    mockSupervisorNode();

    // when
    service.postAuthorize(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(),
            NotificationStatus.SUBMITTED, NotificationStatus.REJECTED);
    Notification notification = verifySavedNotification();
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(requisition.getFacility().getId(), notification.getFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(NotificationStatus.AUTHORIZED, notification.getStatus());
    assertEquals(supervisoryNodeId, notification.getNotifySupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostApproveGivenFinalApproved() {
    // given
    mockAuthentication();
    mockBasicRequisition();
    mockFinalApproved();

    // when
    service.postApprove(requisition);

    // then
    verify(repo).updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
        NotificationStatus.IN_APPROVAL);
    List<Notification> notification = verifySavedNotification(2);
    Notification todoNotification = notification.get(0);
    assertEquals(requisition.getId(), todoNotification.getRefId());
    assertEquals(currentUserHomeFacilityId, todoNotification.getFacilityId());
    assertEquals(requisition.getProgram().getId(), todoNotification.getProgramId());
    assertEquals(requisition.getEmergency(), todoNotification.getEmergency());
    assertEquals(NotificationStatus.APPROVED, todoNotification.getStatus());
    assertNull(todoNotification.getNotifySupervisoryNodeId());
    assertEquals(NotificationType.TODO, todoNotification.getType());

    Notification updateNotification = notification.get(1);
    assertEquals(requisition.getId(), updateNotification.getRefId());
    assertEquals(requisition.getFacility().getId(), updateNotification.getFacilityId());
    assertEquals(requisition.getProgram().getId(), updateNotification.getProgramId());
    assertEquals(requisition.getEmergency(), updateNotification.getEmergency());
    assertEquals(NotificationStatus.APPROVED, updateNotification.getStatus());
    assertNull(updateNotification.getNotifySupervisoryNodeId());
    assertEquals(NotificationType.UPDATE, updateNotification.getType());
  }

  @Test
  public void shouldCallRepoWhenPostApproveGivenInApproval() {
    // given
    mockAuthentication();
    mockBasicRequisition();
    mockSupervisorNode();
    mockInApproval();

    // when
    service.postApprove(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
            NotificationStatus.IN_APPROVAL);
    Notification notification = verifySavedNotification();
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(requisition.getFacility().getId(), notification.getFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(NotificationStatus.IN_APPROVAL, notification.getStatus());
    assertEquals(supervisoryNodeId, notification.getNotifySupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostApproveGivenReleaseWithoutOrder() {
    // given
    mockAuthentication();
    mockBasicRequisition();
    mockSupervisorNode();
    mockReleasedWithoutOrder();

    // when
    service.postApprove(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
            NotificationStatus.IN_APPROVAL);
    verify(repo).save(any(Notification.class));
  }

  @Test
  public void shouldCallRepoWhenPostRejectGivenNotFinalApproved() {
    // given
    mockAuthentication();
    mockBasicRequisition();

    // when
    service.postReject(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
            NotificationStatus.IN_APPROVAL);
    Notification notification = verifySavedNotification();
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(requisition.getFacility().getId(), notification.getFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(NotificationStatus.REJECTED, notification.getStatus());
    assertNull(notification.getNotifySupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostConvertToOrder() {
    // given
    mockAuthentication();
    ApproveRequisitionDto requisition = new ApproveRequisitionDto();
    requisition.setId(randomUUID());
    requisition.setEmergency(nextBoolean());
    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    requisitionV2Dto.setId(requisition.getId());
    when(requisitionService.searchRequisition(requisition.getId()))
        .thenReturn(requisitionV2Dto);
    BasicOrderDto order = new BasicOrderDto();
    order.setExternalId(requisition.getId());
    ProgramDto program = new ProgramDto();
    program.setId(randomUUID());
    order.setProgram(program);
    ProcessingPeriodDto period = new ProcessingPeriodDto();
    period.setId(randomUUID());
    order.setProcessingPeriod(period);
    org.openlmis.fulfillment.service.referencedata.FacilityDto facility =
        new org.openlmis.fulfillment.service.referencedata.FacilityDto();
    order.setRequestingFacility(facility);
    when(siglusOrderService.searchOrders(any(), any())).thenReturn(Pagination.getPage(singletonList(order)));

    // when
    service.postConvertToOrder(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.APPROVED);
    Notification notification = verifySavedNotification();
    assertEquals(order.getId(), notification.getRefId());
    assertEquals(currentUserHomeFacilityId, notification.getFacilityId());
    assertEquals(order.getProgram().getId(), notification.getProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(NotificationStatus.ORDERED, notification.getStatus());
    assertNull(notification.getNotifySupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostConfirmShipment() {
    // given
    when(orderExternalRepository.findOne(any(UUID.class)))
        .thenReturn(null);
    mockAuthentication();
    ShipmentDto shipment = new ShipmentDto();
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(randomUUID());
    shipment.setOrder(orderDto);
    org.openlmis.fulfillment.web.util.OrderDto order =
        new org.openlmis.fulfillment.web.util.OrderDto();
    order.setId(orderDto.getId());
    order.setExternalId(randomUUID());
    ProcessingPeriodDto period = new ProcessingPeriodDto();
    period.setId(randomUUID());
    order.setProcessingPeriod(period);
    SiglusOrderDto siglusOrderDto = new SiglusOrderDto();
    siglusOrderDto.setOrder(order);
    when(siglusOrderService.searchOrderById(order.getId())).thenReturn(siglusOrderDto);

    ProofOfDeliveryDto pod = new ProofOfDeliveryDto();
    pod.setId(randomUUID());
    when(podService.getProofOfDeliveries(order.getId())).thenReturn(singletonList(pod));

    RequisitionV2Dto requisition = new RequisitionV2Dto();
    requisition.setId(randomUUID());
    requisition.setEmergency(nextBoolean());
    ObjectReferenceDto facility = new ObjectReferenceDto();
    facility.setId(randomUUID());
    requisition.setFacility(facility);
    ObjectReferenceDto program = new ObjectReferenceDto();
    program.setId(randomUUID());
    requisition.setProgram(program);
    when(requisitionService.searchRequisition(order.getExternalId())).thenReturn(requisition);

    // when
    service.postConfirmShipment(shipment);

    // then
    verify(repo)
        .updateLastNotificationProcessed(order.getId(), NotificationStatus.ORDERED);
    List<Notification> notification = verifySavedNotification(2);
    Notification notification1 = notification.get(0);
    assertEquals(pod.getId(), notification1.getRefId());
    assertEquals(requisition.getFacility().getId(), notification1.getFacilityId());
    assertEquals(requisition.getEmergency(), notification1.getEmergency());
    assertEquals(NotificationStatus.SHIPPED, notification1.getStatus());
    assertEquals(order.getProcessingPeriod().getId(), notification1.getProcessingPeriodId());
    assertNull(notification1.getNotifySupervisoryNodeId());
    assertEquals(NotificationType.TODO, notification1.getType());
    assertEquals(requisition.getProgram().getId(), notification1.getProgramId());
    Notification notification2 = notification.get(1);
    assertEquals(NotificationType.UPDATE, notification2.getType());
  }

  @Test
  public void shouldCallRepoWhenPostConfirmPod() {
    // given
    mockAuthentication();
    org.openlmis.fulfillment.web.util.ProofOfDeliveryDto pod =
        new org.openlmis.fulfillment.web.util.ProofOfDeliveryDto();
    pod.setId(randomUUID());
    OrderObjectReferenceDto order = new OrderObjectReferenceDto(randomUUID());
    org.openlmis.fulfillment.service.referencedata.FacilityDto facility =
        new org.openlmis.fulfillment.service.referencedata.FacilityDto();
    facility.setId(randomUUID());
    order.setSupplyingFacility(facility);
    order.setRequestingFacility(facility);
    ProcessingPeriodDto period = new ProcessingPeriodDto();
    period.setId(randomUUID());
    order.setProcessingPeriod(period);
    ProgramDto program = new ProgramDto();
    program.setId(randomUUID());
    order.setProgram(program);
    order.setEmergency(nextBoolean());
    ShipmentObjectReferenceDto shipment = new ShipmentObjectReferenceDto(randomUUID());
    shipment.setOrder(order);
    pod.setShipment(shipment);
    // when
    service.postConfirmPod(pod);

    // then
    verify(repo)
        .updateLastNotificationProcessed(pod.getId(), NotificationStatus.SHIPPED);
  }

  private void mockFinalApproved() {
    requisition.setStatus(RequisitionStatus.APPROVED);
  }

  private void mockInApproval() {
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
  }

  private void mockReleasedWithoutOrder() {
    requisition.setStatus(RequisitionStatus.RELEASED_WITHOUT_ORDER);
  }

  private void mockBasicRequisition() {
    requisition = new BasicRequisitionDto();
    requisition.setId(randomUUID());
    requisition.setEmergency(nextBoolean());
    MinimalFacilityDto facility = new MinimalFacilityDto();
    facility.setId(randomUUID());
    requisition.setFacility(facility);
    BasicProgramDto program = new BasicProgramDto();
    program.setId(randomUUID());
    requisition.setProgram(program);
    BasicProcessingPeriodDto basicPeriod = new BasicProcessingPeriodDto();
    basicPeriod.setId(randomUUID());
    requisition.setProcessingPeriod(basicPeriod);
  }

  private void mockSupervisorNode() {
    Requisition requisition = new Requisition();
    requisition.setSupervisoryNodeId(supervisoryNodeId);
    Profiler profiler = new Profiler("GET_REQUISITION");
    when(requisitionController.getProfiler(any(), any())).thenReturn(profiler);
    when(requisitionController.findRequisition(any(), any())).thenReturn(requisition);
  }

  private void mockAuthentication() {
    userId = randomUUID();
    UserDto user = new UserDto();
    user.setId(userId);
    user.setHomeFacilityId(currentUserHomeFacilityId);
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(userId));
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    FacilityDto facility = new FacilityDto();
    facility.setId(currentUserHomeFacilityId);
    facility.setName(currentUserHomeFacilityName);
    when(facilityReferenceDataService.findOne(currentUserHomeFacilityId)).thenReturn(facility);
  }

  private void mockSubmitAndAuthorizeInOneStep() {
    PermissionString permissionString = new PermissionString(
        PermissionService.REQUISITION_AUTHORIZE + "|" + requisition.getFacility().getId()
            + "|" + requisition.getProgram().getId());
    when(authenticationHelper.getCurrentUserPermissionStrings())
        .thenReturn(singletonList(permissionString));
  }

  private void mockSubmitAndAuthorizeInTwoStep() {
    when(authenticationHelper.getCurrentUserPermissionStrings()).thenReturn(emptyList());
  }

  private Notification verifySavedNotification() {
    ArgumentCaptor<Notification> arg = ArgumentCaptor.forClass(Notification.class);
    verify(repo).save(arg.capture());
    return arg.getValue();
  }

  private List<Notification> verifySavedNotification(Integer times) {
    ArgumentCaptor<Notification> arg = ArgumentCaptor.forClass(Notification.class);
    verify(repo, times(times)).save(arg.capture());
    return arg.getAllValues();
  }

  private void mockNotification(boolean viewed, boolean processed) {
    notificationId = randomUUID();
    Notification notification = new Notification();
    notification.setId(notificationId);
    notification.setViewed(viewed);
    notification.setProcessed(processed);
    when(repo.findOne(notificationId)).thenReturn(notification);
  }

}
