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

package org.openlmis.requisition.domain.requisition;

import java.util.UUID;
import org.apache.commons.lang.math.RandomUtils;
import org.openlmis.requisition.testutils.api.DataBuilder;
import org.openlmis.requisition.testutils.api.RepositoryDataBuilder;

public class StockAdjustmentDataBuilder implements DataBuilder<StockAdjustment>,
    RepositoryDataBuilder<StockAdjustment> {
  private UUID id = UUID.randomUUID();
  private UUID reasonId = UUID.randomUUID();
  private Integer quantity = RandomUtils.nextInt();

  /**
   * Creates new instance of {@link StockAdjustment} with provided data.
   */
  @Override
  public StockAdjustment build() {
    StockAdjustment adjustment = new StockAdjustment(reasonId, quantity);
    adjustment.setId(id);

    return adjustment;
  }

  /**
   * Creates new instance of {@link StockAdjustment} without id.
   */
  @Override
  public StockAdjustment buildAsNew() {
    return new StockAdjustment(reasonId, quantity);
  }

  public StockAdjustmentDataBuilder withQuantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }


  public StockAdjustmentDataBuilder withReasonId(UUID reasonId) {
    this.reasonId = reasonId;
    return this;
  }
}
