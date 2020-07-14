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

package org.siglus.siglusapi.service.mapper;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.ShipmentDto;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ProofOfDeliveryFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ShipmentFulfillmentService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.siglus.common.domain.referencedata.User;
import org.siglus.common.repository.UserRepository;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.impl.NotificationMapperImpl;

@RunWith(MockitoJUnitRunner.class)
public class NotificationMapperImplTest {

  private static final int NOT_TOO_LONG = 10;

  @InjectMocks
  private NotificationMapperImpl mapper;

  @Mock
  private SiglusRequisitionRequisitionService requisitionService;

  @Mock
  private FacilityReferenceDataService facilityService;

  @Mock
  private OrderFulfillmentService orderService;

  @Mock
  private ShipmentFulfillmentService shipmentService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProofOfDeliveryFulfillmentService podService;

  private Notification notification;

  private String sourceFacilityName;

  @Test
  public void shouldCallRepoWhenSearchNotifications() {
    // given
    notification = new Notification();
    notification.setId(randomUUID());
    notification.setRefId(randomUUID());
    notification.setRefStatus(NotificationStatus.IN_APPROVAL);
    notification.setOperatorId(randomUUID());
    boolean isEmergency = nextBoolean();
    RequisitionV2Dto requisition = new RequisitionV2Dto();
    requisition.setEmergency(isEmergency);
    when(requisitionService.searchRequisition(notification.getRefId())).thenReturn(requisition);
    mockerUserAndFacility();

    // when
    NotificationDto notificationDto = mapper.from(notification);

    // then
    assertEquals(notification.getId(), notificationDto.getId());
    assertEquals(isEmergency, notificationDto.getEmergencyFlag());
    assertEquals(sourceFacilityName, notificationDto.getSourceFacilityName());
    assertEquals(notification.getRefId(), notificationDto.getRefId());
    assertEquals(notification.getRefStatus(), notificationDto.getStatus());
  }

  @Test
  public void shouldCallRepoWhenSearchNotificationsGivenOrdered() {
    // given
    notification = new Notification();
    notification.setId(randomUUID());
    notification.setRefId(randomUUID());
    notification.setRefStatus(NotificationStatus.ORDERED);
    OrderDto order = new OrderDto();
    order.setExternalId(randomUUID());
    when(orderService.findOne(notification.getRefId())).thenReturn(order);
    boolean isEmergency = nextBoolean();
    RequisitionV2Dto requisition = new RequisitionV2Dto();
    requisition.setEmergency(isEmergency);
    when(requisitionService.searchRequisition(order.getExternalId())).thenReturn(requisition);
    mockerUserAndFacility();

    // when
    NotificationDto notificationDto = mapper.from(notification);

    // then
    assertEquals(notification.getId(), notificationDto.getId());
    assertEquals(isEmergency, notificationDto.getEmergencyFlag());
    assertEquals(sourceFacilityName, notificationDto.getSourceFacilityName());
    assertEquals(notification.getRefId(), notificationDto.getRefId());
    assertEquals(notification.getRefStatus(), notificationDto.getStatus());
  }

  @Test
  public void shouldCallRepoWhenSearchNotificationsGivenShipped() {
    // given
    notification = new Notification();
    notification.setId(randomUUID());
    notification.setRefId(randomUUID());
    notification.setRefStatus(NotificationStatus.SHIPPED);
    ProofOfDeliveryDto pod = new ProofOfDeliveryDto();
    pod.setId(notification.getRefId());
    ObjectReferenceDto shipmentDto = new ObjectReferenceDto();
    shipmentDto.setId(randomUUID());
    pod.setShipment(shipmentDto);
    when(podService.findOne(pod.getId())).thenReturn(pod);
    ShipmentDto shipment = new ShipmentDto();
    ObjectReferenceDto order = new ObjectReferenceDto();
    order.setId(randomUUID());
    shipment.setOrder(order);
    when(shipmentService.findOne(shipmentDto.getId())).thenReturn(shipment);
    OrderDto orderDto = new OrderDto();
    orderDto.setExternalId(randomUUID());
    when(orderService.findOne(order.getId())).thenReturn(orderDto);
    boolean isEmergency = nextBoolean();
    RequisitionV2Dto requisition = new RequisitionV2Dto();
    requisition.setEmergency(isEmergency);
    when(requisitionService.searchRequisition(orderDto.getExternalId())).thenReturn(requisition);
    mockerUserAndFacility();

    // when
    NotificationDto notificationDto = mapper.from(notification);

    // then
    assertEquals(notification.getId(), notificationDto.getId());
    assertEquals(isEmergency, notificationDto.getEmergencyFlag());
    assertEquals(sourceFacilityName, notificationDto.getSourceFacilityName());
    assertEquals(notification.getRefId(), notificationDto.getRefId());
    assertEquals(notification.getRefStatus(), notificationDto.getStatus());
  }

  private void mockerUserAndFacility() {
    User user = new User();
    user.setHomeFacilityId(randomUUID());
    when(userRepository.findOne(notification.getOperatorId())).thenReturn(user);
    sourceFacilityName = random(NOT_TOO_LONG);
    FacilityDto facility = new FacilityDto();
    facility.setName(sourceFacilityName);
    when(facilityService.findOne(user.getHomeFacilityId())).thenReturn(facility);
  }

}
