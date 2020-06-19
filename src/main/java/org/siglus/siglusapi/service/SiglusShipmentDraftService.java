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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.ShipmentLineItem.Importer;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemExtension;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusShipmentDraftService {

  @Autowired
  private SiglusShipmentDraftFulfillmentService siglusShipmentDraftFulfillmentService;

  @Autowired
  private ShipmentDraftLineItemExtensionRepository lineItemExtensionRepository;

  public ShipmentDraftDto updateShipmentDraft(UUID id, ShipmentDraftDto draftDto) {
    ShipmentDraftDto updatedDraftDto = siglusShipmentDraftFulfillmentService
        .updateShipmentDraft(id, draftDto);
    updateLineItemExtension(updatedDraftDto, draftDto);
    return updatedDraftDto;
  }

  public Page<ShipmentDraftDto> searchShipmentDrafts(UUID orderId, Pageable pageable) {
    Page<ShipmentDraftDto> page = siglusShipmentDraftFulfillmentService
        .searchShipmentDrafts(orderId, pageable);
    setLineItemExtension(page.getContent());
    return page;
  }

  public ShipmentDraftDto searchShipmentDraft(UUID id, Set<String> expand) {
    ShipmentDraftDto draftDto = siglusShipmentDraftFulfillmentService
        .searchShipmentDraft(id, expand);
    setLineItemExtension(newArrayList(draftDto));
    return draftDto;
  }

  public void deleteShipmentDraft(UUID id) {
    ShipmentDraftDto draftDto = siglusShipmentDraftFulfillmentService
        .searchShipmentDraft(id, null);
    siglusShipmentDraftFulfillmentService.deleteShipmentDraft(id);
    deleteLineItemExtension(draftDto);
  }

  private void updateLineItemExtension(ShipmentDraftDto updatedDraftDto,
      ShipmentDraftDto draftDto) {
    List<ShipmentLineItemDto> lineItems = updatedDraftDto.lineItems();
    Set<UUID> lineItemIds = lineItems.stream().map(Importer::getId).collect(Collectors.toSet());
    Map<UUID, ShipmentDraftLineItemExtension> lineItemExtensionMap = lineItemExtensionRepository
        .findByShipmentDraftLineItemIdIn(lineItemIds).stream()
        .collect(toMap(ShipmentDraftLineItemExtension::getShipmentDraftLineItemId,
            extension -> extension));
    Map<UUID, Boolean> lineItemSkippedMap = draftDto.lineItems().stream().collect(toMap(
        ShipmentLineItemDto::getId, ShipmentLineItemDto::isSkipped));
    List<ShipmentDraftLineItemExtension> extensionsToUpdate = newArrayList();
    lineItems.forEach(lineItem -> {
      ShipmentDraftLineItemExtension extension = lineItemExtensionMap.get(lineItem.getId());
      boolean skipped = lineItemSkippedMap.get(lineItem.getId());
      lineItem.setSkipped(skipped);
      if (null != extension) {
        extension.setSkipped(lineItem.isSkipped());
        extensionsToUpdate.add(extension);
      } else {
        extensionsToUpdate.add(
            ShipmentDraftLineItemExtension.builder()
                .shipmentDraftLineItemId(lineItem.getId())
                .skipped(lineItem.isSkipped())
                .build());
      }
    });
    lineItemExtensionRepository.save(extensionsToUpdate);
  }

  private void setLineItemExtension(List<ShipmentDraftDto> draftDtos) {
    draftDtos.forEach(draftDto -> {
      List<ShipmentLineItemDto> lineItems = draftDto.lineItems();
      Set<UUID> lineItemIds = lineItems.stream().map(Importer::getId).collect(Collectors.toSet());
      Map<UUID, ShipmentDraftLineItemExtension> lineItemExtensionMap = lineItemExtensionRepository
          .findByShipmentDraftLineItemIdIn(lineItemIds).stream()
          .collect(toMap(ShipmentDraftLineItemExtension::getShipmentDraftLineItemId,
              extension -> extension));
      lineItems.forEach(lineItem -> {
        ShipmentDraftLineItemExtension extension = lineItemExtensionMap.get(lineItem.getId());
        if (null != extension) {
          lineItem.setSkipped(extension.isSkipped());
        }
      });
    });
  }

  private void deleteLineItemExtension(ShipmentDraftDto draftDto) {
    List<ShipmentLineItemDto> lineItems = draftDto.lineItems();
    Set<UUID> lineItemIds = lineItems.stream().map(Importer::getId).collect(Collectors.toSet());
    List<ShipmentDraftLineItemExtension> extensions = lineItemExtensionRepository
        .findByShipmentDraftLineItemIdIn(lineItemIds);
    lineItemExtensionRepository.delete(extensions);
  }

}
