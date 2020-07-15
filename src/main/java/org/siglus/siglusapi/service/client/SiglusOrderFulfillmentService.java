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

import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.common.service.client.BaseReferenceDataService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class SiglusOrderFulfillmentService extends BaseReferenceDataService<OrderDto> {

  public static final String SUPPLYING_FACILITY_ID = "supplyingFacilityId";
  public static final String REQUESTING_FACILITY_ID = "requestingFacilityId";
  public static final String PROGRAM_ID = "programId";
  public static final String PROCESSING_PERIOD_ID = "processingPeriodId";
  public static final String STATUS = "status";
  public static final String PERIOD_START_DATE = "periodStartDate";
  public static final String PERIOD_END_DATE = "periodEndDate";

  @Override
  protected String getUrl() {
    return "/api/orders/";
  }

  @Override
  protected Class<OrderDto> getResultClass() {
    return OrderDto.class;
  }

  @Override
  protected Class<OrderDto[]> getArrayResultClass() {
    return OrderDto[].class;
  }

  public Page<BasicOrderDto> searchOrders(OrderSearchParams params,
      Pageable pageable) {
    RequestParameters queryParams = RequestParameters.init()
        .set(SUPPLYING_FACILITY_ID, params.getSupplyingFacilityId())
        .set(REQUESTING_FACILITY_ID, params.getRequestingFacilityId())
        .set(PROGRAM_ID, params.getProgramId())
        .set(PROCESSING_PERIOD_ID, params.getProcessingPeriodId())
        .set(STATUS, params.getStatus())
        .set(PERIOD_START_DATE, params.getPeriodStartDate())
        .set(PERIOD_END_DATE, params.getPeriodEndDate())
        .setPage(pageable);
    return getPage("", queryParams, null, HttpMethod.GET,
        BasicOrderDto.class, true);
  }

}
