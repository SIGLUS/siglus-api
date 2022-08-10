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
import org.siglus.siglusapi.domain.LocationManagement;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemsByLocation;
import org.siglus.siglusapi.repository.LocationManagementRepository;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemsByLocationRepository;
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
  private LocationManagementRepository locationManagementRepository;

  @Autowired
  private ShipmentDraftLineItemsByLocationRepository shipmentDraftLineItemsByLocationRepository;

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

  public ShipmentDraftDto getShipmentDraftByLocation(UUID orderId) {
    ShipmentDraftDto shipmentDraftDto = siglusShipmentDraftFulfillmentService.searchShipmentDraft(orderId);
    return fulfillShipmentDraftByLocation(shipmentDraftDto);
  }

  @Transactional
  public ShipmentDraftDto updateShipmentDraftByLocation(UUID id, ShipmentDraftDto draftDto) {
    updateOrderLineItemExtension(draftDto);
    ShipmentDraftDto shipmentDraftDto = draftController.updateShipmentDraft(id, draftDto);
    updateLineItemLocation(draftDto);
    return fulfillShipmentDraftByLocation(shipmentDraftDto);
  }

  @Transactional
  public void deleteShipmentDraftByLocation(UUID draftId) {
    deleteOrderLineItemAndInitialedExtension(getDraftOrder(draftId));
    deleteShipmentDraftLocation(draftDto);
    draftController.deleteShipmentDraft(draftId);
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

    List<ShipmentDraftLineItemsByLocation> lineItemsByLocationList = shipmentDraftLineItemsByLocationRepository
        .findByShipmentDraftLineItemIdIn(lineItemIds);
    List<UUID> locationIds = lineItemsByLocationList.stream().map(ShipmentDraftLineItemsByLocation::getLocationId)
        .collect(Collectors.toList());
    List<LocationManagement> locationManagementList = locationManagementRepository.findByIdIn(locationIds);

    shipmentLineItemDtos.forEach(lineItemDto -> {
      lineItemsByLocationList.forEach(lineItemsByLocation -> {
        if (lineItemDto.getId().equals(lineItemsByLocation.getShipmentDraftLineItemId())) {
          LocationManagement locationManagementDto = locationManagementList.stream()
              .filter(locationManagement -> locationManagement.getId().equals(lineItemsByLocation.getLocationId()))
              .findFirst().orElse(null);
          if (null != locationManagementDto) {
            ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
            BeanUtils.copyProperties(lineItemDto, shipmentLineItemDto);
            LocationDto locationDto = LocationDto
                .builder()
                .id(lineItemsByLocation.getLocationId())
                .locationCode(locationManagementDto.getLocationCode())
                .build();
            shipmentLineItemDto.setLocation(locationDto);
            newShipmentLineItemDto.add(shipmentLineItemDto);
          }
        }
      });
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
    shipmentDraftLineItemsByLocationRepository.deleteByShipmentDraftLineItemIdIn(lineItemIds);
  }

  private void saveLineItemLocation(ShipmentDraftDto shipmentDraftDto) {
    List<ShipmentLineItemDto> lineItemDtos = shipmentDraftDto.lineItems();
    List<ShipmentDraftLineItemsByLocation> shipmentDraftLineItemsByLocationList = Lists.newArrayList();
    lineItemDtos.forEach(lineItemDto -> {
      ShipmentDraftLineItemsByLocation shipmentDraftLineItemsByLocation = ShipmentDraftLineItemsByLocation
          .builder()
          .locationId(lineItemDto.getLocation().getId())
          .shipmentDraftLineItemId(lineItemDto.getId())
          .build();
      shipmentDraftLineItemsByLocationList.add(shipmentDraftLineItemsByLocation);
    });
    shipmentDraftLineItemsByLocationRepository.save(shipmentDraftLineItemsByLocationList);
  }
}
