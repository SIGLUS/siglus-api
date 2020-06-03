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

package org.openlmis.fulfillment.web.stockmanagement;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class StockEventLineItemDtoDataBuilder {

  private UUID orderableId = UUID.randomUUID();
  private UUID lotId = UUID.randomUUID();
  private UUID destinationId = UUID.randomUUID();
  private Integer quantity = 10;
  private LocalDate occurredDate = LocalDate.now();
  private UUID reasonId = UUID.randomUUID();
  private UUID sourceId = UUID.randomUUID();
  private Map<String, String> extraData;


  public StockEventLineItemDtoDataBuilder withOrderableId(UUID orderableId) {
    this.orderableId = orderableId;
    return this;
  }

  public StockEventLineItemDtoDataBuilder withQuantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  public StockEventLineItemDtoDataBuilder withOccurredDate(LocalDate occurredDate) {
    this.occurredDate = occurredDate;
    return this;
  }

  /**
   * Builds instance of {@link StockEventLineItemDto}.
   */
  public StockEventLineItemDto build() {
    return new StockEventLineItemDto(
        orderableId, lotId, quantity, occurredDate, destinationId, reasonId, sourceId, extraData
    );
  }
}
