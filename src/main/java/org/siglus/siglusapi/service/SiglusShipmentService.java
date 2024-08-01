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

import static java.util.stream.Collectors.toSet;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_EXPIRED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERIOD_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUB_ORDER_LINE_ITEM;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_CAN_NOT_CONFIRM_SHIPMENT;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_LINE_ITEMS_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;
import static org.siglus.siglusapi.util.LocationUtil.getIfNonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.ShipmentLineItem.Importer;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.ShipmentLineItemsExtension;
import org.siglus.siglusapi.domain.ShipmentsExtension;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.SiglusFefoDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusLotRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusShipmentService {

  private final OrderRepository orderRepository;

  private final OrderLineItemExtensionRepository lineItemExtensionRepository;

  private final ShipmentController shipmentController;

  private final SiglusOrderService siglusOrderService;

  private final OrderController orderController;

  private final ShipmentLineItemsExtensionRepository shipmentLineItemsExtensionRepository;

  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  private final PodExtensionRepository podExtensionRepository;

  private final ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  private final RequisitionService requisitionService;

  private final SiglusShipmentDraftService shipmentDraftService;

  private final SiglusShipmentDraftFulfillmentService shipmentDraftFulfillmentService;

  private final ShipmentsExtensionRepository shipmentsExtensionRepository;

  private final SiglusLotRepository siglusLotRepository;

  @Value("${shipment.fefo.index}")
  private double fefoIndex;

  @Transactional
  public ShipmentDto createOrderAndShipment(boolean isSubOrder, ShipmentExtensionRequest shipmentExtensionRequest) {
    ShipmentDto shipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment(), false);
    saveShipmentExtension(shipmentExtensionRequest.getShipment(), shipmentDto.getId());
    savePodExtension(shipmentDto.getId(), shipmentExtensionRequest);
    return shipmentDto;
  }

  public void checkFulfillOrderExpired(ShipmentExtensionRequest shipmentExtensionRequest) {
    ProcessingPeriodDto period = shipmentExtensionRequest.getShipment().getOrder().getProcessingPeriod();
    YearMonth orderYearMonth = YearMonth.of(period.getEndDate().getYear(), period.getEndDate().getMonth());
    UUID processingPeriodId = period.getId();
    ProcessingPeriodExtension processingPeriodExtension = processingPeriodExtensionRepository
        .findByProcessingPeriodId(processingPeriodId);
    if (processingPeriodExtension == null) {
      throw new NotFoundException(ERROR_PERIOD_NOT_FOUND);
    }
    List<YearMonth> calculatedFulfillOrderMonth = requisitionService
        .calculateFulfillOrderYearMonth(processingPeriodExtension);
    if (!calculatedFulfillOrderMonth.contains(orderYearMonth)) {
      throw new BusinessDataException(new Message(ERROR_ORDER_EXPIRED));
    }
  }

  public void validShipmentLineItemsDuplicated(ShipmentExtensionRequest shipmentExtensionRequest) {
    List<ShipmentLineItemDto> shipmentLineItems = shipmentExtensionRequest.getShipment().lineItems();
    Set<String> orderableLotIds = new HashSet<>();
    boolean isOrderableLotIdDuplicated = !shipmentLineItems.stream()
        .map(item -> {
          if (item.getLocation() != null && item.getLocation().getLocationCode() != null) {
            return item.getOrderable().getId() + "-" + item.getLotId() + "-" + item.getLocation().getLocationCode();
          }
          return item.getOrderable().getId() + "-" + item.getLotId();
        })
        .allMatch(orderableLotIds::add);
    if (isOrderableLotIdDuplicated) {
      throw new ValidationMessageException(new Message(SHIPMENT_LINE_ITEMS_INVALID));
    }
  }

  public void checkStockOnHandQuantity(ShipmentExtensionRequest shipmentExtensionRequest) {
    if (shipmentExtensionRequest.getShipment().getLineItems().stream()
        .anyMatch(item -> item.getQuantityShipped() == null || item.getQuantityShipped() < 0)) {
      throw new ValidationException(SHIPMENT_LINE_ITEMS_INVALID);
    }
    ShipmentDraftDto dto = new ShipmentDraftDto();
    dto.setLineItems(shipmentExtensionRequest.getShipment().lineItems());
    dto.setOrder(shipmentExtensionRequest.getShipment().getOrder());
    Page<ShipmentDraftDto> shipmentDrafts = shipmentDraftFulfillmentService
        .getShipmentDraftByOrderId(shipmentExtensionRequest.getShipment().getOrder().getId());
    UUID shipmentDraftId = null;
    if (shipmentDrafts.getSize() > 0) {
      shipmentDraftId = shipmentDrafts.getContent().get(0).getId();
    }
    shipmentDraftService.checkStockOnHandQuantity(shipmentDraftId, dto);
  }

  @Transactional
  public ShipmentDto createOrderAndShipmentByLocation(boolean isSubOrder,
      ShipmentExtensionRequest shipmentExtensionRequest) {
    shipmentDraftService.deleteShipmentDraftLineItemsExtensionByOrderId(
        shipmentExtensionRequest.getShipment().getOrder().getId());
    List<ShipmentLineItemDto> shipmentLineItemDtos = shipmentExtensionRequest.getShipment().lineItems();
    Multimap<String, ShipmentLineItemDto> uniqueKeyMap = ArrayListMultimap.create();
    shipmentLineItemDtos.forEach(shipmentLineItemDto -> {
      String uniqueKey = buildForUniqueKey(shipmentLineItemDto);
      uniqueKeyMap.put(uniqueKey, shipmentLineItemDto);
    });
    ShipmentDto confirmedShipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment(), true);
    fulfillLocationInfo(uniqueKeyMap, confirmedShipmentDto);
    List<ShipmentLineItemDto> shipmentLineItems = new ArrayList<>(uniqueKeyMap.values());
    saveShipmentExtension(shipmentExtensionRequest.getShipment(), confirmedShipmentDto.getId());
    saveToShipmentLineItemExtension(shipmentLineItems);
    savePodExtension(confirmedShipmentDto.getId(), shipmentExtensionRequest);
    return confirmedShipmentDto;
  }

  private void saveShipmentExtension(ShipmentDto shipmentDto, UUID shipmentId) {
    ShipmentsExtension shipmentsExtension = ShipmentsExtension.builder()
        .shipmentId(shipmentId)
        .isFefo(calcIsFefo(shipmentDto))
        .build();
    shipmentsExtensionRepository.save(shipmentsExtension);
  }

  public boolean calcIsFefo(ShipmentDto shipmentDto) {
    if (CollectionUtils.isEmpty(shipmentDto.lineItems())) {
      return false;
    }
    Map<UUID, List<SiglusFefoDto>> orderableIdToFefoDtoSortedMap = buildOrderableIdToFefoDtoSortedMap(shipmentDto);
    setQuantitySuggestedAndActualFefo(orderableIdToFefoDtoSortedMap);
    List<SiglusFefoDto> siglusFefoDtos = orderableIdToFefoDtoSortedMap.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
    long totalQuantityShipped = siglusFefoDtos.stream()
        .mapToLong(SiglusFefoDto::getQuantityShipped)
        .sum();
    long totalQuantityActualFefo = siglusFefoDtos.stream()
        .mapToLong(SiglusFefoDto::getQuantityActualFefo)
        .sum();
    if (totalQuantityShipped == 0L) {
      throw new BusinessDataException(new Message(ERROR_USER_CAN_NOT_CONFIRM_SHIPMENT));
    }
    return (double) totalQuantityActualFefo / totalQuantityShipped > fefoIndex;
  }

  private void setQuantitySuggestedAndActualFefo(Map<UUID, List<SiglusFefoDto>> orderableIdToFefoDtoSortedMap) {
    for (Entry<UUID, List<SiglusFefoDto>> entry : orderableIdToFefoDtoSortedMap.entrySet()) {
      List<SiglusFefoDto> fefoDtos = entry.getValue();
      long totalQuantityShippedByOrderable = fefoDtos.stream()
          .mapToLong(SiglusFefoDto::getQuantityShipped)
          .sum();
      for (int suggestedQuantityIndex = 0; suggestedQuantityIndex < fefoDtos.size(); suggestedQuantityIndex++) {
        SiglusFefoDto dto = fefoDtos.get(suggestedQuantityIndex);
        if (suggestedQuantityIndex == 0) {
          dto.setQuantitySuggested(dto.getStockOnHand());
        } else {
          long quantity = 0;
          for (int stockOnHandIndex = 0; stockOnHandIndex < suggestedQuantityIndex; stockOnHandIndex++) {
            quantity += fefoDtos.get(stockOnHandIndex).getStockOnHand();
          }
          dto.setQuantitySuggested(totalQuantityShippedByOrderable - quantity);
        }
        dto.setQuantityActualFefo(Math.max(0, Math.min(dto.getQuantityShipped(), dto.getQuantitySuggested())));
      }
    }
  }

  private Map<UUID, List<SiglusFefoDto>> buildOrderableIdToFefoDtoSortedMap(ShipmentDto shipmentDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDto.lineItems();
    lineItemDtos = lineItemDtos.stream()
        .filter(lineItemDto -> !ObjectUtils.isEmpty(lineItemDto.getLot()))
        .collect(Collectors.toList());
    Set<UUID> lotIds = lineItemDtos.stream()
        .map(lineItemDto -> lineItemDto.getLot().getId())
        .collect(toSet());
    Map<UUID, LocalDate> lotIdToExpiredDateMap = siglusLotRepository.findAllByIdIn(lotIds).stream()
        .collect(Collectors.toMap(Lot::getId, Lot::getExpirationDate));
    List<SiglusFefoDto> siglusFefoDtos = lineItemDtos.stream()
        .map(lineItemDto -> SiglusFefoDto.from(lineItemDto, lotIdToExpiredDateMap))
        .collect(Collectors.toList());
    Map<UUID, List<SiglusFefoDto>> orderableIdToFefoDtoMap = siglusFefoDtos.stream()
        .collect(Collectors.groupingBy(SiglusFefoDto::getOrderableId));
    Map<UUID, List<SiglusFefoDto>> orderableIdToFefoDtoSortedMap = new HashMap<>();
    orderableIdToFefoDtoMap.forEach((orderableId, dtos) -> {
      List<SiglusFefoDto> sortedDtos = dtos.stream()
          .sorted(Comparator.comparing(SiglusFefoDto::getExpirationDate))
          .collect(Collectors.toList());
      orderableIdToFefoDtoSortedMap.put(orderableId, sortedDtos);
    });
    return orderableIdToFefoDtoSortedMap;
  }

  private void fulfillLocationInfo(Multimap<String, ShipmentLineItemDto> uniqueKeyMap, ShipmentDto shipmentDto) {
    for (ShipmentLineItemDto lineItemDto : shipmentDto.lineItems()) {
      String newKey = buildForUniqueKey(lineItemDto);
      List<ShipmentLineItemDto> lineItemDtos = (List<ShipmentLineItemDto>) uniqueKeyMap.get(newKey);
      if (!CollectionUtils.isEmpty(lineItemDtos)) {
        lineItemDtos.forEach(m -> m.setId(lineItemDto.getId()));
      }
    }
  }

  private String buildForUniqueKey(ShipmentLineItemDto shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLot()) {
      return shipmentLineItemDto.getOrderable().getId().toString();
    }
    return shipmentLineItemDto.getLot().getId() + FieldConstants.SEPARATOR + shipmentLineItemDto.getOrderable().getId();
  }

  @Transactional
  public ShipmentDto createSubOrderAndShipmentForFc(ShipmentDto shipmentDto) {
    createSubOrder(shipmentDto, false);
    return createShipment(shipmentDto, false, true);
  }

  ShipmentDto createSubOrderAndShipment(boolean isSubOrder, ShipmentDto shipmentDto, boolean isByLocation) {
    if (isSubOrder) {
      createSubOrder(shipmentDto, true);
    }
    return createShipment(shipmentDto, isByLocation, false);
  }

  private ShipmentDto createOrderAndConfirmShipment(boolean isSubOrder, ShipmentDto shipmentDto,
      boolean isByLocation) {
    OrderDto orderDto = orderController.getOrder(shipmentDto.getOrder().getId(), null);
    validateOrderStatus(orderDto);
    if (siglusOrderService.needCloseOrder(orderDto)) {
      if (siglusOrderService.isSuborder(orderDto.getExternalId())) {
        throw new ValidationMessageException(SHIPMENT_ORDER_STATUS_INVALID);
      }
      isSubOrder = false;
    }
    updateOrderLineItems(shipmentDto.getOrder());
    return createSubOrderAndShipment(isSubOrder, shipmentDto, isByLocation);
  }

  private void validateOrderStatus(OrderDto orderDto) {
    if (orderDto.getStatus().equals(OrderStatus.CLOSED)) {
      throw new ValidationMessageException(SHIPMENT_ORDER_STATUS_INVALID);
    }
  }

  private void createSubOrder(ShipmentDto shipmentDto, boolean needValidate) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    Map<UUID, List<ShipmentLineItem.Importer>> groupShipment = shipmentDto.getLineItems().stream()
        .collect(Collectors.groupingBy(lineItem -> lineItem.getOrderableIdentity().getId()));
    OrderObjectReferenceDto order = shipmentDto.getOrder();
    List<OrderLineItemDto> orderLineItems = order.getOrderLineItems();
    List<OrderLineItemDto> subOrderLineItems = getSubOrderLineItemDtos(skippedOrderLineItemIds,
        groupShipment, orderLineItems);
    if (subOrderLineItems.isEmpty() && needValidate) {
      throw new ValidationMessageException(new Message(ERROR_SUB_ORDER_LINE_ITEM));
    } else if (!subOrderLineItems.isEmpty()) {
      siglusOrderService.createSubOrder(order, subOrderLineItems);
    }
  }

  private ShipmentDto createShipment(ShipmentDto shipmentDto, boolean isByLocation, boolean isFcRequest) {
    Set<UUID> skippedOrderLineItemIds = getSkippedOrderLineItemIds(shipmentDto);
    removeSkippedOrderLineItemsAndExtensions(skippedOrderLineItemIds, shipmentDto.getOrder().getId());
    Set<UUID> skippedOrderableIds = getSkippedOrderableIds(shipmentDto);
    shipmentDto.lineItems().removeIf(lineItem -> skippedOrderableIds.contains(lineItem.getOrderable().getId()));
    return shipmentController.createShipment(shipmentDto, isByLocation, isFcRequest);
  }

  private Set<UUID> getSkippedOrderableIds(ShipmentDto shipmentDto) {
    return shipmentDto.getOrder().getOrderLineItems().stream()
        .filter(OrderLineItemDto::isSkipped)
        .map(orderLineItemDto -> orderLineItemDto.getOrderable().getId())
        .collect(toSet());
  }

  private Set<UUID> getSkippedOrderLineItemIds(ShipmentDto shipmentDto) {
    return shipmentDto.getOrder().getOrderLineItems().stream()
        .filter(OrderLineItemDto::isSkipped)
        .map(OrderLineItemDto::getId)
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private void removeSkippedOrderLineItemsAndExtensions(Set<UUID> skippedOrderLineItemIds, UUID orderId) {
    if (CollectionUtils.isEmpty(skippedOrderLineItemIds)) {
      return;
    }
    Order order = orderRepository.findOne(orderId);
    order.getOrderLineItems().removeIf(orderLineItem -> skippedOrderLineItemIds.contains(orderLineItem.getId()));
    orderRepository.save(order);
    List<OrderLineItemExtension> extensions = lineItemExtensionRepository.findByOrderLineItemIdIn(
        skippedOrderLineItemIds);
    lineItemExtensionRepository.delete(extensions);
  }

  private List<OrderLineItemDto> getSubOrderLineItemDtos(Set<UUID> skippedOrderLineItemIds,
      Map<UUID, List<Importer>> groupShipment, List<OrderLineItemDto> orderLineItems) {
    List<OrderLineItemDto> subOrderLineItems = new ArrayList<>();
    for (OrderLineItemDto dto : orderLineItems) {
      if (!skippedOrderLineItemIds.contains(dto.getId())
          && dto.getOrderedQuantity() != null && dto.getOrderedQuantity() > 0) {
        calculateSubOrderPartialFulfilledValue(groupShipment, subOrderLineItems, dto);
      }
    }
    return subOrderLineItems;
  }

  public void calculateSubOrderPartialFulfilledValue(Map<UUID, List<Importer>> groupShipment,
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

  private void updateOrderLineItems(OrderObjectReferenceDto orderDto) {
    Order order = orderRepository.findOne(orderDto.getId());
    List<OrderLineItemDto> orderLineItemDtos = orderDto.getOrderLineItems();

    List<OrderLineItem> original = order.getOrderLineItems();

    orderLineItemDtos.stream()
        .filter(orderLineItemDto -> orderLineItemDto.getId() == null)
        .filter(orderLineItemDto -> !orderLineItemDto.isSkipped())
        .map(OrderLineItem::newInstance)
        .forEach(orderLineItem -> {
          orderLineItem.setOrder(order);
          original.add(orderLineItem);
        });

    log.info("update orderId: {}, orderLineItem: {}", order.getId(), original);
    orderRepository.save(order);
  }

  private void savePodExtension(UUID shipmentId, ShipmentExtensionRequest shipmentExtensionRequest) {
    ProofOfDelivery proofOfDelivery = siglusProofOfDeliveryRepository.findByShipmentId(shipmentId);
    UUID podId = proofOfDelivery.getId();
    PodExtension podExtension = PodExtension
        .builder()
        .podId(podId)
        .conferredBy(shipmentExtensionRequest.getConferredBy())
        .preparedBy(shipmentExtensionRequest.getPreparedBy())
        .build();
    log.info("save pod extension when confirm shipment, shipmentId: {}", shipmentId);
    podExtensionRepository.save(podExtension);
  }

  public void mergeShipmentLineItems(ShipmentDto shipmentDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDto.lineItems();
    List<ShipmentLineItemDto> newLineItemDtos = new ArrayList<>();
    lineItemDtos.forEach(lineItem -> {
      ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
      BeanUtils.copyProperties(lineItem, shipmentLineItemDto);
      newLineItemDtos.add(shipmentLineItemDto);
    });
    Map<String, ShipmentLineItemDto> uniqueKeyToShipmentDto = new HashMap<>();
    newLineItemDtos.forEach(shipmentLineItem -> {
      String key = buildForUniqueKey(shipmentLineItem);
      if (uniqueKeyToShipmentDto.containsKey(key)) {
        ShipmentLineItemDto shipmentLineItemDto = uniqueKeyToShipmentDto.get(key);
        Long quantityShipped = shipmentLineItem.getQuantityShipped() + shipmentLineItemDto.getQuantityShipped();
        if (null != shipmentLineItemDto.getId()) {
          shipmentLineItemDto.setQuantityShipped(quantityShipped);
          uniqueKeyToShipmentDto.put(key, shipmentLineItemDto);
        } else {
          shipmentLineItem.setQuantityShipped(quantityShipped);
          uniqueKeyToShipmentDto.put(key, shipmentLineItem);
        }
      } else {
        uniqueKeyToShipmentDto.put(key, shipmentLineItem);
      }
    });
    shipmentDto.setLineItems(new ArrayList<>(uniqueKeyToShipmentDto.values()));
  }

  private void saveToShipmentLineItemExtension(List<ShipmentLineItemDto> shipmentLineItems) {
    List<ShipmentLineItemsExtension> shipmentLineItemsByLocations = Lists.newArrayList();
    shipmentLineItems.forEach(shipmentLineItemDto -> {
      UUID lineItemId = shipmentLineItemDto.getId();
      ShipmentLineItemsExtension shipmentLineItemsByLocation = ShipmentLineItemsExtension
          .builder()
          .shipmentLineItemId(lineItemId)
          .locationCode(getIfNonNull(LocationDto::getLocationCode, shipmentLineItemDto.getLocation()))
          .area(getIfNonNull(LocationDto::getArea, shipmentLineItemDto.getLocation()))
          .quantityShipped(shipmentLineItemDto.getQuantityShipped().intValue())
          .build();
      shipmentLineItemsByLocations.add(shipmentLineItemsByLocation);
    });
    log.info("create shipment line item by location, size: {}", shipmentLineItemsByLocations.size());
    shipmentLineItemsExtensionRepository.save(shipmentLineItemsByLocations);
  }
}
