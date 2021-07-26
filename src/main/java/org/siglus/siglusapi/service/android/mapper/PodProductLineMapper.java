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

package org.siglus.siglusapi.service.android.mapper;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.siglus.siglusapi.dto.android.response.PodProductLineResponse;

@Mapper(componentModel = "spring")
public interface PodProductLineMapper {

  @Mapping(target = "code", source = "orderable.productCode")
  PodProductLineResponse toResponse(OrderLineItemDto orderLine);

  default List<PodProductLineResponse> toResponses(UUID orderId, @Context Map<UUID, OrderDto> allOrders) {
    if (orderId == null || allOrders.get(orderId) == null) {
      return Collections.emptyList();
    }
    return allOrders.get(orderId).orderLineItems().stream()
        .map(this::toResponse)
        .collect(toList());
  }

}
