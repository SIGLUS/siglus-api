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

import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.web.QueryOrderableSearchParams;
import org.openlmis.requisition.service.RequestParameters;
import org.openlmis.requisition.service.referencedata.BaseReferenceDataService;
import org.siglus.siglusapi.constant.FieldConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusOrderableReferenceDataService
    extends BaseReferenceDataService<OrderableDto> {

  @Override
  protected String getUrl() {
    return "/api/orderables/";
  }

  @Override
  protected Class<OrderableDto> getResultClass() {
    return OrderableDto.class;
  }

  @Override
  protected Class<OrderableDto[]> getArrayResultClass() {
    return OrderableDto[].class;
  }

  public Page<OrderableDto> searchOrderables(QueryOrderableSearchParams searchParams,
      Pageable pageable) {
    RequestParameters parameters = RequestParameters.init()
        .set(FieldConstants.CODE, searchParams.getCode())
        .set(FieldConstants.NAME, searchParams.getName())
        .set(FieldConstants.PROGRAM, searchParams.getProgramCode())
        .set(FieldConstants.ID, searchParams.getIds())
        .setPage(pageable);
    return getPage(parameters);
  }

}
