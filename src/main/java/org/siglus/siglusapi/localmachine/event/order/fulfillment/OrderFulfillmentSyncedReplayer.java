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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.siglus.siglusapi.util.LocationUtil.getIfNonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.BaseEntity;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.service.ShipmentService;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.ShipmentLineItemsExtension;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.localmachine.event.NotificationService;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentRepository;
import org.siglus.siglusapi.service.SiglusLotService;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderFulfillmentSyncedReplayer {

  private final SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  private final SiglusOrdersRepository siglusOrdersRepository;
  private final OrderDtoBuilder orderDtoBuilder;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final RequisitionRepository requisitionRepository;
  private final EventCommonService eventCommonService;
  private final OrderExternalRepository orderExternalRepository;
  private final OrderLineItemExtensionRepository lineItemExtensionRepository;
  private final SiglusShipmentRepository siglusShipmentRepository;
  private final ProofOfDeliveryRepository proofOfDeliveryRepository;
  private final ShipmentLineItemsExtensionRepository shipmentLineItemsExtensionRepository;
  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;
  private final PodExtensionRepository podExtensionRepository;
  private final NotificationService notificationService;
  private final SiglusShipmentService siglusShipmentService;
  private final SiglusLotReferenceDataService lotReferenceDataService;
  private final SiglusLotService siglusLotService;
  private final SiglusOrderService siglusOrderService;
  private final ShipmentService shipmentService;
  private final LotRepository lotRepository;

  @Transactional
  @EventListener(value = {OrderFulfillmentSyncedEvent.class})
  public void replay(OrderFulfillmentSyncedEvent event) {
    try {
      log.info("start replay order code = "
          + event.getShipmentExtensionRequest().getShipment().getOrder().getOrderCode());
      doReplay(event);
      log.info("end replay order code = "
          + event.getShipmentExtensionRequest().getShipment().getOrder().getOrderCode());
    } catch (Exception e) {
      log.error("fail to replay fulfill order event, msg = " + e.getMessage(), e);
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  public void doReplay(OrderFulfillmentSyncedEvent event) {
    Order order;
    simulateUserAuthHelper.simulateNewUserAuth(event.getFulfillUserId());
    // replace the lot id with the old lot in LM here in ShipmentExtensionRequest.shipment
    Map<UUID, UUID> newIdToOldIdMap = new HashMap<>();
    createLotsIfNotExist(event.getShippedLotList(), newIdToOldIdMap);
    if (!newIdToOldIdMap.isEmpty()) {
      List<ShipmentLineItemDto> updatedLineItems = new ArrayList<>();
      ShipmentDto shipmentDto = event.getShipmentExtensionRequest().getShipment();
      shipmentDto.getLineItems().forEach(lineItem -> {
        if (newIdToOldIdMap.containsKey(lineItem.getLotId())) {
          ShipmentLineItemDto copy = new ShipmentLineItemDto();
          BeanUtils.copyProperties(lineItem, copy);
          copy.setLotId(newIdToOldIdMap.get(lineItem.getLotId()));
          copy.setLot(new ObjectReferenceDto(newIdToOldIdMap.get(lineItem.getLotId())));
          updatedLineItems.add(copy);
        } else {
          updatedLineItems.add((ShipmentLineItemDto) lineItem);
        }
      });
      shipmentDto.setLineItems(updatedLineItems);
    }


    if (event.isNeedConvertToOrder()) {
      RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionNumber(
          event.getConvertToOrderRequest().getRequisitionNumber());
      Requisition requisition = requisitionRepository.findOne(requisitionExtension.getRequisitionId());
      if (RequisitionStatus.IN_APPROVAL.equals(requisition.getStatus())) {
        // reset requisition id
        event.getShipmentExtensionRequest()
            .getShipment().getOrder().setExternalId(requisitionExtension.getRequisitionId());
        event.getConvertToOrderRequest().getFirstOrder().setExternalId(requisitionExtension.getRequisitionId());
        finalApprove(requisition, requisitionExtension, event);
        order = convertToOrder(event, requisition);
      } else {
        Order orderOrigin =
            siglusOrdersRepository
                .findByOrderCode(event.getShipmentExtensionRequest().getShipment().getOrder().getOrderCode());
        order = updateOrderLineItems(event.getShipmentExtensionRequest().getShipment().getOrder(), orderOrigin);
        order.setExternalId(orderOrigin.getExternalId());
      }
    } else {
      Order orderOrigin =
          siglusOrdersRepository
              .findByOrderCode(event.getShipmentExtensionRequest().getShipment().getOrder().getOrderCode());
      order = updateOrderLineItems(event.getShipmentExtensionRequest().getShipment().getOrder(), orderOrigin);
      order.setExternalId(orderOrigin.getExternalId());
    }
    
    // pre handle
    Set<UUID> skippedOrderableIds = getSkippedOrderLineItemIds(event.getShipmentExtensionRequest().getShipment());
    removeSkippedOrderLineItems(skippedOrderableIds, order, event);
    // set to SHIPPED
    order.updateStatus(OrderStatus.SHIPPED, new UpdateDetails(event.getFulfillUserId(), ZonedDateTime.now()));
    Order shipped = siglusOrdersRepository.saveAndFlush(order);

    UUID proofOfDeliveryId = fulfillOrder(event, shipped);

    notificationService.postFulfillment(event.getFulfillUserId(), proofOfDeliveryId, order);
  }

  private UUID fulfillOrder(OrderFulfillmentSyncedEvent event, Order shipped) {
    OrderDto orderDto = orderDtoBuilder.build(shipped);
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(orderDto.getId());
    BeanUtils.copyProperties(orderDto, orderObjectReferenceDto);
    event.getShipmentExtensionRequest().getShipment().setOrder(orderObjectReferenceDto);
    return doFulfillOrder(event, shipped);
  }

  private Order updateOrderLineItems(OrderObjectReferenceDto orderDto, Order orderOrigin) {
    List<OrderLineItemDto> orderLineItemDtos = orderDto.getOrderLineItems();
    if (CollectionUtils.isEmpty(orderLineItemDtos)) {
      return orderOrigin;
    }
    List<OrderLineItem> lineItemsOrigin = CollectionUtils.isEmpty(orderOrigin.getOrderLineItems())
        ? new ArrayList<>() : orderOrigin.getOrderLineItems();
    orderLineItemDtos.stream()
        .filter(orderLineItemDto -> orderLineItemDto.getId() == null || orderLineItemDto.isAdded())
        .filter(orderLineItemDto -> !orderLineItemDto.isSkipped())
        .map(OrderLineItem::newInstance)
        .forEach(orderLineItem -> {
          orderLineItem.setOrder(orderOrigin);
          lineItemsOrigin.add(orderLineItem);
        });
    log.info("update orderId: {}, orderLineItem: {}", orderOrigin.getId(), lineItemsOrigin);
    return siglusOrdersRepository.saveAndFlush(orderOrigin);
  }

  private void finalApprove(Requisition requisition, RequisitionExtension requisitionExtension,
      OrderFulfillmentSyncedEvent event) {
    resetApprovedQuantity(requisition, event);
    setSkippedOrderable(requisition, event);

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

  private void setSkippedOrderable(Requisition requisition, OrderFulfillmentSyncedEvent event) {
    ShipmentDto shipmentDto = event.getShipmentExtensionRequest().getShipment();
    Set<UUID> shippedOrderableIds = shipmentDto.getLineItems().stream().map(e -> e.getOrderableIdentity().getId())
        .collect(Collectors.toSet());
    requisition.getRequisitionLineItems().forEach(e -> {
      if (!shippedOrderableIds.contains(e.getOrderable().getId())) {
        e.setSkipped(Boolean.TRUE);
      }
    });
  }

  private void createLotsIfNotExist(List<LotDto> shippedLotList, Map<UUID, UUID> newIdToOldIdMap) {
    List<UUID> lotIds = shippedLotList.stream().map(LotDto::getId).collect(Collectors.toList());
    List<LotDto> lotList = siglusLotService.getLotList(lotIds);
    List<UUID> existedLotIds = lotList.stream().map(LotDto::getId).collect(Collectors.toList());
    List<LotDto> notExistedLotIds =
        shippedLotList.stream().filter(item -> !existedLotIds.contains(item.getId())).collect(Collectors.toList());

    Set<String> pair = notExistedLotIds.stream().map(lotDto ->
            lotDto.getLotCode() + lotDto.getTradeItemId().toString()).collect(toSet());
    if (CollectionUtils.isNotEmpty(pair)) {
      List<Lot> historicalLots = lotRepository.findLotCodeAndTradeItemIdPairs(pair);
      List<LotDto> notExistedNoHistoricalLots = new ArrayList<>();
      notExistedLotIds.forEach(lotDto -> {
        Optional<Lot> existed = historicalLots.stream().filter(lot ->
                        lot.getLotCode().equals(lotDto.getLotCode())
                                && lot.getTradeItem().getId().equals(lotDto.getTradeItemId()))
                .findFirst();
        if (existed.isPresent() && !existed.get().getId().equals(lotDto.getId())) {
          newIdToOldIdMap.put(lotDto.getId(), existed.get().getId());
        } else {
          notExistedNoHistoricalLots.add(lotDto);
        }
      });

      log.info("create lots, size = " + notExistedNoHistoricalLots.size());
      lotReferenceDataService.batchSaveLot(notExistedNoHistoricalLots);
    } else {
      log.info("create lots, size = " + notExistedLotIds.size());
      lotReferenceDataService.batchSaveLot(notExistedLotIds);
    }
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

  private void resetApprovedQuantity(Requisition requisition, OrderFulfillmentSyncedEvent event) {
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

  public Order convertToOrder(OrderFulfillmentSyncedEvent event, Requisition requisition) {
    releaseRequisitionsAsOrder(requisition, event.getConvertToOrderUserId(), event.getSupplierFacilityId());
    return createOrder(event);
  }

  private Order createOrder(OrderFulfillmentSyncedEvent event) {
    return siglusOrdersRepository.saveAndFlush(event.getConvertToOrderRequest().getFirstOrder());
  }

  private void releaseRequisitionsAsOrder(Requisition requisition, UUID supplierUserId, UUID supplierFacilityId) {
    requisition.release(supplierUserId);
    requisition.setSupplyingFacilityId(supplierFacilityId);
    requisitionRepository.saveAndFlush(requisition);
  }

  private UUID doFulfillOrder(OrderFulfillmentSyncedEvent event, Order shipped) {
    if (event.isWithLocation()) {
      siglusShipmentService.mergeShipmentLineItems(event.getShipmentExtensionRequest().getShipment());
    }
    Shipment shipment = createSubOrderAndShipment(event.isSubOrder(),
        event.getShipmentExtensionRequest().getShipment(), event.getFulfillUserId(), shipped);
    if (event.isWithLocation()) {
      saveShipmentLineItemsExtensionWithLocation(event, shipment);
    }
    return savePodExtension(shipment.getId(), event.getShipmentExtensionRequest());
  }

  public UUID savePodExtension(UUID shipmentId, ShipmentExtensionRequest shipmentExtensionRequest) {
    ProofOfDelivery proofOfDelivery = siglusProofOfDeliveryRepository.findByShipmentId(shipmentId);
    UUID proofOfDeliveryId = proofOfDelivery.getId();
    PodExtension podExtension = PodExtension
        .builder()
        .podId(proofOfDeliveryId)
        .conferredBy(shipmentExtensionRequest.getConferredBy())
        .preparedBy(shipmentExtensionRequest.getPreparedBy())
        .build();
    log.info("save pod extension when confirm shipment, shipmentId: {}", shipmentId);
    podExtensionRepository.save(podExtension);
    return proofOfDeliveryId;
  }

  private void saveShipmentLineItemsExtensionWithLocation(OrderFulfillmentSyncedEvent event, Shipment shipment) {
    Map<String, UUID> shipmentUkToId = shipment.getLineItems().stream().collect(toMap(this::buildForUniqueKey,
        BaseEntity::getId));
    List<ShipmentLineItemDto> shipmentLineItemDtos = event.getShipmentExtensionRequest().getShipment().lineItems();
    Multimap<String, ShipmentLineItemDto> uniqueKeyMap = ArrayListMultimap.create();
    shipmentLineItemDtos.forEach(shipmentLineItemDto -> {
      String uniqueKey = buildForUniqueKey(shipmentLineItemDto);
      UUID newId = shipmentUkToId.get(uniqueKey);
      if (newId == null) {
        throw new IllegalStateException("not matched. uniqueKey = " + uniqueKey);
      }
      // reset shipment line item id
      shipmentLineItemDto.setId(newId);
      uniqueKeyMap.put(uniqueKey, shipmentLineItemDto);
    });
    List<ShipmentLineItemsExtension> shipmentLineItemsByLocations = Lists.newArrayList();
    fulfillLocationInfo(uniqueKeyMap, event.getShipmentExtensionRequest().getShipment());
    event.getShipmentExtensionRequest().getShipment().lineItems().forEach(shipmentLineItemDto -> {
      UUID lineItemId = shipmentLineItemDto.getId();
      ShipmentLineItemsExtension shipmentLineItemsByLocation = ShipmentLineItemsExtension
          .builder()
          .shipmentLineItemId(lineItemId)
          .locationCode(getIfNonNull(LocationDto::getLocationCode, shipmentLineItemDto.getLocation()))
          .area(getIfNonNull(LocationDto::getArea, shipmentLineItemDto.getLocation()))
          .build();
      shipmentLineItemsByLocations.add(shipmentLineItemsByLocation);
    });
    log.info("create shipment line item by location, size: {}", shipmentLineItemsByLocations.size());
    shipmentLineItemsExtensionRepository.save(shipmentLineItemsByLocations);
  }

  private void fulfillLocationInfo(Multimap<String, ShipmentLineItemDto> uniqueKeyMap, ShipmentDto shipmentDto) {
    for (ShipmentLineItemDto lineItemDto : shipmentDto.lineItems()) {
      String newKey = buildForUniqueKey(lineItemDto);
      List<ShipmentLineItemDto> lineItemDtos = (List<ShipmentLineItemDto>) uniqueKeyMap.get(newKey);
      if (null != lineItemDtos.get(0).getLocation()) {
        lineItemDto.setLocation(lineItemDtos.get(0).getLocation());
        uniqueKeyMap.remove(newKey, lineItemDtos.get(0));
      }
    }
  }

  private String buildForUniqueKey(ShipmentLineItem shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLotId()) {
      return shipmentLineItemDto.getOrderable().getId().toString();
    }
    return shipmentLineItemDto.getLotId() + "&" + shipmentLineItemDto.getOrderable().getId();
  }

  private String buildForUniqueKey(ShipmentLineItemDto shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLot()) {
      return shipmentLineItemDto.getOrderable().getId().toString();
    }
    return shipmentLineItemDto.getLot().getId() + "&" + shipmentLineItemDto.getOrderable().getId();
  }

  private Shipment createSubOrderAndShipment(boolean isSubOrder, ShipmentDto shipmentDto, UUID fulfillUserId,
      Order shipped) {
    if (isSubOrder) {
      createSubOrder(shipmentDto, fulfillUserId);
    }
    return createShipment(shipmentDto, fulfillUserId, shipped);
  }

  public Shipment createShipment(ShipmentDto shipmentDto, UUID fulfillUserId, Order shippedOrder) {
    nullIds(shipmentDto);
    shipmentDto.setShipDetails(new CreationDetails(fulfillUserId, ZonedDateTime.now()));
    fillEmptyQuantityShipped(shipmentDto);
    shipmentService.mergeShipmentLineItems(shipmentDto);
    Shipment shipment = Shipment.newInstance(shipmentDto, shippedOrder);

    Shipment saved = this.siglusShipmentRepository.saveAndFlush(shipment);
    ProofOfDelivery proofOfDelivery = ProofOfDelivery.newInstance(saved);
    this.proofOfDeliveryRepository.saveAndFlush(proofOfDelivery);
    return saved;
  }

  private void fillEmptyQuantityShipped(ShipmentDto shipmentDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDto.lineItems();
    lineItemDtos.forEach(lineItem -> {
      if (lineItem.getQuantityShipped() == null) {
        lineItem.setQuantityShipped(0L);
      }
    });
  }

  private void nullIds(ShipmentDto shipmentDto) {
    shipmentDto.setId(null);
    if (shipmentDto.lineItems() != null) {
      shipmentDto.lineItems().forEach(l -> l.setId(null));
    }
  }

  private void removeSkippedOrderLineItems(Set<UUID> skippedOrderableIds,
      Order order, OrderFulfillmentSyncedEvent event) {
    log.info("skippedOrderableIds size = " + skippedOrderableIds.size());
    List<UUID> skippedLineItemIds = new ArrayList<>();
    order.getOrderLineItems().forEach(orderLineItem -> {
      if (skippedOrderableIds.contains(orderLineItem.getOrderable().getId())) {
        skippedLineItemIds.add(orderLineItem.getId());
      }
    });
    log.info("skippedLineItemIds size = " + skippedLineItemIds.size());
    order.getOrderLineItems().removeIf(
        orderLineItem -> skippedOrderableIds.contains(orderLineItem.getOrderable().getId()));
    siglusOrdersRepository.saveAndFlush(order);
    if (CollectionUtils.isEmpty(skippedLineItemIds)) {
      return;
    }
    List<OrderLineItemExtension> extensions = lineItemExtensionRepository.findByOrderLineItemIdIn(skippedLineItemIds);
    lineItemExtensionRepository.delete(extensions);

    event.getShipmentExtensionRequest().getShipment().lineItems().removeIf(lineItem ->
        skippedOrderableIds.contains(lineItem.getOrderable().getId()));
  }

  private void createSubOrder(ShipmentDto shipmentDto, UUID fulfillUserId) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    Map<UUID, List<ShipmentLineItem.Importer>> groupShipment = shipmentDto.getLineItems().stream()
        .collect(Collectors.groupingBy(lineItem -> lineItem.getOrderableIdentity().getId()));
    OrderObjectReferenceDto order = shipmentDto.getOrder();
    List<OrderLineItemDto> orderLineItems = order.getOrderLineItems();
    List<OrderLineItemDto> subOrderLineItems = getSubOrderLineItemDtos(skippedOrderLineItemIds,
        groupShipment, orderLineItems);
    createSubOrder(order, subOrderLineItems, fulfillUserId);
  }

  public void createSubOrder(OrderObjectReferenceDto order,
      List<OrderLineItemDto> orderLineItemDtos, UUID fulfillUserId) {
    // if order's external id is not found in orderExternalRepository, then it means this is a requisition id
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    OrderExternal newOrderExternal = null;
    if (external == null) {
      // need convert to order
      OrderExternal firstExternal = OrderExternal.builder()
          .requisitionId(order.getExternalId()).build();
      OrderExternal secondExternal = OrderExternal.builder()
          .requisitionId(order.getExternalId()).build();
      orderExternalRepository.saveAndFlush(firstExternal);
      updateExistOrderForSubOrder(order.getId(), firstExternal.getId(), order.getOrderCode(), order.getStatus());
      newOrderExternal = orderExternalRepository.saveAndFlush(secondExternal);
    } else {
      OrderExternal newExternal = OrderExternal.builder().requisitionId(external.getRequisitionId()).build();
      newOrderExternal = orderExternalRepository.saveAndFlush(newExternal);
    }
    String newOrderCode = siglusOrderService.increaseOrderNumber(order.getOrderCode());
    log.info("lastOrderCode {} newOrderCode {}", order.getOrderCode(), newOrderCode);
    createNewOrder(order, newOrderCode, orderLineItemDtos, newOrderExternal, fulfillUserId);
  }

  private void createNewOrder(OrderObjectReferenceDto order, String newOrderCode,
      List<OrderLineItemDto> orderLineItemDtos,
      OrderExternal newOrderExternal, UUID fulfillUserId) {
    OrderDto newOrder = new OrderDto();
    BeanUtils.copyProperties(order, newOrder);
    newOrder.setId(null);
    newOrder.setExternalId(newOrderExternal.getId());
    newOrder.setOrderCode(newOrderCode);
    newOrder.setOrderLineItems(orderLineItemDtos);
    // set status as PARTIALLY_FULFILLED directly
    newOrder.setStatus(OrderStatus.PARTIALLY_FULFILLED);
    newOrder.setStatusMessages(Collections.emptyList());

    Order saved = saveOrder(fulfillUserId, newOrder);
    updateOrderExtension(orderLineItemDtos, saved);
  }

  private Order saveOrder(UUID fulfillUserId, OrderDto order) {
    Order newOrder = Order.newInstance(order, new UpdateDetails(fulfillUserId, ZonedDateTime.now()));
    return siglusOrdersRepository.saveAndFlush(newOrder);
  }

  private void updateOrderExtension(
      List<org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtos,
      Order order) {
    Map<UUID, org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtoMap =
        orderLineItemDtos.stream()
            .collect(toMap(orderLineItemDto -> orderLineItemDto.getOrderableIdentity().getId(),
                orderLineItemDto -> orderLineItemDto));

    List<OrderLineItemExtension> extensions = new ArrayList<>();
    Map<UUID, OrderLineItemExtension> existedLineItemIdToExtension = lineItemExtensionRepository.findByOrderId(
        order.getId()).stream().collect(Collectors.toMap(OrderLineItemExtension::getOrderLineItemId, e -> e));
    for (OrderLineItem orderLineItem : order.getOrderLineItems()) {
      OrderLineItemExtension extension = Objects.isNull(existedLineItemIdToExtension.get(orderLineItem.getId()))
          ? new OrderLineItemExtension()
          : existedLineItemIdToExtension.get(orderLineItem.getId());
      extension.setOrderId(order.getId());
      extension.setOrderLineItemId(orderLineItem.getId());
      OrderLineItemDto orderLineItemDto = orderLineItemDtoMap.get(orderLineItem.getOrderable().getId());
      if (orderLineItemDto != null) {
        extension.setPartialFulfilledQuantity(orderLineItemDto.getPartialFulfilledQuantity());
      }

      extensions.add(extension);
    }
    log.info("save order line item extension, orderId:{}, size:{}", order.getId(), extensions);
    lineItemExtensionRepository.save(extensions);
  }

  private void updateExistOrderForSubOrder(UUID orderId,
      UUID externalId, String orderCode, OrderStatus orderStatus) {
    Order originOrder = siglusOrdersRepository.findOne(orderId);
    originOrder.setExternalId(externalId);
    originOrder.setOrderCode(orderCode);
    originOrder.setStatus(orderStatus);
    log.info("update exist order for subOrder: {}", originOrder);
    siglusOrdersRepository.saveAndFlush(originOrder);
  }

  private Set<UUID> getSkippedOrderLineItemIds(ShipmentDto shipmentDto) {
    return shipmentDto.getOrder().getOrderLineItems().stream()
        .filter(OrderLineItemDto::isSkipped)
        .map(orderLineItemDto -> orderLineItemDto.getOrderable().getId())
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private List<OrderLineItemDto> getSubOrderLineItemDtos(Set<UUID> skippedOrderLineItemIds,
      Map<UUID, List<ShipmentLineItem.Importer>> groupShipment, List<OrderLineItemDto> orderLineItems) {
    List<OrderLineItemDto> subOrderLineItems = new ArrayList<>();
    for (OrderLineItemDto dto : orderLineItems) {
      if (!skippedOrderLineItemIds.contains(dto.getId())
          && dto.getOrderedQuantity() != null && dto.getOrderedQuantity() > 0) {
        siglusShipmentService.calculateSubOrderPartialFulfilledValue(groupShipment, subOrderLineItems, dto);
      }
    }
    return subOrderLineItems;
  }
}
