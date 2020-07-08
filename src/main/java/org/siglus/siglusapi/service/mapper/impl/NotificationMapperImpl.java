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

package org.siglus.siglusapi.service.mapper.impl;

import java.util.UUID;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.ShipmentDto;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ShipmentFulfillmentService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapperImpl implements NotificationMapper {

  @Autowired
  private SiglusRequisitionRequisitionService requisitionService;

  @Autowired
  @Qualifier("facilityReferenceDataService")
  private FacilityReferenceDataService facilityService;

  @Autowired
  private OrderFulfillmentService orderService;

  @Autowired
  private ShipmentFulfillmentService shipmentService;

  @Autowired
  private UserRepository userRepository;

  @Override
  public NotificationDto from(Notification notification) {
    if (notification == null) {
      return null;
    }
    NotificationDto dto = new NotificationDto();
    dto.setId(notification.getId());
    setEmergencyAndFacilityName(notification, dto);
    dto.setRefId(notification.getRefId());
    dto.setStatus(notification.getRefStatus());
    return dto;
  }

  private void setEmergencyAndFacilityName(Notification notification, NotificationDto dto) {
    UUID requisitionId;
    if (notification.getRefStatus().isRequisitionPeriod()) {
      requisitionId = notification.getRefId();
    } else if (notification.getRefStatus().isOrderPeriod()) {
      OrderDto order = orderService.findOne(notification.getRefId());
      requisitionId = order.getExternalId();
    } else if (notification.getRefStatus().isShipmentPeriod()) {
      ShipmentDto shipment = shipmentService.findOne(notification.getRefId());
      OrderDto order = orderService.findOne(shipment.getOrder().getId());
      requisitionId = order.getExternalId();
    } else {
      throw new UnsupportedOperationException();
    }
    RequisitionV2Dto requisition = requisitionService.searchRequisition(requisitionId);
    dto.setEmergencyFlag(requisition.getEmergency());

    User operator = userRepository.findOne(notification.getOperatorId());
    final FacilityDto facility = facilityService.findOne(operator.getHomeFacilityId());
    dto.setSourceFacilityName(facility.getName());
  }

}
