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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class StockEventDtoDataBuilder {

  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private List<StockEventLineItemDto> lineItems = new ArrayList<>();
  private UUID userId = UUID.randomUUID();

  public StockEventDtoDataBuilder withProgramId(UUID programId) {
    this.programId = programId;
    return this;
  }

  public StockEventDtoDataBuilder withFacilityId(UUID facilityId) {
    this.facilityId = facilityId;
    return this;
  }

  public StockEventDtoDataBuilder withLineItems(List<StockEventLineItemDto> lineItems) {
    this.lineItems = lineItems;
    return this;
  }

  public StockEventDtoDataBuilder withUserId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Builds instance of {@link StockEventDto}.
   */
  public StockEventDto build() {
    return new StockEventDto(this.programId, this.facilityId, this.lineItems, this.userId);
  }
}
