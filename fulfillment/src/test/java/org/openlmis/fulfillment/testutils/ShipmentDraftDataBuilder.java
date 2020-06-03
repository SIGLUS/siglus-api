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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.domain.ShipmentDraftLineItem;

public class ShipmentDraftDataBuilder {

  private UUID id = UUID.randomUUID();
  private Order order = new OrderDataBuilder().build();
  private String notes = "all shipped";
  private List<ShipmentDraftLineItem> shipmentLineItems =
      Lists.newArrayList(new ShipmentDraftLineItemDataBuilder().build());

  public ShipmentDraftDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public ShipmentDraftDataBuilder withOrder(Order order) {
    this.order = order;
    return this;
  }

  public ShipmentDraftDataBuilder withNotes(String notes) {
    this.notes = notes;
    return this;
  }

  public ShipmentDraftDataBuilder withLineItems(List<ShipmentDraftLineItem> lineItems) {
    this.shipmentLineItems = lineItems;
    return this;
  }

  public ShipmentDraftDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  /**
   * Builds instance of {@link ShipmentDraft}.
   */
  public ShipmentDraft build() {
    ShipmentDraft shipment = new ShipmentDraft(order, notes, shipmentLineItems);
    shipment.setId(id);
    return shipment;
  }
}
