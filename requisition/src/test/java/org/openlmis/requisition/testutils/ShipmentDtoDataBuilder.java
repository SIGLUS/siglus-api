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

package org.openlmis.requisition.testutils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ShipmentDto;
import org.openlmis.requisition.dto.ShipmentLineItemDto;
import org.openlmis.requisition.dto.UserObjectReferenceDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ShipmentDtoDataBuilder implements DtoDataBuilder<ShipmentDto> {

  private ObjectReferenceDto order;
  private UserObjectReferenceDto shippedBy;
  private ZonedDateTime shippedDate;
  private String notes;
  private List<ShipmentLineItemDto> lineItems;
  private Map<String, String> extraData;

  /**
   * Builder for {@link ShipmentDto}.
   */
  public ShipmentDtoDataBuilder() {
    this.order = new ObjectReferenceDtoDataBuilder().buildAsDto();
    this.shippedBy = new UserObjectReferenceDto("someone");
    this.shippedDate = ZonedDateTime.now();
    this.notes = "notes";
    this.lineItems = new ArrayList<>();
    this.extraData = new HashMap<>();
  }

  @Override
  public ShipmentDto buildAsDto() {
    return new ShipmentDto(order, shippedBy, shippedDate, notes, lineItems, extraData);
  }
}
