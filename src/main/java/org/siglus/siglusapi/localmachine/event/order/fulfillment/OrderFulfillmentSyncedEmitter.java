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

import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.ORDER_FULFILLED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusStatusChangeRepository;
import org.siglus.siglusapi.service.SiglusLotService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentSyncedEmitter {

  private final EventPublisher eventPublisher;
  private final SiglusAuthenticationHelper authHelper;
  private final SiglusRequisitionRepository siglusRequisitionRepository;
  private final RequisitionRepository requisitionRepository;
  private final OrderRepository orderRepository;
  private final SiglusStatusChangeRepository siglusStatusChangeRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final OrderExternalRepository orderExternalRepository;
  private final SiglusLotService siglusLotService;

  public OrderFulfillmentSyncedEvent emit(boolean isWithLocation, boolean isSubOrder,
      ShipmentExtensionRequest shipmentExtensionRequest) {
    ShipmentDto shipmentDto = shipmentExtensionRequest.getShipment();

    RequisitionExtension requisitionExtension =
        requisitionExtensionRepository.findByRequisitionNumber(shipmentDto.getOrder().getRequisitionNumber());
    List<StatusChange> statusChanges =
        siglusStatusChangeRepository.findByRequisitionId(requisitionExtension.getRequisitionId());
    Optional<StatusChange> finalApprovedStatusChange = statusChanges.stream().filter(item ->
        item.getStatus() == RequisitionStatus.APPROVED).findFirst();
    // TODO: stock event submit locally add implement ( 2022/9/15 by kourengang)
    OrderFulfillmentSyncedEvent event = OrderFulfillmentSyncedEvent.builder()
        .finalApproveUserId(finalApprovedStatusChange.orElse(new StatusChange()).getAuthorId())
        .convertToOrderUserId(statusChanges.stream().filter(item ->
            item.getStatus() == RequisitionStatus.RELEASED).findFirst().orElse(new StatusChange()).getAuthorId())
        .fulfillUserId(authHelper.getCurrentUser().getId())
        .supplierFacilityId(authHelper.getCurrentUser().getHomeFacilityId())
        .isWithLocation(isWithLocation)
        .isSubOrder(isSubOrder)
        .shipmentExtensionRequest(shipmentExtensionRequest)
        .convertToOrderRequest(getConvertToOrderRequest(shipmentDto, requisitionExtension, finalApprovedStatusChange))
        .shippedLotList(getAllShippedLots(shipmentExtensionRequest.getShipment()))
        .build();
    eventPublisher.emitGroupEvent(requisitionExtension.getRealRequisitionNumber(),
        shipmentDto.getOrder().getFacility().getId(), event, ORDER_FULFILLED);
    return event;
  }

  private List<LotDto> getAllShippedLots(ShipmentDto shipment) {
    if (CollectionUtils.isEmpty(shipment.getLineItems())) {
      return new ArrayList<>();
    }
    List<UUID> lotIds = shipment.getLineItems().stream()
        .map(ShipmentLineItem.Importer::getLotId).collect(Collectors.toList());
    return siglusLotService.getLotList(new ArrayList<>(lotIds));
  }

  private ConvertToOrderRequest getConvertToOrderRequest(ShipmentDto shipmentDto,
      RequisitionExtension requisitionExtension, Optional<StatusChange> finalApprovedStatusChange) {
    if (neededFirstOrder(shipmentDto.getOrder(), requisitionExtension.getRequisitionId())) {
      Requisition requisition = requisitionRepository.findOne(requisitionExtension.getRequisitionId());
      final List<RequisitionLineItemRequest> requisitionLineItemRequests = new ArrayList<>();
      requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
        RequisitionLineItemRequest requisitionLineItemRequest = new RequisitionLineItemRequest();
        BeanUtils.copyProperties(requisitionLineItem, requisitionLineItemRequest);
        requisitionLineItemRequests.add(requisitionLineItemRequest);
      });
      return ConvertToOrderRequest.builder()
          .firstOrder(orderRepository.findOne(shipmentDto.getOrder().getId()))
          .requisitionNumber(shipmentDto.getOrder().getRequisitionNumber())
          .requisitionLineItems(requisitionLineItemRequests)
          .finalApproveStatusMessage(getStatusMessageRequest(finalApprovedStatusChange))
          .finalApproveSupervisoryNodeId(finalApprovedStatusChange.orElse(new StatusChange()).getSupervisoryNodeId())
          .build();
    }
    return null;
  }

  private StatusMessageRequest getStatusMessageRequest(Optional<StatusChange> finalApprovedStatusChange) {
    StatusMessage statusMessage =
        finalApprovedStatusChange.orElseThrow(IllegalStateException::new).getStatusMessage();
    StatusMessageRequest finalApproveStatusMessage = null;
    if (statusMessage != null) {
      finalApproveStatusMessage = new StatusMessageRequest();
      BeanUtils.copyProperties(statusMessage, finalApproveStatusMessage);
    }
    return finalApproveStatusMessage;
  }

  private boolean neededFirstOrder(OrderObjectReferenceDto order, UUID requisitionId) {
    return externalIdIsRequisitionId(order) || orderExternalRepository.findByRequisitionId(requisitionId).size() == 1;
  }

  private boolean externalIdIsRequisitionId(OrderObjectReferenceDto order) {
    return siglusRequisitionRepository.countById(order.getExternalId()) > 0;
  }
}
