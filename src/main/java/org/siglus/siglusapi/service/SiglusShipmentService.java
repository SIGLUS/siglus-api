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
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_LINE_ITEMS_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;
import static org.siglus.siglusapi.util.LocationUtil.getIfNonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.math.BigInteger;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.organicdesign.fp.tuple.Tuple2;
import org.organicdesign.fp.tuple.Tuple3;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.ShipmentLineItemsExtension;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.dto.StockCardReservedDto;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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

  private final StockCardSummariesService stockCardSummariesService;

  private final SiglusShipmentDraftService shipmentDraftService;

  private final SiglusShipmentDraftFulfillmentService shipmentDraftFulfillmentService;

  @Transactional
  public ShipmentDto createOrderAndShipment(boolean isSubOrder, ShipmentExtensionRequest shipmentExtensionRequest) {
    ShipmentDto shipmentDto = createOrderAndConfirmShipment(isSubOrder,
        shipmentExtensionRequest.getShipment(), false);
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
    // get available soh
    StockCardSummariesV2SearchParams v2SearchParams = new StockCardSummariesV2SearchParams();
    v2SearchParams.setFacilityId(shipmentExtensionRequest.getShipment().getOrder().getSupplyingFacility().getId());
    v2SearchParams.setProgramId(shipmentExtensionRequest.getShipment().getOrder().getProgram().getId());
    StockCardSummaries summaries = stockCardSummariesService.findStockCards(v2SearchParams);
    // get reserved soh
    Page<ShipmentDraftDto> shipmentDrafts = shipmentDraftFulfillmentService
            .getShipmentDraftByOrderId(shipmentExtensionRequest.getShipment().getOrder().getId());
    UUID shipmentDraftId = null;
    if (shipmentDrafts.getSize() > 0) {
      shipmentDraftId = shipmentDrafts.getContent().get(0).getId();
    }
    List<StockCardReservedDto> reservedDtos = shipmentDraftService
        .reservedCount(v2SearchParams.getFacilityId(), v2SearchParams.getProgramId(), shipmentDraftId);
    // check soh
    if (canNotFullFillShipmentQuantity(summaries, reservedDtos, shipmentExtensionRequest)) {
      throw new ValidationMessageException(new Message(SHIPMENT_LINE_ITEMS_INVALID));
    }
  }

  private boolean canNotFullFillShipmentQuantity(StockCardSummaries summaries,
                                              List<StockCardReservedDto> reservedDtos,
                                              ShipmentExtensionRequest shipmentExtensionRequest) {
    Map<Tuple2<UUID, UUID>, Integer> sohMap = summaries.getStockCardsForFulfillOrderables()
            .stream().collect(Collectors.toMap(
              summary -> Tuple2.of(summary.getOrderableId(), summary.getLotId()),
              StockCard::getStockOnHand
            ));
    Map<Tuple3<UUID, Integer, UUID>, BigInteger> reservedMap = reservedDtos
            .stream().collect(Collectors.toMap(
              dto -> Tuple3.of(dto.getOrderableId(), dto.getOrderableVersionNumber(), dto.getLotId()),
              StockCardReservedDto::getReserved
            ));
    return shipmentExtensionRequest.getShipment().lineItems().stream().anyMatch(item -> {
      int soh = 0;
      Tuple2<UUID, UUID> sohKey = Tuple2.of(item.getOrderable().getId(), item.getLotId());
      if (sohMap.containsKey(sohKey)) {
        soh = sohMap.get(sohKey);
      }
      int reserved = 0;
      Tuple3<UUID, Integer, UUID> reservedKey =
          Tuple3.of(item.getOrderable().getId(), item.getOrderable().getVersionNumber().intValue(), item.getLotId());
      if (reservedMap.containsKey(reservedKey)) {
        reserved = reservedMap.get(reservedKey).intValue();
      }
      return soh - reserved < item.getQuantityShipped().intValue();
    });
  }

  @Transactional
  public ShipmentDto createOrderAndShipmentByLocation(boolean isSubOrder,
      ShipmentExtensionRequest shipmentExtensionRequest) {
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
    saveToShipmentLineItemExtension(shipmentLineItems);
    savePodExtension(confirmedShipmentDto.getId(), shipmentExtensionRequest);
    return confirmedShipmentDto;
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
