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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderFulfillmentSyncedReplayer {

  private final SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  private final SiglusShipmentService siglusShipmentService;
  private final SiglusNotificationService notificationService;
  private final OrdersRepository ordersRepository;
  private final OrderDtoBuilder orderDtoBuilder;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final RequisitionRepository requisitionRepository;
  private final OrderableReferenceDataService orderableReferenceDataService;

  @EventListener(value = {OrderFulfillmentSyncedEvent.class})
  public void replay(OrderFulfillmentSyncedEvent event) {
    try {
      doReplay(event);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  @EventListener(value = {OrderFulfillmentSyncedEvent.class})
  public void doReplay(OrderFulfillmentSyncedEvent event) {
    if (event.isNeedConvertToOrder()) {
      RequisitionExtension requisitionExtension =
          requisitionExtensionRepository.findByRequisitionNumber(
              event.getConvertToOrderRequest().getRequisitionNumber());
      Requisition requisition = requisitionRepository.findOne(requisitionExtension.getRequisitionId());

      finalApprove(requisition, requisitionExtension, event);

      Order order = convertToOrder(event, requisition);
      // todo  reset order ?
      OrderDto orderDto = orderDtoBuilder.build(order);
      OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(orderDto.getId());
      BeanUtils.copyProperties(orderDto, orderObjectReferenceDto);
      event.getShipmentExtensionRequest().getShipment().setOrder(orderObjectReferenceDto);
    }
    fulfillOrder(event);
  }

  private void finalApprove(Requisition requisition, RequisitionExtension requisitionExtension,
      OrderFulfillmentSyncedEvent event) {
    // todo 新增lineitem？？
    resetApprovedQuantity(requisition, event);
    // do approve
    requisition.approve(null, getOrderableDtoMap(requisition), Collections.emptyList(),
        event.getFinalApproveUserId());

    requisitionRepository.saveAndFlush(requisition);

    requisitionExtension.setIsApprovedByInternal(false);
    requisitionExtensionRepository.save(requisitionExtension);

    // todo 需要发notice siglusNotificationService.postApprove(buildBaseRequisitionDto(requisition));
  }

  private void resetApprovedQuantity(Requisition requisition, OrderFulfillmentSyncedEvent event) {
    Map<VersionEntityReference, Integer> orderableToQuantity =
        event.getConvertToOrderRequest().getRequisitionLineItems().stream().collect(Collectors.toMap(
            RequisitionLineItemRequest::getOrderable, RequisitionLineItemRequest::getApprovedQuantity));
    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      Integer approvedQuantity = orderableToQuantity.get(requisitionLineItem.getOrderable());
      requisitionLineItem.setApprovedQuantity(approvedQuantity);
    });
  }

  public Map<VersionIdentityDto, OrderableDto> getOrderableDtoMap(Requisition requisition) {
    Set<VersionEntityReference> orderables =
        requisition.getRequisitionLineItems().stream()
            .map(RequisitionLineItem::getOrderable).collect(Collectors.toSet());
    Map<VersionIdentityDto, OrderableDto> orderableDtoMap =
        orderableReferenceDataService.findByIdentities(orderables).stream()
            .collect(Collectors.toMap(BasicOrderableDto::getIdentity, Function.identity()));
    log.info("orderableDtoMap size = " + orderableDtoMap.keySet().size());
    return orderableDtoMap;
  }

  public Order convertToOrder(OrderFulfillmentSyncedEvent event, Requisition requisition) {
    releaseRequisitionsAsOrder(requisition, event.getConvertToOrderUserId(), event.getSupplierFacilityId());
    return createOrder(event);
    // todo 需要发notice吗 前端主动掉的？后端没这个逻辑 notificationService.postConvertToOrder(order);
  }

  private Order createOrder(OrderFulfillmentSyncedEvent event) {
    return ordersRepository.saveAndFlush(event.getConvertToOrderRequest().getFirstOrder());
    // todo  notificationService.postRelease(); 看一下表里的数据就行了！！
    // TODO: notifiction of create order:  FulfillmentNotificationService.sendOrderCreatedNotification()
  }

  private void releaseRequisitionsAsOrder(Requisition requisition, UUID supplierUserId, UUID supplierFacilityId) {
    requisition.release(supplierUserId);
    requisition.setSupplyingFacilityId(supplierFacilityId);
    requisitionRepository.saveAndFlush(requisition);
    // TODO: notifiction of release add implement ( 2022/9/19 by kourengang)  notificationService.postRelease();
  }

  private void fulfillOrder(OrderFulfillmentSyncedEvent event) {
    simulateUserAuthHelper.simulateNewUserAuth(event.getFulfillUserId());
    ShipmentDto shipmentDto;
    if (event.isWithLocation()) {
      shipmentDto = siglusShipmentService.createOrderAndShipmentByLocation(event.isSubOrder(),
          event.getShipmentExtensionRequest());
    } else {
      shipmentDto = siglusShipmentService.createOrderAndShipment(event.isSubOrder(),
          event.getShipmentExtensionRequest());
    }
    notificationService.postConfirmShipment(shipmentDto);
  }
}
