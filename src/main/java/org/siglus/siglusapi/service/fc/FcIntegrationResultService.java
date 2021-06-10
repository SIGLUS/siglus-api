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
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_JOB;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_TYPE_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_TYPE_JOB;
import static org.siglus.siglusapi.constant.FcConstants.GEOGRAPHIC_ZONE_API;
import static org.siglus.siglusapi.constant.FcConstants.GEOGRAPHIC_ZONE_JOB;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_JOB;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_JOB;
import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_API;
import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_JOB;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_JOB;
import static org.siglus.siglusapi.constant.FcConstants.REGIMEN_API;
import static org.siglus.siglusapi.constant.FcConstants.REGIMEN_JOB;
import static org.siglus.siglusapi.constant.FcConstants.getQueryByPeriodApiList;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.domain.FcIntegrationResult;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.repository.FcIntegrationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings("PMD.CyclomaticComplexity")
public class FcIntegrationResultService {

  @Value("${fc.startDate}")
  private String defaultStartDate;

  @Value("${fc.startPeriod}")
  private String defaultStartPeriod;

  @Autowired
  private FcIntegrationResultRepository fcIntegrationResultRepository;

  @Autowired
  private CallFcService callFcService;

  @Autowired
  private SiglusDateHelper dateHelper;

  public String getLatestSuccessDate(String api) {
    FcIntegrationResult result = fcIntegrationResultRepository
        .findTopByJobAndFinalSuccessOrderByEndDateDesc(getJobName(api), true);
    if (result == null) {
      if (getQueryByPeriodApiList().contains(api)) {
        return defaultStartPeriod;
      }
      return defaultStartDate;
    }
    return result.getEndDate();
  }

  public void recordCallFcFailed(String api, String date) {
    FcIntegrationResultDto result = FcIntegrationResultDto.builder()
        .api(api)
        .date(date)
        .callFcSuccess(false)
        .totalObjectsFromFc(null)
        .callFcCostTimeInSeconds(null)
        .finalSuccess(null)
        .totalCostTimeInSeconds(null)
        .build();
    recordFcIntegrationResult(result);
  }

  public void recordFcIntegrationResult(FcIntegrationResultDto resultDto) {
    String api = resultDto.getApi();
    String endDate = getQueryByPeriodApiList().contains(api) ? resultDto.getDate() :
        dateHelper.getTodayDateStr();
    FcIntegrationResult result = FcIntegrationResult.builder()
        .job(getJobName(api))
        .startDate(resultDto.getDate())
        .endDate(endDate)
        .finishTime(new Date())
        .totalObjectsFromFc(resultDto.getTotalObjectsFromFc())
        .callFcSuccess(resultDto.getCallFcSuccess())
        .callFcCostTimeInSeconds(resultDto.getCallFcCostTimeInSeconds())
        .finalSuccess(resultDto.getFinalSuccess())
        .errorMessage(resultDto.getErrorMessage())
        .totalCostTimeInSeconds(resultDto.getTotalCostTimeInSeconds())
        .build();
    log.info("save fc_integration_results: {}", result);
    fcIntegrationResultRepository.save(result);
    clearFcData(api);
  }

  private String getJobName(String api) {
    String jobName = null;
    if (RECEIPT_PLAN_API.equals(api)) {
      jobName = RECEIPT_PLAN_JOB;
    } else if (ISSUE_VOUCHER_API.equals(api)) {
      jobName = ISSUE_VOUCHER_JOB;
    } else if (CMM_API.equals(api)) {
      jobName = CMM_JOB;
    } else if (CP_API.equals(api)) {
      jobName = CP_JOB;
    } else if (PROGRAM_API.equals(api)) {
      jobName = PROGRAM_JOB;
    } else if (PRODUCT_API.equals(api)) {
      jobName = PRODUCT_JOB;
    } else if (FACILITY_API.equals(api)) {
      jobName = FACILITY_JOB;
    } else if (FACILITY_TYPE_API.equals(api)) {
      jobName = FACILITY_TYPE_JOB;
    } else if (REGIMEN_API.equals(api)) {
      jobName = REGIMEN_JOB;
    } else if (GEOGRAPHIC_ZONE_API.equals(api)) {
      jobName = GEOGRAPHIC_ZONE_JOB;
    }
    return jobName;
  }

  private void clearFcData(String api) {
    if (RECEIPT_PLAN_API.equals(api)) {
      callFcService.getReceiptPlans().clear();
    } else if (ISSUE_VOUCHER_API.equals(api)) {
      callFcService.getIssueVouchers().clear();
    } else if (CMM_API.equals(api)) {
      callFcService.getCmms().clear();
    } else if (CP_API.equals(api)) {
      callFcService.getCps().clear();
    } else if (PROGRAM_API.equals(api)) {
      callFcService.getPrograms().clear();
    } else if (PRODUCT_API.equals(api)) {
      callFcService.getProducts().clear();
    } else if (FACILITY_TYPE_API.equals(api)) {
      callFcService.getFacilityTypes().clear();
    } else if (FACILITY_API.equals(api)) {
      callFcService.getFacilities().clear();
    } else if (REGIMEN_API.equals(api)) {
      callFcService.getRegimens().clear();
    } else if (GEOGRAPHIC_ZONE_API.equals(api)) {
      callFcService.getGeographicZones().clear();
    }
  }

}
