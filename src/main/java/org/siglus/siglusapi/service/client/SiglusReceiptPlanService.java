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

import org.siglus.common.service.client.BaseReferenceDataService;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SiglusReceiptPlanService extends BaseReferenceDataService<ReceiptPlanDto> {

  @Value("${fc.accessToken}")
  private String accessToken;

  @Override
  protected String getUrl() {
    return "/api/siglusapi/receiptPlans";
  }

  @Override
  protected Class<ReceiptPlanDto> getResultClass() {
    return ReceiptPlanDto.class;
  }

  @Override
  protected Class<ReceiptPlanDto[]> getArrayResultClass() {
    return ReceiptPlanDto[].class;
  }

  public void processingReceiptPlans(String date) {
    postResult("?date=" + date + "&access_token=" + accessToken, null, Void.class, false);
  }

}
