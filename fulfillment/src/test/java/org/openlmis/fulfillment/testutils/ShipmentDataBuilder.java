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

package org.openlmis.fulfillment.testutils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;

public class ShipmentDataBuilder {

  private UUID id = UUID.randomUUID();
  private Order order = new OrderDataBuilder().build();
  private CreationDetails shipDetails = new CreationDetailsDataBuilder().build();
  private String notes = "all shipped";
  private List<ShipmentLineItem> shipmentLineItems =
      Collections.singletonList(new ShipmentLineItemDataBuilder().build());
  private Map<String, String> extraData = null;

  public ShipmentDataBuilder withOrder(Order order) {
    this.order = order;
    return this;
  }

  public ShipmentDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public ShipmentDataBuilder withShipDetails(CreationDetails shipDetails) {
    this.shipDetails = shipDetails;
    return this;
  }

  public ShipmentDataBuilder withNotes(String notes) {
    this.notes = notes;
    return this;
  }

  public ShipmentDataBuilder withLineItems(List<ShipmentLineItem> lineItems) {
    this.shipmentLineItems = lineItems;
    return this;
  }

  public ShipmentDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  public ShipmentDataBuilder withoutLineItems() {
    this.shipmentLineItems = Collections.emptyList();
    return this;
  }

  /**
   * Builds instance of {@link Shipment}.
   */
  public Shipment build() {
    Shipment shipment = new Shipment(order, shipDetails, notes, shipmentLineItems, extraData);
    shipment.setId(id);
    return shipment;
  }
}
