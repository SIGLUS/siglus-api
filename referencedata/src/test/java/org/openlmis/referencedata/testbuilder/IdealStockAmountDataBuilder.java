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

import java.util.UUID;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.IdealStockAmount;
import org.openlmis.referencedata.domain.ProcessingPeriod;

public class IdealStockAmountDataBuilder {

  private UUID id;
  private Facility facility;
  private CommodityType commodityType;
  private ProcessingPeriod processingPeriod;
  private Integer amount;

  /**
   * Returns instance of {@link IdealStockAmountDataBuilder} with sample data.
   */
  public IdealStockAmountDataBuilder() {
    id = UUID.randomUUID();
    facility = new FacilityDataBuilder().build();
    commodityType = new CommodityTypeDataBuilder().build();
    processingPeriod = new ProcessingPeriodDataBuilder().build();
    amount = 1000;
  }

  /**
   * Builds instance of {@link IdealStockAmount}.
   */
  public IdealStockAmount build() {
    IdealStockAmount idealStockAmount = buildAsNew();
    idealStockAmount.setId(id);

    return idealStockAmount;
  }

  public IdealStockAmount buildAsNew() {
    return new IdealStockAmount(facility, commodityType, processingPeriod, amount);
  }

  public IdealStockAmountDataBuilder withFacility(Facility facility) {
    this.facility = facility;
    return this;
  }

  public IdealStockAmountDataBuilder withCommodityType(CommodityType commodityType) {
    this.commodityType = commodityType;
    return this;
  }

  public IdealStockAmountDataBuilder withProcessingPeriod(ProcessingPeriod processingPeriod) {
    this.processingPeriod = processingPeriod;
    return this;
  }

}
