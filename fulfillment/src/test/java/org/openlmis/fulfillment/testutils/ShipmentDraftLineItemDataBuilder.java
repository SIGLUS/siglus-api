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

import java.util.Random;
import java.util.UUID;
import org.openlmis.fulfillment.domain.ShipmentDraftLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;

public class ShipmentDraftLineItemDataBuilder {

  private UUID id = UUID.randomUUID();
  private VersionEntityReference orderable = new VersionEntityReference(UUID.randomUUID(), 1L);
  private UUID lotId = UUID.randomUUID();
  private Long quantityShipped = new Random().nextLong();

  public ShipmentDraftLineItemDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public ShipmentDraftLineItemDataBuilder withOrderable(UUID id, Long versionNumber) {
    this.orderable = new VersionEntityReference(id, versionNumber);
    return this;
  }

  public ShipmentDraftLineItemDataBuilder withLotId(UUID id) {
    this.lotId = id;
    return this;
  }

  public ShipmentDraftLineItemDataBuilder withQuantityShipped(Long quantityShipped) {
    this.quantityShipped = quantityShipped;
    return this;
  }

  public ShipmentDraftLineItemDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  /**
   * Builds instance of {@link ShipmentDraftLineItem}.
   */
  public ShipmentDraftLineItem build() {
    ShipmentDraftLineItem line = new ShipmentDraftLineItem(orderable, lotId, quantityShipped);
    line.setId(id);
    return line;
  }
}
