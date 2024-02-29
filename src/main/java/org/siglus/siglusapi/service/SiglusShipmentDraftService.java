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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toMap;
import static org.siglus.siglusapi.i18n.MessageKeys.SHIPMENT_LINE_ITEMS_INVALID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.ShipmentLineItem.Importer;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.organicdesign.fp.tuple.Tuple2;
import org.organicdesign.fp.tuple.Tuple3;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemsExtension;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsRepository;
import org.siglus.siglusapi.repository.dto.StockCardReservedDto;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusShipmentDraftService {

  private final SiglusShipmentDraftFulfillmentService siglusShipmentDraftFulfillmentService;
  private final ShipmentDraftController draftController;
  private final OrderLineItemExtensionRepository lineItemExtensionRepository;
  private final OrderLineItemRepository orderLineItemRepository;
  private final OrderRepository orderRepository;
  private final SiglusOrderService siglusOrderService;
  private final ShipmentDraftLineItemsExtensionRepository shipmentDraftLineItemsExtensionRepository;

  private final ShipmentDraftLineItemsRepository shipmentDraftLineItemsRepository;

  private final StockCardSummariesService stockCardSummariesService;

  @Transactional
  public ShipmentDraftDto createShipmentDraft(ShipmentDraftDto draftDto) {
    checkStockOnHandQuantity(null, draftDto);
    return draftController.createShipmentDraft(draftDto);
  }

  @Transactional
  public ShipmentDraftDto updateShipmentDraft(UUID id, ShipmentDraftDto draftDto) {
    checkStockOnHandQuantity(id, draftDto);
    updateOrderLineItemsWithExtension(draftDto);
    return draftController.updateShipmentDraft(id, draftDto);
  }

  @Transactional
  public void deleteShipmentDraft(UUID id) {
    deleteOrderLineItemAndInitialedExtension(getDraftOrder(id));
    draftController.deleteShipmentDraft(id);
  }

  public ShipmentDraftDto getShipmentDraftByLocation(UUID orderId) {
    Page<ShipmentDraftDto> shipmentDraftDto = siglusShipmentDraftFulfillmentService.getShipmentDraftByOrderId(orderId);
    return fulfillShipmentDraftByLocation(shipmentDraftDto.getContent().get(0));
  }

  @Transactional
  public ShipmentDraftDto updateShipmentDraftByLocation(UUID shipmentDraftId, ShipmentDraftDto draftDto) {
    checkStockOnHandQuantity(shipmentDraftId, draftDto);
    Multimap<String, ShipmentLineItemDto> uniqueKeyMap = ArrayListMultimap.create();
    draftDto.lineItems().forEach(shipmentLineItemDto -> {
      if (shipmentLineItemDto.getLotId() == null && shipmentLineItemDto.getId() != null) {
        shipmentLineItemDto.setId(null);
      }
      String uniqueKey = buildUniqueKey(shipmentLineItemDto);
      uniqueKeyMap.put(uniqueKey, shipmentLineItemDto);
    });
    updateOrderLineItemsWithExtension(draftDto);
    ShipmentDraftDto shipmentDraftDtoAfterMerge = mergeShipmentDraftLineItems(draftDto);
    ShipmentDraftDto updatedShipmentDraftDto = draftController.updateShipmentDraft(shipmentDraftId,
        shipmentDraftDtoAfterMerge);
    updatedShipmentDraftDto.lineItems().forEach(lineItemDto -> {
      String newKey = buildUniqueKey(lineItemDto);
      List<ShipmentLineItemDto> shipmentLineItemDtos = (List<ShipmentLineItemDto>) uniqueKeyMap.get(newKey);
      if (!CollectionUtils.isEmpty(shipmentLineItemDtos)) {
        shipmentLineItemDtos.forEach(m -> m.setId(lineItemDto.getId()));
      }
    });
    draftDto.setLineItems(new ArrayList<>(uniqueKeyMap.values()));
    updateShipmentDraftLineItemsExtension(draftDto);
    return draftDto;
  }

  private String buildUniqueKey(ShipmentLineItemDto shipmentLineItemDto) {
    if (null == shipmentLineItemDto.getLot()) {
      return shipmentLineItemDto.getOrderable().getId().toString();
    }
    return shipmentLineItemDto.getLot().getId() + "&" + shipmentLineItemDto.getOrderable().getId();
  }

  @Transactional
  public void deleteShipmentDraftByLocation(UUID shipmentDraftId) {
    deleteOrderLineItemAndInitialedExtension(getDraftOrder(shipmentDraftId));
    deleteShipmentDraftLineItemsExtension(shipmentDraftId);
    draftController.deleteShipmentDraft(shipmentDraftId);
  }

  public void deleteOrderLineItemAndInitialedExtension(Order order) {
    List<OrderLineItemExtension> extensions = lineItemExtensionRepository
        .findByOrderId(order.getId());
    deleteAddedOrderLineItemsInOrder(extensions, order);
    Set<OrderLineItemExtension> addedLineItemExtensions = deleteAddedLineItemsInExtension(
        extensions);
    initialedExtension(extensions, addedLineItemExtensions);
  }

  public List<StockCardReservedDto> reservedCount(UUID facilityId,
          UUID programId, UUID shipmentDraftId, List<ShipmentLineItemDto> lineItems) {
    List<StockCardReservedDto> allReserved = queryReservedCount(facilityId, programId, shipmentDraftId);
    if (lineItems == null || lineItems.isEmpty()) {
      return allReserved;
    }
    Map<Tuple3<UUID, Integer, UUID>, Integer> reservedMap = allReserved
            .stream()
            .collect(Collectors.toMap(
                item -> Tuple3.of(item.getOrderableId(), item.getOrderableVersionNumber(), item.getLotId()),
                StockCardReservedDto::getReserved));
    return lineItems.stream().map(item -> {
      Tuple3<UUID, Integer, UUID> key = Tuple3.of(item.getOrderable().getId(),
              item.getOrderable().getVersionNumber().intValue(), item.getLotId());
      return StockCardReservedDto.builder()
              .orderableId(key._1())
              .orderableVersionNumber(key._2())
              .lotId(key._3())
              .reserved(reservedMap.getOrDefault(key, 0))
              .build();
    }).collect(Collectors.toList());
  }

  public void checkStockOnHandQuantity(UUID shipmentDraftId, ShipmentDraftDto draftDto) {
    // get available soh
    StockCardSummariesV2SearchParams v2SearchParams = new StockCardSummariesV2SearchParams();
    v2SearchParams.setFacilityId(draftDto.getOrder().getSupplyingFacility().getId());
    v2SearchParams.setProgramId(draftDto.getOrder().getProgram().getId());
    StockCardSummaries summaries = stockCardSummariesService.findStockCards(v2SearchParams);
    // get reserved soh
    List<StockCardReservedDto> reservedDtos =
            queryReservedCount(v2SearchParams.getFacilityId(), v2SearchParams.getProgramId(), shipmentDraftId);
    // check soh
    if (canNotFullFillShipmentQuantity(summaries, reservedDtos, draftDto)) {
      throw new ValidationMessageException(new Message(SHIPMENT_LINE_ITEMS_INVALID));
    }
  }

  private List<StockCardReservedDto> queryReservedCount(UUID facilityId, UUID programId, UUID shipmentDraftId) {
    List<StockCardReservedDto> reservedDtos;
    if (shipmentDraftId == null) {
      reservedDtos = shipmentDraftLineItemsRepository.reservedCount(facilityId, programId);
    } else {
      reservedDtos = shipmentDraftLineItemsRepository.reservedCount(facilityId, programId, shipmentDraftId);
    }
    return reservedDtos;
  }

  private boolean canNotFullFillShipmentQuantity(StockCardSummaries summaries,
                                                 List<StockCardReservedDto> reservedDtos,
                                                 ShipmentDraftDto draftDto) {
    Map<Tuple2<UUID, UUID>, Integer> sohMap = summaries.getStockCardsForFulfillOrderables()
        .stream().collect(Collectors.toMap(
          summary -> Tuple2.of(summary.getOrderableId(), summary.getLotId()),
          StockCard::getStockOnHand
        ));
    Map<Tuple3<UUID, Integer, UUID>, Integer> reservedMap = reservedDtos
        .stream().collect(Collectors.toMap(
          dto -> Tuple3.of(dto.getOrderableId(), dto.getOrderableVersionNumber(), dto.getLotId()),
          StockCardReservedDto::getReserved
        ));
    return draftDto.lineItems().stream().anyMatch(item -> {
      Tuple2<UUID, UUID> sohKey = Tuple2.of(item.getOrderable().getId(), item.getLotId());
      int soh = sohMap.getOrDefault(sohKey, 0);
      Tuple3<UUID, Integer, UUID> reservedKey = Tuple3.of(item.getOrderable().getId(),
                      item.getOrderable().getVersionNumber().intValue(),
                      item.getLotId());
      int reserved = reservedMap.getOrDefault(reservedKey, 0);
      return soh - reserved < item.getQuantityShipped().intValue();
    });
  }

  private Order getDraftOrder(UUID draftId) {
    ShipmentDraftDto draftDto = siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId);
    return orderRepository.findOne(draftDto.getOrder().getId());
  }

  private void updateOrderLineItemsWithExtension(ShipmentDraftDto draftDto) {
    Set<UUID> addedLineItemIds = siglusOrderService.updateOrderLineItems(draftDto);

    List<OrderLineItemDto> lineItems = draftDto.getOrder().getOrderLineItems();
    Set<UUID> lineItemIds = lineItems.stream().map(OrderLineItemDto::getId).collect(Collectors.toSet());
    Map<UUID, OrderLineItemExtension> lineItemExtensionMap = lineItemExtensionRepository
        .findByOrderLineItemIdIn(lineItemIds).stream()
        .collect(toMap(OrderLineItemExtension::getOrderLineItemId, extension -> extension));
    Map<UUID, Boolean> lineItemSkippedMap = lineItems.stream()
        .collect(toMap(OrderLineItemDto::getId, OrderLineItemDto::isSkipped));
    List<OrderLineItemExtension> extensionsToUpdate = newArrayList();
    UUID orderId = draftDto.getOrder().getId();
    lineItems.forEach(lineItem -> {
      OrderLineItemExtension extension = lineItemExtensionMap.get(lineItem.getId());
      boolean skipped = lineItemSkippedMap.get(lineItem.getId());
      lineItem.setSkipped(skipped);
      if (null != extension) {
        extension.setSkipped(lineItem.isSkipped());
        extension.setOrderId(orderId);
        extensionsToUpdate.add(extension);
      } else {
        extensionsToUpdate.add(
            OrderLineItemExtension.builder()
                .orderId(orderId)
                .orderLineItemId(lineItem.getId())
                .skipped(lineItem.isSkipped())
                .added(addedLineItemIds.contains(lineItem.getId()))
                .partialFulfilledQuantity(lineItem.getPartialFulfilledQuantity())
                .build());
      }
    });
    log.info("save order line item extension with orderId: {}", orderId);
    lineItemExtensionRepository.save(extensionsToUpdate);
  }

  private void initialedExtension(List<OrderLineItemExtension> extensions,
      Set<OrderLineItemExtension> addedLineItemExtensions) {
    extensions.removeAll(addedLineItemExtensions);
    extensions.forEach(extension -> extension.setSkipped(false));
    if (!extensions.isEmpty()) {
      lineItemExtensionRepository.save(extensions);
    }
  }

  private void deleteAddedOrderLineItemsInOrder(List<OrderLineItemExtension> extensions,
      Order order) {
    Set<UUID> addedIds = extensions.stream()
        .filter(OrderLineItemExtension::isAdded)
        .map(OrderLineItemExtension::getOrderLineItemId)
        .collect(Collectors.toSet());

    log.info("orderId: {}, deleteAddedOrderLineItemIds: {}", order.getId(), addedIds);

    if (!CollectionUtils.isEmpty(addedIds)) {
      List<OrderLineItem> deleteLineItems = orderLineItemRepository
          .findByOrderId(order.getId())
          .stream().filter(orderLineItem -> addedIds.contains(orderLineItem.getId()))
          .collect(Collectors.toList());
      List<OrderLineItem> orderLineItems = orderLineItemRepository
          .findByOrderId(order.getId())
          .stream().filter(orderLineItem -> !addedIds.contains(orderLineItem.getId()))
          .collect(Collectors.toList());
      try {
        order.getOrderLineItems().clear();
        order.getOrderLineItems().addAll(orderLineItems);
      } catch (Exception e) {
        order.setOrderLineItems(orderLineItems);
      }
      orderLineItemRepository.delete(deleteLineItems);
    }
  }

  private Set<OrderLineItemExtension> deleteAddedLineItemsInExtension(
      List<OrderLineItemExtension> extensions) {
    Set<OrderLineItemExtension> addedExtension = extensions.stream()
        .filter(OrderLineItemExtension::isAdded)
        .collect(Collectors.toSet());

    if (!addedExtension.isEmpty()) {
      log.info("delete extension line item: {}", addedExtension);
      lineItemExtensionRepository.delete(addedExtension);
    }
    return addedExtension;
  }

  private ShipmentDraftDto fulfillShipmentDraftByLocation(ShipmentDraftDto shipmentDraftDto) {
    List<ShipmentLineItemDto> shipmentLineItemDtos = shipmentDraftDto.lineItems();
    List<UUID> lineItemIds = shipmentLineItemDtos.stream().map(ShipmentLineItemDto::getId).collect(Collectors.toList());
    List<ShipmentDraftLineItemsExtension> lineItemsExtensionList = shipmentDraftLineItemsExtensionRepository
        .findByShipmentDraftLineItemIdIn(lineItemIds);
    List<ShipmentLineItemDto> lineItems = new ArrayList<>();
    shipmentLineItemDtos.forEach(shipmentLineItemDto -> {
      UUID shipmentDraftLineItemId = shipmentLineItemDto.getId();
      lineItemsExtensionList.forEach(lineItemExtension -> {
        if (lineItemExtension.getShipmentDraftLineItemId().equals(shipmentDraftLineItemId)) {
          ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
          BeanUtils.copyProperties(shipmentLineItemDto, lineItemDto);
          LocationDto locationDto = LocationDto
              .builder()
              .locationCode(lineItemExtension.getLocationCode())
              .area(lineItemExtension.getArea())
              .build();
          lineItemDto.setLocation(locationDto);
          lineItemDto.setQuantityShipped(lineItemExtension.getQuantityShipped() == null
              ? null : lineItemExtension.getQuantityShipped().longValue());
          lineItems.add(lineItemDto);
        }
      });
    });
    shipmentDraftDto.setLineItems(lineItems);
    return shipmentDraftDto;
  }

  private void deleteShipmentDraftLineItemsExtension(UUID shipmentDraftId) {
    ShipmentDraftDto shipmentDraft = draftController.getShipmentDraft(shipmentDraftId, Collections.emptySet());
    List<UUID> shipmentDraftLineItemIds = shipmentDraft.getLineItems()
        .stream()
        .map(Importer::getId)
        .collect(Collectors.toList());
    log.info("delete shipmentDraftLineItemsExtension, shipmentDraftId: {}", shipmentDraftId);
    shipmentDraftLineItemsExtensionRepository.deleteByShipmentDraftLineItemIdIn(shipmentDraftLineItemIds);
  }

  private void updateShipmentDraftLineItemsExtension(ShipmentDraftDto shipmentDraftDto) {
    deleteShipmentDraftLineItemsExtension(shipmentDraftDto.getId());

    List<ShipmentLineItemDto> lineItemDtos = shipmentDraftDto.lineItems();
    List<ShipmentDraftLineItemsExtension> shipmentDraftLineItemsByLocationList = lineItemDtos.stream()
        .map(lineItemDto -> ShipmentDraftLineItemsExtension.builder()
            .locationCode(lineItemDto.getLocation() == null ? null : lineItemDto.getLocation().getLocationCode())
            .area(lineItemDto.getLocation() == null ? null : lineItemDto.getLocation().getArea())
            .shipmentDraftLineItemId(lineItemDto.getId())
            .quantityShipped(
                lineItemDto.getQuantityShipped() == null ? null : lineItemDto.getQuantityShipped().intValue())
            .build())
        .collect(Collectors.toList());
    log.info("save shipmentDraftLineItemsExtension, shipmentDraftDto: {}", shipmentDraftDto);
    shipmentDraftLineItemsExtensionRepository.save(shipmentDraftLineItemsByLocationList);
  }

  private ShipmentDraftDto mergeShipmentDraftLineItems(ShipmentDraftDto shipmentDraftDto) {
    List<ShipmentLineItemDto> newLineItemDtos = new ArrayList<>();
    shipmentDraftDto.lineItems().forEach(lineItem -> {
      ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
      BeanUtils.copyProperties(lineItem, shipmentLineItemDto);
      newLineItemDtos.add(shipmentLineItemDto);
    });
    Map<String, ShipmentLineItemDto> uniqueKeyToShipmentDto = new HashMap<>();
    newLineItemDtos.forEach(shipmentLineItem -> {
      String key = buildUniqueKey(shipmentLineItem);
      if (uniqueKeyToShipmentDto.containsKey(key)) {
        ShipmentLineItemDto shipmentLineItemDto = uniqueKeyToShipmentDto.get(key);
        Long quantityShipped = getQuantity(shipmentLineItem.getQuantityShipped())
            + getQuantity(shipmentLineItemDto.getQuantityShipped());
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
    ShipmentDraftDto shipmentDraftDtoAfterMerge = new ShipmentDraftDto();
    BeanUtils.copyProperties(shipmentDraftDto, shipmentDraftDtoAfterMerge);
    shipmentDraftDtoAfterMerge.setLineItems(new ArrayList<>(uniqueKeyToShipmentDto.values()));
    return shipmentDraftDtoAfterMerge;
  }

  private long getQuantity(Long value) {
    return value == null ? 0 : value;
  }
}
