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
import org.openlmis.requisition.dto.stockmanagement.StockCardSummaryDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class StockCardSummariesStockManagementService
    extends BaseStockManagementService<StockCardSummaryDto> {

  /**
   * Retrieves the list of stock cards summaries for the given orderables.
   *
   * @param orderableIds  set of orderable IDs
   * @return  the list of matching stock card summaries
   */
  public List<StockCardSummaryDto> search(UUID programId, UUID facilityId,
                                          Set<UUID> orderableIds,
                                          LocalDate asOfDate) {
    RequestParameters params = RequestParameters.init()
        .set("size", Integer.MAX_VALUE)
        .set("programId", programId)
        .set("facilityId", facilityId)
        .set("orderableId", orderableIds)
        .set("asOfDate", asOfDate);

    return getPage(params).getContent();
  }

  @Override
  protected String getUrl() {
    return "/api/v2/stockCardSummaries";
  }

  @Override
  protected Class<StockCardSummaryDto> getResultClass() {
    return StockCardSummaryDto.class;
  }

  @Override
  protected Class<StockCardSummaryDto[]> getArrayResultClass() {
    return StockCardSummaryDto[].class;
  }
}
