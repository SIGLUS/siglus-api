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

package org.openlmis.requisition.service.stockmanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class StockCardRangeSummaryStockManagementService
    extends BaseStockManagementService<StockCardRangeSummaryDto> {

  /**
   * Get a map of stock cards assigned to orderable ids.
   * Stock cards are grouped using orderable fulfills endpoint.
   * If there is no orderable that can be fulfilled by stock card its orderable id will be used.
   *
   * @param programId id of the program
   * @param facilityId id of the facility
   * @param orderableIdentities set of orderable IDs
   * @param tag string value of the tag
   * @param startDate start date
   * @param endDate end date
   * @return  the list of matching stock card range summaries
   */
  public List<StockCardRangeSummaryDto> search(UUID programId, UUID facilityId,
      Set<VersionIdentityDto> orderableIdentities, String tag,
      LocalDate startDate, LocalDate endDate) {
    RequestParameters params = RequestParameters.init()
        .set("size", Integer.MAX_VALUE)
        .set("programId", programId)
        .set("facilityId", facilityId)
        .set("tag", tag)
        .set("orderableId", orderableIdentities
            .stream()
            .map(BaseDto::getId)
            .collect(Collectors.toList()))
        .set("startDate", startDate)
        .set("endDate", endDate);

    return getPage(params).getContent();
  }

  @Override
  protected String getUrl() {
    return "/api/stockCardRangeSummaries";
  }

  @Override
  protected Class<StockCardRangeSummaryDto> getResultClass() {
    return StockCardRangeSummaryDto.class;
  }

  @Override
  protected Class<StockCardRangeSummaryDto[]> getArrayResultClass() {
    return StockCardRangeSummaryDto[].class;
  }

}
