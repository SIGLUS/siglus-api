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

import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.stockmanagement.StockCardDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class StockCardStockManagementService extends BaseStockManagementService<StockCardDto> {

  /**
   * Retrieves the list of stock cards fo the given facility and program.
   *
   * @param facility  the id of the facility
   * @param program  the program of the facility
   * @return  the list of matching stock cards
   */
  public List<StockCardDto> getStockCards(UUID facility, UUID program) {
    RequestParameters params = RequestParameters.init()
        .set("size", Integer.MAX_VALUE)
        .set("facility", facility.toString())
        .set("program", program.toString());

    return getPage(params).getContent();
  }

  @Override
  protected String getUrl() {
    return "/api/stockCardSummaries";
  }

  @Override
  protected Class<StockCardDto> getResultClass() {
    return StockCardDto.class;
  }

  @Override
  protected Class<StockCardDto[]> getArrayResultClass() {
    return StockCardDto[].class;
  }
}
