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

package org.siglus.siglusapi.localmachine.event.order.fulfillment;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusStatusChangeRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentSyncedEmitter {

  public static final String FULFILL = "FULFILL-";
  private final EventPublisher eventPublisher;
  private final SiglusAuthenticationHelper authHelper;
  private final SiglusRequisitionRepository siglusRequisitionRepository;
  private final RequisitionRepository requisitionRepository;
  private final OrderRepository orderRepository;
  private final SiglusStatusChangeRepository siglusStatusChangeRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;

  public OrderFulfillmentSyncedEvent emit(boolean isWithLocation, boolean isSubOrder,
      ShipmentExtensionRequest shipmentExtensionRequest) {
    ShipmentDto shipmentDto = shipmentExtensionRequest.getShipment();

    RequisitionExtension requisitionExtension =
        requisitionExtensionRepository.findByRequisitionNumber(shipmentDto.getOrder().getRequisitionNumber());
    List<StatusChange> statusChanges =
        siglusStatusChangeRepository.findByRequisitionId(requisitionExtension.getRequisitionId());

    // TODO: stock event submit locally add implement ( 2022/9/15 by kourengang)
    OrderFulfillmentSyncedEvent event = OrderFulfillmentSyncedEvent.builder()
        .finalApproveUserId(statusChanges.stream().filter(item ->
            item.getStatus() == RequisitionStatus.APPROVED).findFirst().orElse(new StatusChange()).getAuthorId())
        .convertToOrderUserId(statusChanges.stream().filter(item ->
            item.getStatus() == RequisitionStatus.RELEASED).findFirst().orElse(new StatusChange()).getAuthorId())
        .fulfillUserId(authHelper.getCurrentUser().getId())
        .supplierFacilityId(authHelper.getCurrentUser().getHomeFacilityId())
        .isWithLocation(isWithLocation)
        .isSubOrder(isSubOrder)
        .shipmentExtensionRequest(shipmentExtensionRequest)
        .convertToOrderRequest(getConvertToOrderRequest(shipmentDto, requisitionExtension))
        .build();
    // todo at this break point, shipmentDto.getOrder().getOrderCode() is not right
    eventPublisher.emitGroupEvent(
        FULFILL + shipmentDto.getOrder().getRequisitionNumber(),
        shipmentDto.getOrder().getFacility().getId(), event);
    return event;
  }

  private ConvertToOrderRequest getConvertToOrderRequest(ShipmentDto shipmentDto,
      RequisitionExtension requisitionExtension) {
    if (checkIfNeededFirstOrder(shipmentDto.getOrder())) {
      Requisition requisition = requisitionRepository.findOne(requisitionExtension.getRequisitionId());
      final List<RequisitionLineItemRequest> requisitionLineItemRequests =
          requisition.getRequisitionLineItems().stream()
              .map(requisitionLineItem -> RequisitionLineItemRequest.builder()
                  .orderable(requisitionLineItem.getOrderable())
                  .approvedQuantity(requisitionLineItem.getApprovedQuantity())
                  .build())
              .collect(Collectors.toList());
      return ConvertToOrderRequest.builder()
          .firstOrder(orderRepository.findOne(shipmentDto.getOrder().getId()))
          .requisitionNumber(shipmentDto.getOrder().getRequisitionNumber())
          .requisitionLineItems(requisitionLineItemRequests)
          .build();
    }
    return null;
  }

  private boolean checkIfNeededFirstOrder(OrderObjectReferenceDto order) {
    return siglusRequisitionRepository.countById(order.getExternalId()) > 0;
  }
}
