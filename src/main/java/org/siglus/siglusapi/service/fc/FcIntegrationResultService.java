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

package org.siglus.siglusapi.service.fc;

import static org.siglus.siglusapi.constant.FcConstants.CMM_API;
import static org.siglus.siglusapi.constant.FcConstants.CMM_JOB;
import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.constant.FcConstants.CP_JOB;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_JOB;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_JOB;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.domain.FcIntegrationResult;
import org.siglus.siglusapi.repository.FcIntegrationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class FcIntegrationResultService {

  @Autowired
  private FcIntegrationResultRepository fcIntegrationResultRepository;

  @Autowired
  private SiglusDateHelper dateHelper;

  public void recordCallFcFailed(String api, String date) {
    recordFcIntegrationResult(api, date, false, null, false, null);
  }

  public void recordFcIntegrationResult(String api, String date, Boolean callFcSuccess,
      Integer callFcCostTimeInSeconds, Boolean finalSuccess, Integer totalCostTimeInSeconds) {
    FcIntegrationResult result = FcIntegrationResult.builder()
        .job(getJobName(api))
        .startDate(date)
        .endDate(dateHelper.getTodayDateStr())
        .finishTime(new Date())
        .callFcSuccess(callFcSuccess)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(totalCostTimeInSeconds)
        .build();
    log.info("save fc_integration_results: {}", result);
    fcIntegrationResultRepository.save(result);
  }

  private String getJobName(String api) {
    if (RECEIPT_PLAN_API.equals(api)) {
      return RECEIPT_PLAN_JOB;
    } else if (ISSUE_VOUCHER_API.equals(api)) {
      return ISSUE_VOUCHER_JOB;
    } else if (CMM_API.equals(api)) {
      return CMM_JOB;
    } else if (CP_API.equals(api)) {
      return CP_JOB;
    }
    return null;
  }

}
