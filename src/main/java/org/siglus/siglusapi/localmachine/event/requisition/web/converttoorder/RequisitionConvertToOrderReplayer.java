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

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.localmachine.event.NotificationService;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.StatusMessageRequest;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequisitionConvertToOrderReplayer {
  private final SiglusOrdersRepository siglusOrdersRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final RequisitionRepository requisitionRepository;
  private final EventCommonService eventCommonService;
  private final NotificationService notificationService;

  @Transactional
  @EventListener(value = {RequisitionConvertToOrderEvent.class})
  public void replay(RequisitionConvertToOrderEvent event) {
    try {
      log.info("start replay convert to order requisition number= "
          + event.getConvertToOrderRequest().getRequisitionNumber());
      doReplay(event);
      log.info("end replay convert to order requisition number = "
          + event.getConvertToOrderRequest().getRequisitionNumber());
    } catch (Exception e) {
      log.error("fail to replay fulfill order event, msg = " + e.getMessage(), e);
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  public void doReplay(RequisitionConvertToOrderEvent event) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionNumber(
        event.getConvertToOrderRequest().getRequisitionNumber());
    // reset requisition id
    event.getConvertToOrderRequest().getFirstOrder().setExternalId(requisitionExtension.getRequisitionId());
    Requisition requisition = requisitionRepository.findOne(requisitionExtension.getRequisitionId());
    if (RequisitionStatus.IN_APPROVAL.equals(requisition.getStatus())) {
      finalApprove(requisition, requisitionExtension, event);
    }

    Order order = convertToOrder(event, requisition);
    log.info("save requisition convert to order, orderId:{}", order.getId());
    siglusOrdersRepository.saveAndFlush(order);

    notificationService.postConvertToOrder(event.getConvertToOrderUserId(),
        event.getSupplierFacilityId(), order);
  }

  private void finalApprove(Requisition requisition, RequisitionExtension requisitionExtension,
                RequisitionConvertToOrderEvent event) {
    resetApprovedQuantity(requisition, event);

    // do approve
    requisition.approve(null, eventCommonService.getOrderableDtoMap(requisition), Collections.emptyList(),
        event.getFinalApproveUserId());

    StatusChange finalApprovedStatusChange = requisition.getStatusChanges().stream().filter(item ->
        item.getStatus() == RequisitionStatus.APPROVED).findFirst().orElseThrow(IllegalStateException::new);
    resetFinalApproveStatusMessage(requisition, event.getConvertToOrderRequest().getFinalApproveStatusMessage(),
        finalApprovedStatusChange);

    finalApprovedStatusChange.setSupervisoryNodeId(event.getConvertToOrderRequest().getFinalApproveSupervisoryNodeId());
    requisitionRepository.saveAndFlush(requisition);

    requisitionExtension.setIsApprovedByInternal(false);
    requisitionExtensionRepository.saveAndFlush(requisitionExtension);
  }

  private void resetApprovedQuantity(Requisition requisition, RequisitionConvertToOrderEvent event) {
    if (CollectionUtils.isEmpty(event.getConvertToOrderRequest().getRequisitionLineItems())) {
      return;
    }
    Map<VersionEntityReference, RequisitionLineItem> requisitionLineItemMap =
        requisition.getRequisitionLineItems().stream().collect(toMap(RequisitionLineItem::getOrderable,
        Function.identity()));
    List<RequisitionLineItem> newLineItems = new ArrayList<>();
    event.getConvertToOrderRequest().getRequisitionLineItems().forEach(item -> {
      RequisitionLineItem requisitionLineItem = requisitionLineItemMap.get(item.getOrderable());
      if (requisitionLineItem != null) {
        requisitionLineItem.setApprovedQuantity(item.getApprovedQuantity());
      } else {
        // new and add to line item list
        RequisitionLineItem requisitionLineItemRequest = new RequisitionLineItem();
        BeanUtils.copyProperties(item, requisitionLineItemRequest);
        requisitionLineItemRequest.setRequisition(requisition);
        newLineItems.add(requisitionLineItemRequest);
      }
    });
    requisition.getRequisitionLineItems().addAll(newLineItems);
  }

  public Order convertToOrder(RequisitionConvertToOrderEvent event, Requisition requisition) {
    releaseRequisitionsAsOrder(requisition, event.getConvertToOrderUserId(), event.getSupplierFacilityId());
    return createOrder(event);
  }

  private Order createOrder(RequisitionConvertToOrderEvent event) {
    return siglusOrdersRepository.saveAndFlush(event.getConvertToOrderRequest().getFirstOrder());
  }

  private void releaseRequisitionsAsOrder(Requisition requisition, UUID supplierUserId, UUID supplierFacilityId) {
    requisition.release(supplierUserId);
    requisition.setSupplyingFacilityId(supplierFacilityId);
    requisitionRepository.saveAndFlush(requisition);
  }

  private void resetFinalApproveStatusMessage(Requisition requisition, StatusMessageRequest statusMessageRequest,
                        StatusChange finalApprovedStatusChange) {
    if (statusMessageRequest == null) {
      return;
    }
    StatusMessage newStatusMessage = new StatusMessage();
    newStatusMessage.setRequisition(requisition);
    newStatusMessage.setStatusChange(finalApprovedStatusChange);
    newStatusMessage.setId(statusMessageRequest.getAuthorId());
    newStatusMessage.setCreatedDate(statusMessageRequest.getCreatedDate());
    newStatusMessage.setModifiedDate(statusMessageRequest.getModifiedDate());
    newStatusMessage.setAuthorId(statusMessageRequest.getAuthorId());
    newStatusMessage.setAuthorFirstName(statusMessageRequest.getAuthorFirstName());
    newStatusMessage.setAuthorLastName(statusMessageRequest.getAuthorLastName());
    newStatusMessage.setStatus(RequisitionStatus.APPROVED);
    newStatusMessage.setBody(statusMessageRequest.getBody());
    finalApprovedStatusChange.setStatusMessage(newStatusMessage);
  }

}
