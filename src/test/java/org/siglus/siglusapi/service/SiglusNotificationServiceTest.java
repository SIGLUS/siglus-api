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
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApproveRequisitionDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ProofOfDeliveryFulfillmentService;
import org.siglus.common.domain.referencedata.Facility;
import org.siglus.common.domain.referencedata.User;
import org.siglus.common.repository.FacilityRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.repository.SiglusRightAssignmentRepository;
import org.siglus.siglusapi.service.SiglusNotificationService.ViewableStatus;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
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
  private SiglusRightAssignmentRepository rightAssignRepo;

  @Mock
  private SiglusRequisitionRequisitionService requisitionService;

  @Mock
  private ProofOfDeliveryFulfillmentService podService;

  @Mock
  private OrderFulfillmentService orderService;

  @Mock
  private FacilityRepository facilityRepo;

  private UUID notificationId;

  private UUID currentUserHomeFacilityId = randomUUID();

  private String currentUserHomeFacilityName = random(NOT_LONG);

  private BasicRequisitionDto requisition;

  private UUID supervisoryNodeId;

  private UUID userId;

  @Test
  public void shouldCallRepoAndMapperWhenSearchNotifications() {
    // given
    Pageable pageable = new PageRequest(nextInt(), nextInt());
    Notification notification1 = new Notification();
    Notification notification2 = new Notification();
    Notification notification3 = new Notification();
    when(authenticationHelper.getCurrentUserDomain()).thenReturn(new User());
    when(repo.findViewable(eq(pageable), any(), any()))
        .thenReturn(new PageImpl<>(asList(notification1, notification2, notification3)));

    // when
    service.searchNotifications(pageable);

    // then
    verify(repo).findViewable(eq(pageable), any(), any());
    ArgumentCaptor<Notification> argument = ArgumentCaptor.forClass(Notification.class);
    verify(mapper, times(3)).from(argument.capture());
    assertThat(argument.getAllValues(), hasItems(notification1, notification2, notification3));
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
  public void shouldReturnViewedWhenViewNotificationGivenViewedAndProcessedNotification() {
    // given
    mockNotification(true, true);

    // when
    ViewableStatus status = service.viewNotification(notificationId);

    // then
    assertEquals(ViewableStatus.VIEWED, status);
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
    mockSubmitAndAuthorizeInOneStep();
    requisition = new BasicRequisitionDto();
    MinimalFacilityDto facility = new MinimalFacilityDto();
    requisition.setFacility(facility);
    BasicProgramDto program = new BasicProgramDto();
    requisition.setProgram(program);

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
    assertEquals(requisition.getFacility().getId(), notification.getRefFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(NotificationStatus.SUBMITTED, notification.getRefStatus());
    assertEquals(requisition.getFacility().getId(), notification.getNotifyFacilityId());
    assertNull(notification.getSupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostAuthorizeGivenCanBeInternalApproved() {
    // given
    mockAuthentication();
    mockCanBeInternalApproved();
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
    assertEquals(requisition.getFacility().getId(), notification.getRefFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(NotificationStatus.AUTHORIZED, notification.getRefStatus());
    assertEquals(supervisoryNodeId, notification.getSupervisoryNodeId());
    assertNull(notification.getNotifyFacilityId());
  }

  @Test
  public void shouldCallRepoWhenPostAuthorizeGivenCanNotBeInternalApproved() {
    // given
    mockAuthentication();
    mockCanNotBeInternalApproved();
    mockBasicRequisition();
    mockSupervisorNode();

    // when
    service.postAuthorize(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.SUBMITTED,
            NotificationStatus.REJECTED);
    Notification notification = verifySavedNotification();
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(requisition.getFacility().getId(), notification.getRefFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(NotificationStatus.AUTHORIZED, notification.getRefStatus());
    assertEquals(supervisoryNodeId, notification.getSupervisoryNodeId());
    assertNull(notification.getNotifyFacilityId());
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
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
            NotificationStatus.IN_APPROVAL);
    Notification notification = verifySavedNotification();
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(currentUserHomeFacilityId, notification.getRefFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(NotificationStatus.APPROVED, notification.getRefStatus());
    assertEquals(currentUserHomeFacilityId, notification.getNotifyFacilityId());
    assertNull(notification.getSupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostApproveGivenNotFinalApproved() {
    // given
    mockAuthentication();
    mockBasicRequisition();
    mockSupervisorNode();
    mockNotFinalApproved();

    // when
    service.postApprove(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
            NotificationStatus.IN_APPROVAL);
    Notification notification = verifySavedNotification();
    assertEquals(requisition.getId(), notification.getRefId());
    assertEquals(requisition.getFacility().getId(), notification.getRefFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(NotificationStatus.IN_APPROVAL, notification.getRefStatus());
    assertEquals(supervisoryNodeId, notification.getSupervisoryNodeId());
    assertNull(notification.getNotifyFacilityId());
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
    assertEquals(requisition.getFacility().getId(), notification.getRefFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(NotificationStatus.REJECTED, notification.getRefStatus());
    assertEquals(requisition.getFacility().getId(), notification.getNotifyFacilityId());
    assertNull(notification.getSupervisoryNodeId());
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
    OrderDto order = new OrderDto();
    order.setExternalId(requisition.getId());
    ProgramDto program = new ProgramDto();
    program.setId(randomUUID());
    order.setProgram(program);
    when(orderService.search(any(), any(), any(), any(), any()))
        .thenReturn(singletonList(order));
    // when
    service.postConvertToOrder(requisition);

    // then
    verify(repo)
        .updateLastNotificationProcessed(requisition.getId(), NotificationStatus.APPROVED);
    Notification notification = verifySavedNotification();
    assertEquals(order.getId(), notification.getRefId());
    assertEquals(currentUserHomeFacilityId, notification.getRefFacilityId());
    assertEquals(order.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(NotificationStatus.ORDERED, notification.getRefStatus());
    assertEquals(currentUserHomeFacilityId, notification.getNotifyFacilityId());
    assertNull(notification.getSupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostConfirmShipment() {
    // given
    mockAuthentication();
    ShipmentDto shipment = new ShipmentDto();
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(randomUUID());
    shipment.setOrder(orderDto);
    OrderDto order = new OrderDto();
    order.setId(orderDto.getId());
    order.setExternalId(randomUUID());
    when(orderService.findOne(order.getId())).thenReturn(order);

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
    Notification notification = verifySavedNotification();
    assertEquals(pod.getId(), notification.getRefId());
    assertEquals(requisition.getFacility().getId(), notification.getRefFacilityId());
    assertEquals(requisition.getProgram().getId(), notification.getRefProgramId());
    assertEquals(requisition.getEmergency(), notification.getEmergency());
    assertEquals(currentUserHomeFacilityName, notification.getSourceFacilityName());
    assertEquals(NotificationStatus.SHIPPED, notification.getRefStatus());
    assertNull(notification.getNotifyFacilityId());
    assertNull(notification.getSupervisoryNodeId());
  }

  @Test
  public void shouldCallRepoWhenPostConfirmPod() {
    // given
    mockAuthentication();
    org.openlmis.fulfillment.web.util.ProofOfDeliveryDto pod =
        new org.openlmis.fulfillment.web.util.ProofOfDeliveryDto();
    pod.setId(randomUUID());
    // when
    service.postConfirmPod(pod);

    // then
    verify(repo)
        .updateLastNotificationProcessed(pod.getId(), NotificationStatus.SHIPPED);
  }

  private void mockFinalApproved() {
    requisition.setStatus(RequisitionStatus.APPROVED);
  }

  private void mockNotFinalApproved() {
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
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
  }

  private void mockSupervisorNode() {
    supervisoryNodeId = randomUUID();
    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    requisitionV2Dto.setSupervisoryNode(supervisoryNodeId);
    when(requisitionService.searchRequisition(requisition.getId())).thenReturn(requisitionV2Dto);
  }

  private void mockAuthentication() {
    userId = randomUUID();
    User user = new User();
    user.setId(userId);
    user.setHomeFacilityId(currentUserHomeFacilityId);
    when(authenticationHelper.getCurrentUserDomain()).thenReturn(user);
    Facility facility = new Facility();
    facility.setId(currentUserHomeFacilityId);
    facility.setName(currentUserHomeFacilityName);
    when(facilityRepo.findOne(currentUserHomeFacilityId)).thenReturn(facility);
  }

  private void mockCanBeInternalApproved() {
    when(rightAssignRepo.count(any())).thenReturn(1L);
  }

  private void mockCanNotBeInternalApproved() {
    when(rightAssignRepo.count(any())).thenReturn(0L);
  }

  private void mockSubmitAndAuthorizeInOneStep() {
    when(rightAssignRepo.count(any())).thenReturn(1L);
  }

  private void mockSubmitAndAuthorizeInTwoStep() {
    when(rightAssignRepo.count(any())).thenReturn(0L);
  }

  private Notification verifySavedNotification() {
    ArgumentCaptor<Notification> arg = ArgumentCaptor.forClass(Notification.class);
    verify(repo).save(arg.capture());
    return arg.getValue();
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
