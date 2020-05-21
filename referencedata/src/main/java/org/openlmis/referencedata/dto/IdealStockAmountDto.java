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

import lombok.Getter;
import lombok.Setter;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.IdealStockAmount;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.web.BaseController;
import org.openlmis.referencedata.web.CommodityTypeController;
import org.openlmis.referencedata.web.FacilityController;
import org.openlmis.referencedata.web.ProcessingPeriodController;

public class IdealStockAmountDto extends BaseDto implements IdealStockAmount.Exporter {

  @Setter
  private String serviceUrl;

  @Getter
  private ObjectReferenceDto facility;

  @Getter
  private ObjectReferenceDto commodityType;

  @Getter
  private ObjectReferenceDto processingPeriod;

  @Getter
  @Setter
  private Integer amount;

  @Override
  public void setFacility(Facility facility) {
    this.facility = new ObjectReferenceDto(serviceUrl,
        BaseController.API_PATH + FacilityController.RESOURCE_PATH, facility.getId());
  }

  @Override
  public void setCommodityType(CommodityType commodityType) {
    this.commodityType = new ObjectReferenceDto(serviceUrl,
        BaseController.API_PATH + CommodityTypeController.RESOURCE_PATH, commodityType.getId());
  }

  @Override
  public void setProcessingPeriod(ProcessingPeriod processingPeriod) {
    this.processingPeriod = new ObjectReferenceDto(serviceUrl,
        BaseController.API_PATH + ProcessingPeriodController.RESOURCE_PATH,
        processingPeriod.getId());
  }
}
