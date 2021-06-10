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

package org.siglus.siglusapi.testutils;

import java.time.LocalDate;
import java.util.UUID;
import org.openlmis.stockmanagement.testutils.DatesUtil;
import org.siglus.common.domain.StockCardExtension;

public class StockCardExtensionDataBuilder {

  private final UUID id = UUID.randomUUID();
  private UUID stockCardId = UUID.randomUUID();
  private final LocalDate createDate = DatesUtil.getBaseDate();

  public StockCardExtensionDataBuilder() {
  }

  public StockCardExtensionDataBuilder withStockCardId(UUID stockCardId) {
    this.stockCardId = stockCardId;
    return this;
  }

  public StockCardExtensionDataBuilder withArchived(boolean archived) {
    return this;
  }

  public StockCardExtension build() {
    StockCardExtension stockCardExtension = new StockCardExtension(stockCardId, createDate);
    stockCardExtension.setId(id);
    return stockCardExtension;
  }
}
