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

package org.openlmis.fulfillment;

import java.util.UUID;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;

public class OrderLineItemDataBuilder {
  private UUID id = UUID.randomUUID();
  private Order order;
  private VersionEntityReference orderable = new VersionEntityReference(UUID.randomUUID(), 1L);
  private Long orderedQuantity = 1200L;

  public OrderLineItemDataBuilder withoutId() {
    id = null;
    return this;
  }

  public OrderLineItemDataBuilder withOrderable(UUID orderableId, Long versionNumber) {
    this.orderable = new VersionEntityReference(orderableId, versionNumber);
    return this;
  }

  public OrderLineItemDataBuilder withoutOrderable() {
    this.orderable = null;
    return this;
  }

  public OrderLineItemDataBuilder withRandomOrderedQuantity() {
    orderedQuantity = randomValue();
    return this;
  }

  public OrderLineItemDataBuilder withOrderedQuantity(Long orderedQuantity) {
    this.orderedQuantity = orderedQuantity;
    return this;
  }

  public OrderLineItemDataBuilder withOrder(Order order) {
    this.order = order;
    return this;
  }

  /**
   * Creates new instance of {@link OrderLineItem} based on passed data.
   */
  public OrderLineItem build() {
    OrderLineItem lineItem = new OrderLineItem(
        order, orderable, orderedQuantity
    );
    lineItem.setId(id);

    return lineItem;
  }

  private long randomValue() {
    return (long) (Math.random() * (5000));
  }

}
