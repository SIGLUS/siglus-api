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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ProofsOfDeliveryExtension;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.ShipmentLineItemsExtension;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.ProofsOfDeliveryExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentRepository;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderFulfillmentSyncedReplayer {

  private final SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  private final OrdersRepository ordersRepository;
  private final OrderDtoBuilder orderDtoBuilder;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final RequisitionRepository requisitionRepository;
  private final OrderableReferenceDataService orderableReferenceDataService;
  private final OrderExternalRepository orderExternalRepository;
  private final OrderLineItemExtensionRepository lineItemExtensionRepository;
  private final SiglusShipmentRepository siglusShipmentRepository;
  private final ProofOfDeliveryRepository proofOfDeliveryRepository;
  private final ShipmentLineItemsExtensionRepository shipmentLineItemsExtensionRepository;
  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;
  private final ProofsOfDeliveryExtensionRepository proofsOfDeliveryExtensionRepository;

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
    if (event.isNeedConvertToOrder()) {
      RequisitionExtension requisitionExtension =
          requisitionExtensionRepository.findByRequisitionNumber(
              event.getConvertToOrderRequest().getRequisitionNumber());
      // reset requisition id
      event.getShipmentExtensionRequest()
          .getShipment().getOrder().setExternalId(requisitionExtension.getRequisitionId());
      Requisition requisition = requisitionRepository.findOne(requisitionExtension.getRequisitionId());

      finalApprove(requisition, requisitionExtension, event);

      order = convertToOrder(event, requisition);
    } else {
      Order orderOrigin =
          ordersRepository.findByOrderCode(event.getShipmentExtensionRequest().getShipment().getOrder().getOrderCode());
      order = updateOrderLineItems(event.getShipmentExtensionRequest().getShipment().getOrder(),
          orderOrigin);
      order.setExternalId(orderOrigin.getExternalId());
    }
    // pre handle
    Set<UUID> skippedOrderableIds = getSkippedOrderLineItemIds(event.getShipmentExtensionRequest().getShipment());
    removeSkippedOrderLineItems(skippedOrderableIds, order, event);
    // set to SHIPPED
    order.updateStatus(OrderStatus.SHIPPED, new UpdateDetails(event.getFulfillUserId(), ZonedDateTime.now()));
    Order shipped = ordersRepository.saveAndFlush(order);

    fulfillOrder(event, shipped);
  }

  private void fulfillOrder(OrderFulfillmentSyncedEvent event, Order shipped) {
    OrderDto orderDto = orderDtoBuilder.build(shipped);
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(orderDto.getId());
    BeanUtils.copyProperties(orderDto, orderObjectReferenceDto);
    event.getShipmentExtensionRequest().getShipment().setOrder(orderObjectReferenceDto);
    doFulfillOrder(event, shipped);
  }

  private Order updateOrderLineItems(OrderObjectReferenceDto orderDto, Order orderOrigin) {
    List<OrderLineItemDto> orderLineItemDtos = orderDto.getOrderLineItems();
    List<OrderLineItem> lineItemsOrigin = orderOrigin.getOrderLineItems();
    orderLineItemDtos.stream()
        .filter(orderLineItemDto -> orderLineItemDto.getId() == null)
        .filter(orderLineItemDto -> !orderLineItemDto.isSkipped())
        .map(OrderLineItem::newInstance)
        .forEach(orderLineItem -> {
          orderLineItem.setOrder(orderOrigin);
          lineItemsOrigin.add(orderLineItem);
        });
    log.info("update orderId: {}, orderLineItem: {}", orderOrigin.getId(), lineItemsOrigin);
    return ordersRepository.saveAndFlush(orderOrigin);
  }

  private void finalApprove(Requisition requisition, RequisitionExtension requisitionExtension,
      OrderFulfillmentSyncedEvent event) {
    resetApprovedQuantity(requisition, event);
    // do approve
    requisition.approve(null, getOrderableDtoMap(requisition), Collections.emptyList(),
        event.getFinalApproveUserId());

    requisitionRepository.saveAndFlush(requisition);

    requisitionExtension.setIsApprovedByInternal(false);
    requisitionExtensionRepository.saveAndFlush(requisitionExtension);

    // todo siglusNotificationService.postApprove(buildBaseRequisitionDto(requisition));
  }

  private BasicRequisitionDto buildBaseRequisitionDto(Requisition requisition) {
    BasicRequisitionDto basicRequisitionDto = new BasicRequisitionDto();
    basicRequisitionDto.setId(requisition.getId());
    MinimalFacilityDto minimalFacilityDto = new MinimalFacilityDto();
    minimalFacilityDto.setId(requisition.getFacilityId());
    basicRequisitionDto.setFacility(minimalFacilityDto);
    BasicProgramDto basicProgramDto = new BasicProgramDto();
    basicProgramDto.setId(requisition.getProgramId());
    basicRequisitionDto.setProgram(basicProgramDto);
    basicRequisitionDto.setEmergency(requisition.getEmergency());
    BasicProcessingPeriodDto basicProcessingPeriodDto = new BasicProcessingPeriodDto();
    basicProcessingPeriodDto.setId(requisition.getProcessingPeriodId());
    basicRequisitionDto.setProcessingPeriod(basicProcessingPeriodDto);
    return basicRequisitionDto;
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
    // todo siglusNotificationService.postConvertToOrder(new ApproveRequisitionDto(requisition)); called rpc
    return createOrder(event);
  }

  private Order createOrder(OrderFulfillmentSyncedEvent event) {
    return ordersRepository.saveAndFlush(event.getConvertToOrderRequest().getFirstOrder());
    // TODO: notifiction of create order: FulfillmentNotificationService.sendOrderCreatedNotification() called rpc
  }

  private void releaseRequisitionsAsOrder(Requisition requisition, UUID supplierUserId, UUID supplierFacilityId) {
    requisition.release(supplierUserId);
    requisition.setSupplyingFacilityId(supplierFacilityId);
    requisitionRepository.saveAndFlush(requisition);
    // TODO: notifiction of release,  notificationService.postRelease() not exists
  }

  private void doFulfillOrder(OrderFulfillmentSyncedEvent event, Order shiped) {
    Shipment shipment = createSubOrderAndShipment(event.isSubOrder(),
        event.getShipmentExtensionRequest().getShipment(), event.getFulfillUserId(), shiped,
        event.isNeedConvertToOrder());
    if (event.isWithLocation()) {
      saveShipmentLineItemsExtensionWithLocation(event, shipment);
    }
    savePodExtension(shipment.getId(), event.getShipmentExtensionRequest());
    // todo siglusNotificationService.postConfirmShipment(event.getShipmentExtensionRequest().getShipment()); rpc
  }

  public void savePodExtension(UUID shipmentId, ShipmentExtensionRequest shipmentExtensionRequest) {
    ProofOfDelivery proofOfDelivery = siglusProofOfDeliveryRepository.findByShipmentId(shipmentId);
    UUID podId = proofOfDelivery.getId();
    ProofsOfDeliveryExtension proofsOfDeliveryExtension = ProofsOfDeliveryExtension
        .builder()
        .podId(podId)
        .conferredBy(shipmentExtensionRequest.getConferredBy())
        .preparedBy(shipmentExtensionRequest.getPreparedBy())
        .build();
    log.info("save pod extension when confirm shipment, shipmentId: {}", shipmentId);
    proofsOfDeliveryExtensionRepository.save(proofsOfDeliveryExtension);
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
      String locationCode = shipmentLineItemDto.getLocation().getLocationCode();
      String area = shipmentLineItemDto.getLocation().getArea();
      ShipmentLineItemsExtension shipmentLineItemsByLocation = ShipmentLineItemsExtension
          .builder()
          .shipmentLineItemId(lineItemId)
          .locationCode(locationCode)
          .area(area)
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
      return shipmentLineItemDto.getOrderable().getId() + "&" + shipmentLineItemDto.getQuantityShipped();
    }
    return shipmentLineItemDto.getLotId() + "&" + shipmentLineItemDto.getOrderable().getId()
        + "&" + shipmentLineItemDto.getQuantityShipped();
  }

  private String buildForUniqueKey(ShipmentLineItemDto shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLot()) {
      return shipmentLineItemDto.getOrderable().getId() + "&" + shipmentLineItemDto.getQuantityShipped();
    }
    return shipmentLineItemDto.getLot().getId() + "&" + shipmentLineItemDto.getOrderable().getId()
        + "&" + shipmentLineItemDto.getQuantityShipped();
  }

  private Shipment createSubOrderAndShipment(boolean isSubOrder, ShipmentDto shipmentDto, UUID fulfillUserId,
      Order shiped, boolean needConvertToOrder) {
    if (isSubOrder) {
      createSubOrder(shipmentDto, fulfillUserId, needConvertToOrder);
    }
    return createShipment(shipmentDto, fulfillUserId, shiped);
  }

  private Shipment createShipment(ShipmentDto shipmentDto, UUID fulfillUserId, Order shipedOrder) {
    nullIds(shipmentDto);
    shipmentDto.setShipDetails(new CreationDetails(fulfillUserId, ZonedDateTime.now()));
    Shipment shipment = Shipment.newInstance(shipmentDto, shipedOrder);

    Shipment saved = this.siglusShipmentRepository.saveAndFlush(shipment);
    ProofOfDelivery proofOfDelivery = ProofOfDelivery.newInstance(saved);
    this.proofOfDeliveryRepository.saveAndFlush(proofOfDelivery);
    return saved;
  }

  private void nullIds(ShipmentDto shipmentDto) {
    shipmentDto.setId(null);
    if (shipmentDto.lineItems() != null) {
      shipmentDto.lineItems().forEach((l) -> l.setId(null));
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
    ordersRepository.saveAndFlush(order);
    if (CollectionUtils.isEmpty(skippedLineItemIds)) {
      return;
    }
    List<OrderLineItemExtension> extensions = lineItemExtensionRepository
        .findByOrderLineItemIdIn(skippedLineItemIds);
    lineItemExtensionRepository.delete(extensions);

    event.getShipmentExtensionRequest().getShipment().lineItems().removeIf(lineItem ->
        skippedOrderableIds.contains(lineItem.getOrderable().getId()));
  }

  private void createSubOrder(ShipmentDto shipmentDto, UUID fulfillUserId, boolean needConvertToOrder) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    Map<UUID, List<ShipmentLineItem.Importer>> groupShipment = shipmentDto.getLineItems().stream()
        .collect(Collectors.groupingBy(lineItem -> lineItem.getOrderableIdentity().getId()));
    OrderObjectReferenceDto order = shipmentDto.getOrder();
    List<OrderLineItemDto> orderLineItems = order.getOrderLineItems();
    List<OrderLineItemDto> subOrderLineItems = getSubOrderLineItemDtos(skippedOrderLineItemIds,
        groupShipment, orderLineItems);
    createSubOrder(order, subOrderLineItems, fulfillUserId, needConvertToOrder);
  }

  public void createSubOrder(OrderObjectReferenceDto order,
      List<OrderLineItemDto> orderLineItemDtos, UUID fulfillUserId, boolean needConvertToOrder) {
    List<OrderExternal> externals = new ArrayList<>();
    // if order's external id is not found in orderExternalRepository, then it means this is a requisition id
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    String newOrderCode = null;
    OrderExternal newOrderExternal = null;
    if (external == null) {
      // need convert to order
      OrderExternal firstExternal = OrderExternal.builder()
          .requisitionId(order.getExternalId()).build();
      OrderExternal secondExternal = OrderExternal.builder()
          .requisitionId(order.getExternalId()).build();
      log.info("save order external : {}", Arrays.asList(firstExternal, secondExternal));
      externals.add(orderExternalRepository.saveAndFlush(firstExternal));
      externals.add(orderExternalRepository.saveAndFlush(secondExternal));
      updateExistOrderForSubOrder(order.getId(), externals.get(0).getId(),
          order.getOrderCode().concat("-" + 1), order.getStatus());
      newOrderCode = order.getOrderCode().concat("-" + 2);
      newOrderExternal = externals.get(1);
    } else {
      externals = orderExternalRepository
          .findByRequisitionId(external.getRequisitionId());
      OrderExternal newExternal = OrderExternal.builder()
          .requisitionId(external.getRequisitionId()).build();
      log.info("save new external : {}", newExternal);
      newOrderExternal = orderExternalRepository.saveAndFlush(newExternal);
      externals.add(newOrderExternal);
      newOrderCode = replaceLast(order.getOrderCode(), "-" + (externals.size() - 1),
          "-" + externals.size());
    }
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

    Order saved = saveOrder(order, fulfillUserId, newOrder);
    updateOrderExtension(orderLineItemDtos, saved);
  }

  private Order saveOrder(OrderObjectReferenceDto orderDto, UUID fulfillUserId, OrderDto order) {
    Order newOrder = Order.newInstance(order, new UpdateDetails(fulfillUserId, ZonedDateTime.now()));
    return ordersRepository.saveAndFlush(newOrder);
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

  private String replaceLast(String text, String regex, String replacement) {
    return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
  }


  private void updateExistOrderForSubOrder(UUID orderId,
      UUID externalId, String orderCode, OrderStatus orderStatus) {
    Order originOrder = ordersRepository.findOne(orderId);
    originOrder.setExternalId(externalId);
    originOrder.setOrderCode(orderCode);
    originOrder.setStatus(orderStatus);
    log.info("update exist order for subOrder: {}", originOrder);
    ordersRepository.saveAndFlush(originOrder);
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
        calculateSubOrderPartialFulfilledValue(groupShipment, subOrderLineItems, dto);
      }
    }
    return subOrderLineItems;
  }

  private void calculateSubOrderPartialFulfilledValue(Map<UUID, List<ShipmentLineItem.Importer>> groupShipment,
      List<OrderLineItemDto> subOrderLineItems, OrderLineItemDto dto) {
    dto.setId(null);
    if (groupShipment.containsKey(dto.getOrderable().getId())) {
      Long shippedValue = getShippedValue(groupShipment, dto.getOrderable().getId());
      if (dto.getPartialFulfilledQuantity() + shippedValue < dto.getOrderedQuantity()) {
        Long partialFulfilledQuantity = dto.getPartialFulfilledQuantity() + shippedValue;
        dto.setPartialFulfilledQuantity(partialFulfilledQuantity);
        subOrderLineItems.add(dto);
      }
    } else {
      subOrderLineItems.add(dto);
    }
  }

  private Long getShippedValue(Map<UUID, List<ShipmentLineItem.Importer>> groupShipment,
      UUID orderableId) {
    List<ShipmentLineItem.Importer> shipments = groupShipment.get(orderableId);
    Long shipmentValue = 0L;
    for (ShipmentLineItem.Importer shipment : shipments) {
      shipmentValue += shipment.getQuantityShipped();
    }
    return shipmentValue;
  }
}