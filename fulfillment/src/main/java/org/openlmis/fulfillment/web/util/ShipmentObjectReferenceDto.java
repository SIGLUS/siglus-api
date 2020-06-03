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

import static org.openlmis.fulfillment.service.ResourceNames.SHIPMENTS;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ShipmentObjectReferenceDto extends ObjectReferenceDto {
  private OrderObjectReferenceDto order;
  private UserObjectReferenceDto shippedBy;
  private ZonedDateTime shippedDate;
  private String notes;
  private List<ShipmentLineItemDto> lineItems;
  private Map<String, String> extraData;

  public ShipmentObjectReferenceDto(UUID id) {
    this(id, null);
  }

  public ShipmentObjectReferenceDto(UUID id, String serviceUrl) {
    super(id, serviceUrl, SHIPMENTS);
  }
}
