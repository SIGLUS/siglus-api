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

package org.openlmis.fulfillment.web.shipment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;

public class ShipmentDtoDataBuilder {

  private ShipmentDto shipmentDto;

  /**
   * Constructs new shipment dto data builder.
   */
  public ShipmentDtoDataBuilder() {
    shipmentDto = new ShipmentDto();
    shipmentDto.setServiceUrl("localhost");
    new ShipmentDataBuilder().build().export(shipmentDto);

    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDtoDataBuilder().build();
    shipmentDto.setLineItems(Collections.singletonList(shipmentLineItemDto));
  }

  public ShipmentDtoDataBuilder withOrder(OrderObjectReferenceDto order) {
    shipmentDto.setOrder(order);
    return this;
  }

  public ShipmentDtoDataBuilder withNotes(String notes) {
    shipmentDto.setNotes(notes);
    return this;
  }

  public ShipmentDtoDataBuilder withLineItems(List<ShipmentLineItemDto> lineItems) {
    shipmentDto.setLineItems(lineItems);
    return this;
  }

  public ShipmentDtoDataBuilder withoutId() {
    shipmentDto.setId(null);
    return this;
  }

  public ShipmentDtoDataBuilder withoutShippedBy() {
    shipmentDto.setShippedBy(null);
    return this;
  }

  public ShipmentDtoDataBuilder withoutShippedDate() {
    shipmentDto.setShippedDate(null);
    return this;
  }

  public ShipmentDtoDataBuilder withExtraData(Map<String, String> extraData) {
    shipmentDto.setExtraData(extraData);
    return this;
  }

  /**
   * Builds instance of {@link ShipmentLineItemDto}.
   */
  public ShipmentDto build() {
    shipmentDto.setServiceUrl(null);
    return shipmentDto;
  }

}
