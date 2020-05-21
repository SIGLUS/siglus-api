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
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.dto.ReasonCategory;
import org.openlmis.requisition.dto.ReasonType;
import org.openlmis.requisition.testutils.api.DataBuilder;

@NoArgsConstructor
public class StockAdjustmentReasonDataBuilder implements DataBuilder<StockAdjustmentReason> {

  private UUID reasonId = UUID.randomUUID();
  private String name = RandomStringUtils.randomAlphanumeric(5);
  private String description = RandomStringUtils.randomAlphanumeric(10);
  private ReasonType reasonType = ReasonType.CREDIT;
  private ReasonCategory reasonCategory = ReasonCategory.ADJUSTMENT;
  private Boolean isFreeTextAllowed = true;
  private Boolean hidden = false;

  /**
   * Builds {@link AvailableRequisitionColumn} instance with test data.
   */
  public StockAdjustmentReason build() {
    return new StockAdjustmentReason(reasonId, name, description, reasonType, reasonCategory,
        isFreeTextAllowed, hidden);
  }

  public StockAdjustmentReasonDataBuilder withReasonId(UUID reasonId) {
    this.reasonId = reasonId;
    return this;
  }

  public StockAdjustmentReasonDataBuilder withReasonCategory(ReasonCategory reasonCategory) {
    this.reasonCategory = reasonCategory;
    return this;
  }

  public StockAdjustmentReasonDataBuilder withReasonType(ReasonType reasonType) {
    this.reasonType = reasonType;
    return this;
  }

  public StockAdjustmentReasonDataBuilder withIsFreeTextAllowed(boolean isFreeTextAllowed) {
    this.isFreeTextAllowed = isFreeTextAllowed;
    return this;
  }

  public StockAdjustmentReasonDataBuilder withName(String name) {
    this.name = name;
    return this;
  }
}
