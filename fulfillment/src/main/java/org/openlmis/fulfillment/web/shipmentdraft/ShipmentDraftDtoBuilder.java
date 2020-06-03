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

package org.openlmis.fulfillment.web.shipmentdraft;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.domain.ShipmentDraftLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShipmentDraftDtoBuilder {

  @Value("${service.url}")
  private String serviceUrl;

  @Autowired
  private OrderableReferenceDataService orderableReferenceDataService;

  /**
   * Create a new list of {@link ShipmentDraftDto} based on data
   * from {@link ShipmentDraft}.
   *
   * @param shipments collection used to create {@link ShipmentDraftDto} list (can be {@code null})
   * @return new list of {@link ShipmentDraftDto}. Empty list if passed argument is {@code null}.
   */
  public List<ShipmentDraftDto> build(Collection<ShipmentDraft> shipments) {
    if (null == shipments) {
      return Collections.emptyList();
    }
    return shipments.stream()
        .map(this::export)
        .collect(Collectors.toList());
  }

  /**
   * Create a new instance of {@link ShipmentDraftDto} based on data
   * from {@link ShipmentDraft}.
   *
   * @param draft instance used to create {@link ShipmentDraftDto} (can be {@code null})
   * @return new instance of {@link ShipmentDraftDto}.
   * {@code null} if passed argument is {@code null}.
   */
  public ShipmentDraftDto build(ShipmentDraft draft) {
    return draft != null ? export(draft) : null;
  }

  private ShipmentDraftDto export(ShipmentDraft draft) {
    ShipmentDraftDto dto = new ShipmentDraftDto();
    dto.setServiceUrl(serviceUrl);
    draft.export(dto);
    dto.setLineItems(exportToDtos(draft.viewLineItems()));

    return dto;
  }

  private List<ShipmentLineItemDto> exportToDtos(List<ShipmentDraftLineItem> lineItems) {
    List<ShipmentLineItemDto> lineItemDtos = new ArrayList<>(lineItems.size());
    Set<VersionEntityReference> orderableIdentities = new HashSet<>(lineItems.size());

    for (ShipmentDraftLineItem lineItem: lineItems) {
      orderableIdentities.add(lineItem.getOrderable());
    }

    Map<VersionIdentityDto, OrderableDto> orderables =
        orderableReferenceDataService.findByIdentities(orderableIdentities)
            .stream()
            .collect(toMap(OrderableDto::getIdentity, identity -> identity));

    lineItems.forEach(l -> lineItemDtos.add(exportToDto(l, orderables)));
    return lineItemDtos;
  }

  private ShipmentLineItemDto exportToDto(ShipmentDraftLineItem lineItem,
      Map<VersionIdentityDto, OrderableDto> orderables) {
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setServiceUrl(serviceUrl);

    final OrderableDto orderableDto = orderables.get(
        new VersionIdentityDto(lineItem.getOrderable()));

    lineItem.export(lineItemDto, orderableDto);
    return lineItemDto;
  }

}
