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

package org.openlmis.requisition.service.referencedata;

import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class IdealStockAmountReferenceDataService
    extends BaseReferenceDataService<IdealStockAmountDto> {

  @Override
  protected String getUrl() {
    return "/api/idealStockAmounts";
  }

  @Override
  protected Class<IdealStockAmountDto> getResultClass() {
    return IdealStockAmountDto.class;
  }

  @Override
  protected Class<IdealStockAmountDto[]> getArrayResultClass() {
    return IdealStockAmountDto[].class;
  }

  /**
   * Find ideal stock amount based on passed parameters.
   */
  public List<IdealStockAmountDto> search(UUID facilityId, UUID processingPeriodId) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("facilityId", facilityId)
        .set("processingPeriodId", processingPeriodId)
        .set("size", 2000);

    return getPage(parameters).getContent();
  }
}
