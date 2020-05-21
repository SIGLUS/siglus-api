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

package org.openlmis.referencedata.dto;

import static org.openlmis.referencedata.web.csv.processor.CsvCellProcessors.COMMODITY_TYPE;
import static org.openlmis.referencedata.web.csv.processor.CsvCellProcessors.FACILITY_TYPE;
import static org.openlmis.referencedata.web.csv.processor.CsvCellProcessors.POSITIVE_INT;
import static org.openlmis.referencedata.web.csv.processor.CsvCellProcessors.PROCESSING_PERIOD_TYPE;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.IdealStockAmount;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.web.csv.model.ImportField;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class IdealStockAmountCsvModel extends BaseDto
    implements IdealStockAmount.Exporter, IdealStockAmount.Importer {

  private static final String FACILITY_CODE = "Facility Code";
  private static final String COMMODITY = "Commodity Type";
  private static final String PERIOD = "Period";
  private static final String IDEAL_STOCK_AMOUNT = "Ideal Stock Amount";

  @Getter
  @ImportField(name = FACILITY_CODE, type = FACILITY_TYPE, mandatory = true)
  private BasicFacilityDto facility;

  @Getter
  @ImportField(name = COMMODITY, type = COMMODITY_TYPE, mandatory = true)
  private CommodityTypeDto commodityType;

  @Getter
  @ImportField(name = PERIOD, type = PROCESSING_PERIOD_TYPE, mandatory = true)
  private ProcessingPeriodDto processingPeriod;

  @Getter
  @ImportField(name = IDEAL_STOCK_AMOUNT, type = POSITIVE_INT)
  private Integer amount;

  public void setFacility(BasicFacilityDto facility) {
    this.facility = facility;
  }

  @Override
  public void setFacility(Facility facility) {
    if (facility != null) {
      this.facility = new BasicFacilityDto();
      facility.export(this.facility);
    } else {
      this.facility = null;
    }
  }

  public void setCommodityType(CommodityTypeDto commodityType) {
    this.commodityType = commodityType;
  }

  @Override
  public void setCommodityType(CommodityType commodityType) {
    if (commodityType != null) {
      this.commodityType = new CommodityTypeDto();
      commodityType.export(this.commodityType);
    } else {
      this.commodityType = null;
    }
  }

  public void setProcessingPeriod(ProcessingPeriodDto processingPeriod) {
    this.processingPeriod = processingPeriod;
  }

  @Override
  public void setProcessingPeriod(ProcessingPeriod processingPeriod) {
    if (processingPeriod != null) {
      this.processingPeriod = new IsaProcessingPeriodDto();
      processingPeriod.export(this.processingPeriod);
    } else {
      this.processingPeriod = null;
    }
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }
}
