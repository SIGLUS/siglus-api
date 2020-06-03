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

package org.openlmis.fulfillment.web.util;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProofOfDeliveryDtoBuilder {

  @Value("${service.url}")
  private String serviceUrl;

  @Autowired
  private OrderableReferenceDataService orderableReferenceDataService;

  /**
   * Create a new list of {@link ProofOfDeliveryDto} based on data
   * from {@link Shipment}.
   *
   * @param pods collection used to create {@link ProofOfDeliveryDto} list (can be {@code null})
   * @return new list of {@link ProofOfDeliveryDto}. Empty list if passed argument is {@code null}.
   */
  public List<ProofOfDeliveryDto> build(Collection<ProofOfDelivery> pods) {
    if (null == pods) {
      return Collections.emptyList();
    }
    return pods
        .stream()
        .map(this::export)
        .collect(Collectors.toList());
  }

  /**
   * Create a new instance of {@link ProofOfDeliveryDto} based on data
   * from {@link ProofOfDelivery}.
   *
   * @param pod instance used to create {@link ProofOfDeliveryDto} (can be {@code null})
   * @return new instance of {@link ProofOfDeliveryDto}. {@code null}
   *         if passed argument is {@code null}.
   */
  public ProofOfDeliveryDto build(ProofOfDelivery pod) {
    if (null == pod) {
      return null;
    }
    return export(pod);
  }

  private ProofOfDeliveryDto export(ProofOfDelivery pod) {
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto();
    dto.setServiceUrl(serviceUrl);
    dto.setLineItems(exportToDtos(pod.getLineItems()));
    pod.export(dto);
    return dto;
  }

  private List<ProofOfDeliveryLineItemDto> exportToDtos(List<ProofOfDeliveryLineItem> lineItems) {
    List<ProofOfDeliveryLineItemDto> lineItemDtos = new ArrayList<>(lineItems.size());
    Set<VersionEntityReference> orderableIdentities = new HashSet<>(lineItems.size());

    for (ProofOfDeliveryLineItem lineItem: lineItems) {
      orderableIdentities.add(lineItem.getOrderable());
    }

    Map<VersionIdentityDto, OrderableDto> orderables =
        orderableReferenceDataService.findByIdentities(orderableIdentities)
            .stream()
            .collect(toMap(OrderableDto::getIdentity, identity -> identity));

    lineItems.forEach(l -> lineItemDtos.add(exportToDto(l, orderables)));
    return lineItemDtos;
  }

  private ProofOfDeliveryLineItemDto exportToDto(ProofOfDeliveryLineItem lineItem,
      Map<VersionIdentityDto, OrderableDto> orderables) {
    ProofOfDeliveryLineItemDto lineItemDto = new ProofOfDeliveryLineItemDto();
    lineItemDto.setServiceUrl(serviceUrl);

    final OrderableDto orderableDto = orderables.get(
        new VersionIdentityDto(lineItem.getOrderable()));

    lineItem.export(lineItemDto, orderableDto);
    return lineItemDto;
  }

}
