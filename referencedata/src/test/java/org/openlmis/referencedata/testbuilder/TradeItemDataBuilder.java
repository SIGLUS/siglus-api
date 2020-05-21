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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.domain.TradeItemClassification;

public class TradeItemDataBuilder {

  private UUID id;
  private String manufacturerOfTradeItem;
  private List<TradeItemClassification> classifications;

  /**
   * Builds instance of {@link TradeItemDataBuilder} with sample data.
   */
  public TradeItemDataBuilder() {
    id = UUID.randomUUID();
    manufacturerOfTradeItem = RandomStringUtils.randomAlphanumeric(5);
    classifications = Lists.newArrayList();
  }

  /**
   * Add classification based on data from {@link CommodityType}.
   */
  public TradeItemDataBuilder withClassification(CommodityType type) {
    TradeItemClassification classification = new TradeItemClassification();
    classification.setClassificationSystem(type.getClassificationSystem());
    classification.setClassificationId(type.getClassificationId());

    classifications.add(classification);
    return this;
  }

  /**
   * Adds manufacturerOfTradeItem based on the provided data.
   */
  public TradeItemDataBuilder withManufacturerOfTradeItem(String manufacturerOfTradeItem) {
    this.manufacturerOfTradeItem = manufacturerOfTradeItem;
    return this;
  }

  /**
   * Creates a new instance of {@link TradeItem} without id field.
   */
  public TradeItem buildAsNew() {
    return new TradeItem(manufacturerOfTradeItem, classifications);
  }

  /**
   * Creates a new instance of {@link TradeItem}.
   */
  public TradeItem build() {
    TradeItem tradeItem = buildAsNew();
    tradeItem.setId(id);

    return tradeItem;
  }
}
