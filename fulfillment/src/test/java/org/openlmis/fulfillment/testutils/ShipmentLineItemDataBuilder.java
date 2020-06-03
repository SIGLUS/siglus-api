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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;

public class ShipmentLineItemDataBuilder {

  private UUID id = UUID.randomUUID();
  private VersionEntityReference orderable = new VersionEntityReference(UUID.randomUUID(), 1L);
  private UUID lotId = UUID.randomUUID();
  private Long quantityShipped = 10L;
  private Map<String, String> extraData = new HashMap<>();

  public ShipmentLineItemDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public ShipmentLineItemDataBuilder withOrderable(UUID orderableId, Long versionNumber) {
    this.orderable = new VersionEntityReference(orderableId, versionNumber);
    return this;
  }

  public ShipmentLineItemDataBuilder withOrderable(VersionObjectReferenceDto reference) {
    this.orderable = new VersionEntityReference(reference.getId(), reference.getVersionNumber());
    return this;
  }

  public ShipmentLineItemDataBuilder withLotId(UUID id) {
    this.lotId = id;
    return this;
  }

  public ShipmentLineItemDataBuilder withExtraData(Map<String, String> extraData) {
    this.extraData = extraData;
    return this;
  }

  public ShipmentLineItemDataBuilder withQuantityShipped(Long quantityShipped) {
    this.quantityShipped = quantityShipped;
    return this;
  }

  public ShipmentLineItemDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  public ShipmentLineItemDataBuilder withoutLotId() {
    this.lotId = null;
    return this;
  }

  /**
   * Builds instance of {@link ShipmentLineItem}.
   */
  public ShipmentLineItem build() {
    ShipmentLineItem line = new ShipmentLineItem(orderable, lotId, quantityShipped, extraData);
    line.setId(id);
    return line;
  }
}
