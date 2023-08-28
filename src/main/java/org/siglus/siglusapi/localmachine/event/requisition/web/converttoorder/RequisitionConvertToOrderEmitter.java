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

package org.siglus.siglusapi.localmachine.event.requisition.web.converttoorder;

import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.REQUISITION_CONVERT_TO_ORDER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.ConvertToOrderRequest;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.RequisitionLineItemRequest;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.StatusMessageRequest;
import org.siglus.siglusapi.repository.SiglusStatusChangeRepository;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequisitionConvertToOrderEmitter {
  private final RequisitionRepository requisitionRepository;
  private final OrderRepository orderRepository;
  private final SiglusStatusChangeRepository siglusStatusChangeRepository;
  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  private final EventPublisher eventPublisher;
  private final EventCommonService eventCommonService;

  public RequisitionConvertToOrderEvent emit(ReleasableRequisitionDto releasableRequisitionDto, UUID authorId) {
    RequisitionConvertToOrderEvent event = getConvertToOrderEvent(releasableRequisitionDto, authorId);
    // ignore final approve event
    eventPublisher.emitGroupEventForConvertToOrderEvent(
        eventCommonService.getGroupId(releasableRequisitionDto.getRequisitionId()),
        getReceiverId(releasableRequisitionDto.getRequisitionId()), event, REQUISITION_CONVERT_TO_ORDER);

    return event;
  }

  private RequisitionConvertToOrderEvent getConvertToOrderEvent(ReleasableRequisitionDto releasableRequisitionDto,
                                                                UUID authorId) {
    UUID requisitionId = releasableRequisitionDto.getRequisitionId();
    String requisitionNumber = siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId);

    Requisition requisition = requisitionRepository.findOne(requisitionId);
    final List<RequisitionLineItemRequest> requisitionLineItemRequests = new ArrayList<>();
    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      RequisitionLineItemRequest requisitionLineItemRequest = new RequisitionLineItemRequest();
      BeanUtils.copyProperties(requisitionLineItem, requisitionLineItemRequest);
      requisitionLineItemRequests.add(requisitionLineItemRequest);
    });

    List<StatusChange> statusChanges =
        siglusStatusChangeRepository.findByRequisitionId(requisitionId);
    Optional<StatusChange> finalApprovedStatusChange = statusChanges.stream().filter(item ->
        item.getStatus() == RequisitionStatus.APPROVED).findFirst();

    Order order = orderRepository.findByExternalId(requisitionId);

    ConvertToOrderRequest request = ConvertToOrderRequest.builder()
        .firstOrder(order)
        .requisitionNumber(requisitionNumber)
        .requisitionLineItems(requisitionLineItemRequests)
        .finalApproveStatusMessage(getStatusMessageRequest(finalApprovedStatusChange))
        .finalApproveSupervisoryNodeId(finalApprovedStatusChange.orElse(new StatusChange()).getSupervisoryNodeId())
        .build();

    return RequisitionConvertToOrderEvent.builder()
        .convertToOrderUserId(authorId)
        .supplierFacilityId(releasableRequisitionDto.getSupplyingDepotId())
        .finalApproveUserId(finalApprovedStatusChange.orElse(new StatusChange()).getAuthorId())
        .convertToOrderRequest(request)
        .build();
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

  private UUID getReceiverId(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    return requisition.getFacilityId();
  }
}
