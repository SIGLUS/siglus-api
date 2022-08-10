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

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.LocationDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemsExtension;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class SiglusShipmentDraftService {

  @Autowired
  private SiglusShipmentDraftFulfillmentService siglusShipmentDraftFulfillmentService;

  @Autowired
  private ShipmentDraftController draftController;

  @Autowired
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private OrderLineItemRepository orderLineItemRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private SiglusOrderService siglusOrderService;

  @Autowired
  private FacilityLocationsRepository facilityLocationsRepository;

  @Autowired
  private ShipmentDraftLineItemsExtensionRepository shipmentDraftLineItemsExtensionRepository;

  private ShipmentDraftDto draftDto;

  @Transactional
  public ShipmentDraftDto createShipmentDraft(ShipmentDraftDto draftDto) {
    return draftController.createShipmentDraft(draftDto);
  }

  @Transactional
  public ShipmentDraftDto updateShipmentDraft(UUID id, ShipmentDraftDto draftDto) {
    updateOrderLineItemExtension(draftDto);
    return draftController.updateShipmentDraft(id, draftDto);
  }

  @Transactional
  public void deleteShipmentDraft(UUID id) {
    deleteOrderLineItemAndInitialedExtension(getDraftOrder(id));
    draftController.deleteShipmentDraft(id);
  }

  public ShipmentDraftDto getShipmentDraftByLocation(UUID shipmentDraftId) {
    ShipmentDraftDto shipmentDraftDto = siglusShipmentDraftFulfillmentService.searchShipmentDraft(shipmentDraftId);
    return fulfillShipmentDraftByLocation(shipmentDraftDto);
  }

  @Transactional
  public ShipmentDraftDto updateShipmentDraftByLocation(UUID shipmentDraftId, ShipmentDraftDto draftDto) {
    updateOrderLineItemExtension(draftDto);
    ShipmentDraftDto shipmentDraftDto = draftController.updateShipmentDraft(shipmentDraftId, draftDto);
    updateLineItemLocation(draftDto);
    return fulfillShipmentDraftByLocation(shipmentDraftDto);
  }

  @Transactional
  public void deleteShipmentDraftByLocation(UUID shipmentDraftId) {
    deleteOrderLineItemAndInitialedExtension(getDraftOrder(shipmentDraftId));
    deleteShipmentDraftLocation(draftDto);
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

  private Order getDraftOrder(UUID draftId) {
    draftDto = siglusShipmentDraftFulfillmentService
        .searchShipmentDraft(draftId);
    return orderRepository.findOne(draftDto.getOrder().getId());
  }

  private void updateOrderLineItemExtension(ShipmentDraftDto draftDto) {
    Set<UUID> addedLineItemIds = siglusOrderService.updateOrderLineItems(draftDto);

    List<OrderLineItemDto> lineItems = draftDto.getOrder().getOrderLineItems();
    Set<UUID> lineItemIds = lineItems.stream().map(OrderLineItemDto::getId)
        .collect(Collectors.toSet());
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
    List<ShipmentLineItemDto> newShipmentLineItemDto = Lists.newArrayList();
    List<UUID> lineItemIds = shipmentLineItemDtos.stream().map(ShipmentLineItemDto::getId).collect(Collectors.toList());

    List<ShipmentDraftLineItemsExtension> lineItemsExtensionList = shipmentDraftLineItemsExtensionRepository
        .findByShipmentDraftLineItemIdIn(lineItemIds);
    List<UUID> locationIds = lineItemsExtensionList.stream().map(ShipmentDraftLineItemsExtension::getLocationId)
        .collect(Collectors.toList());
    List<FacilityLocations> locationManagementList = facilityLocationsRepository.findByIdIn(locationIds);

    Map<UUID, UUID> lineItemIdToLocationIdMap = new HashMap<>();
    lineItemsExtensionList.forEach(lineItemExtension -> {
      lineItemIdToLocationIdMap.put(lineItemExtension.getShipmentDraftLineItemId(), lineItemExtension.getLocationId());
    });
    Map<UUID, String> locationIdToCodeMap = new HashMap<>();
    locationManagementList.forEach(locationManagement -> {
      locationIdToCodeMap.put(locationManagement.getId(), locationManagement.getLocationCode());
    });
    shipmentLineItemDtos.forEach(lineItemDto -> {
      UUID locationId = lineItemIdToLocationIdMap.get(lineItemDto.getId());
      String locationCode = locationIdToCodeMap.get(locationId);
      ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
      BeanUtils.copyProperties(lineItemDto, shipmentLineItemDto);
      LocationDto locationDto = LocationDto
          .builder()
          .id(locationId)
          .locationCode(locationCode)
          .build();
      shipmentLineItemDto.setLocation(locationDto);
      newShipmentLineItemDto.add(shipmentLineItemDto);
    });
    shipmentDraftDto.setLineItems(newShipmentLineItemDto);
    return shipmentDraftDto;
  }

  private void updateLineItemLocation(ShipmentDraftDto shipmentDraftDto) {
    deleteShipmentDraftLocation(shipmentDraftDto);
    saveLineItemLocation(shipmentDraftDto);
  }

  private void deleteShipmentDraftLocation(ShipmentDraftDto shipmentDraftDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDraftDto.lineItems();
    List<UUID> lineItemIds = lineItemDtos.stream().map(ShipmentLineItemDto::getId).collect(Collectors.toList());
    log.info("shipment draft line items by location; delete with line item ids: {}", lineItemIds);
    shipmentDraftLineItemsExtensionRepository.deleteByShipmentDraftLineItemIdIn(lineItemIds);
  }

  private void saveLineItemLocation(ShipmentDraftDto shipmentDraftDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDraftDto.lineItems();
    List<ShipmentDraftLineItemsExtension> shipmentDraftLineItemsByLocationList = lineItemDtos.stream()
            .map(lineItemDto -> ShipmentDraftLineItemsExtension
                .builder()
                .locationId(lineItemDto.getLocation().getId())
                .shipmentDraftLineItemId(lineItemDto.getId())
                .build())
        .collect(Collectors.toList());
    List<UUID> id = shipmentDraftLineItemsByLocationList.stream().map(ShipmentDraftLineItemsExtension::getId)
        .collect(Collectors.toList());
    log.info("shipment draft line items by location; save with line item ids: {}", id);
    shipmentDraftLineItemsExtensionRepository.save(shipmentDraftLineItemsByLocationList);
  }
}
