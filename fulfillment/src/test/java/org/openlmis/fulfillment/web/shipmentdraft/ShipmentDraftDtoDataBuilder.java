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

import java.util.List;
import org.openlmis.fulfillment.testutils.ShipmentDraftDataBuilder;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;

public class ShipmentDraftDtoDataBuilder {

  private ShipmentDraftDto draftDto;

  /**
   * Constructs new shipment draft dto data builder.
   */
  public ShipmentDraftDtoDataBuilder() {
    draftDto = new ShipmentDraftDto();
    draftDto.setServiceUrl("localhost");
    new ShipmentDraftDataBuilder().build().export(draftDto);
  }

  public ShipmentDraftDtoDataBuilder withOrder(OrderObjectReferenceDto order) {
    draftDto.setOrder(order);
    return this;
  }

  public ShipmentDraftDtoDataBuilder withNotes(String notes) {
    draftDto.setNotes(notes);
    return this;
  }

  public ShipmentDraftDtoDataBuilder withLineItems(List<ShipmentLineItemDto> lineItems) {
    draftDto.setLineItems(lineItems);
    return this;
  }

  public ShipmentDraftDtoDataBuilder withoutId() {
    draftDto.setId(null);
    return this;
  }

  /**
   * Builds instance of {@link ShipmentDraftDto}.
   */
  public ShipmentDraftDto build() {
    draftDto.setServiceUrl(null);
    return draftDto;
  }

}
