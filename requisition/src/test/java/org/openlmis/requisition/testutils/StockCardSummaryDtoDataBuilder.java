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

import java.util.UUID;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardSummaryDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class StockCardSummaryDtoDataBuilder implements DtoDataBuilder<StockCardSummaryDto> {
  private UUID orderableId = UUID.randomUUID();
  private int stockOnHand = 10;

  public StockCardSummaryDtoDataBuilder withOrderableId(UUID orderableId) {
    this.orderableId = orderableId;
    return this;
  }

  public StockCardSummaryDtoDataBuilder withStockOnHand(Integer stockOnHand) {
    this.stockOnHand = stockOnHand;
    return this;
  }

  /**
   * Creates new instance of {@link StockCardSummaryDto} based on passed data.
   */
  public StockCardSummaryDto buildAsDto() {
    StockCardSummaryDto dto = new StockCardSummaryDto();
    dto.setOrderable(new ObjectReferenceDto(orderableId));
    dto.setStockOnHand(stockOnHand);

    return dto;
  }

}
