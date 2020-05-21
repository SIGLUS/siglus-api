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

package org.openlmis.referencedata.testbuilder;

import java.time.LocalDate;
import java.util.UUID;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;

public class LotDataBuilder {
  private static int instanceNumber = 0;

  private UUID id = UUID.randomUUID();
  private String lotCode = "code #" + Integer.toString(instanceNumber++);
  private LocalDate expirationDate;
  private LocalDate manufacturedDate;
  private TradeItem tradeItem = new TradeItemDataBuilder().build();
  private boolean active = true;

  public LotDataBuilder withTradeItem(TradeItem tradeItem) {
    this.tradeItem = tradeItem;
    return this;
  }

  public LotDataBuilder withExpirationDate(LocalDate expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  public LotDataBuilder withLotCode(String lotCode) {
    this.lotCode = lotCode;
    return this;
  }

  /**
   * Builds an instance of the {@link Lot} class with populated ID.
   *
   * @return the instance of {@link Lot} class
   */
  public Lot build() {
    Lot lot = buildAsNew();
    lot.setId(id);
    return lot;
  }

  /**
   * Build an instance of the {@link Lot} class without ID field populated.
   *
   * @return the instance of {@link Lot} class
   */
  public Lot buildAsNew() {
    return new Lot(
        lotCode,
        expirationDate,
        manufacturedDate,
        tradeItem,
        active
    );
  }
}
