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

import java.util.UUID;

import org.siglus.common.util.RequestParameters;
import org.siglus.siglusapi.constant.FieldConstants;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

@Service
public class SiglusStockCardSummariesPrintService extends BaseStockManagementService<ModelAndView> {

  @Override
  protected String getUrl() {
    return "/api/stockCardSummaries/print";
  }

  @Override
  protected Class<ModelAndView> getResultClass() {
    return ModelAndView.class;
  }

  @Override
  protected Class<ModelAndView[]> getArrayResultClass() {
    return ModelAndView[].class;
  }

  public ModelAndView getStockCardSummariesReportView(UUID programId, UUID facilityId) {
    RequestParameters queryParams = RequestParameters.init()
        .set(FieldConstants.PROGRAM, programId)
        .set(FieldConstants.FACILITY, facilityId);
    return findOne("", queryParams);
  }

}
