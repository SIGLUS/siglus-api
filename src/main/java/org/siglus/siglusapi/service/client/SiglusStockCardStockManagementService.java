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

package org.siglus.siglusapi.service.client;

import java.util.Collections;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.web.Pagination;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.siglus.common.util.RequestParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusStockCardStockManagementService
    extends BaseStockManagementService<StockCardSummaryV2Dto> {

  @Autowired
  private ProgramOrderableRepository programOrderableRepository;

  public Page<StockCardSummaryV2Dto> search(StockCardSummariesV2SearchParams v2SearchParams,
      Pageable pageable) {
    if (programOrderableRepository.countByProgramId(v2SearchParams.getProgramId()) > 0) {
      RequestParameters params = RequestParameters.init()
          .set("programId", v2SearchParams.getProgramId())
          .set("facilityId", v2SearchParams.getFacilityId())
          .set("orderableId", v2SearchParams.getOrderableIds())
          .set("asOfDate", v2SearchParams.getAsOfDate())
          .set("nonEmptyOnly", v2SearchParams.isNonEmptyOnly())
          .setPage(pageable);

      return getPage(params);
    }
    return Pagination.getPage(Collections.emptyList(), pageable);
  }

  @Override
  protected String getUrl() {
    return "/api/v2/stockCardSummaries";
  }

  @Override
  protected Class<StockCardSummaryV2Dto> getResultClass() {
    return StockCardSummaryV2Dto.class;
  }

  @Override
  protected Class<StockCardSummaryV2Dto[]> getArrayResultClass() {
    return StockCardSummaryV2Dto[].class;
  }
}
