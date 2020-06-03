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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.service.ExporterBuilder;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineItemDto implements OrderLineItem.Importer, OrderLineItem.Exporter {

  private UUID id;
  private OrderableDto orderable;
  private Long orderedQuantity;
  private Long totalDispensingUnits;

  /**
   * Create new instance of TemplateParameterDto based on given {@link OrderLineItem}.
   * @param line instance of Template
   * @return new instance of TemplateDto.
   */
  public static OrderLineItemDto newInstance(OrderLineItem line, ExporterBuilder exporter,
                                             List<OrderableDto> orderables) {
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    exporter.export(line, orderLineItemDto, orderables);

    return orderLineItemDto;
  }

  @JsonIgnore
  @Override
  public VersionIdentityDto getOrderableIdentity() {
    return Optional
        .ofNullable(orderable)
        .map(item -> new VersionIdentityDto(orderable.getId(), orderable.getVersionNumber()))
        .orElse(null);
  }
}
