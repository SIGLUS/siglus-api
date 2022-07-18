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
import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_TYPE_API;
import static org.siglus.siglusapi.constant.FcConstants.GEOGRAPHIC_ZONE_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;
import static org.siglus.siglusapi.constant.FcConstants.REGIMEN_API;
import static org.siglus.siglusapi.constant.FcConstants.getCmmAndCpApis;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.FcIntegrationResult;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.repository.FcIntegrationResultRepository;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.CyclomaticComplexity")
public class FcIntegrationResultService {

  private final FcIntegrationResultRepository fcIntegrationResultRepository;
  private final CallFcService callFcService;
  private final SiglusDateHelper dateHelper;

  public ZonedDateTime getLastUpdatedAt(String api) {
    FcIntegrationResult result = fcIntegrationResultRepository
        .findTopByApiAndFinalSuccessOrderByLastUpdatedAtDesc(api, true);
    if (result == null) {
      return ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
    }
    return result.getLastUpdatedAt();
  }

  public FcIntegrationResult recordFcIntegrationResult(FcIntegrationResultDto resultDto) {
    if (resultDto == null) {
      return null;
    }
    String api = resultDto.getApi();
    String endDate = getCmmAndCpApis().contains(api) ? dateHelper.getCurrentMonthStr() : dateHelper.getTodayDateStr();
    FcIntegrationResult result = FcIntegrationResult.builder()
        .api(api)
        .startDate(resultDto.getStartDate())
        .endDate(endDate)
        .lastUpdatedAt(resultDto.getLastUpdatedAt())
        .totalObjects(resultDto.getTotalObjects())
        .createdObjects(resultDto.getCreatedObjects())
        .updatedObjects(resultDto.getUpdatedObjects())
        .finalSuccess(resultDto.getFinalSuccess())
        .errorMessage(resultDto.getErrorMessage())
        .build();
    log.info("save fc_integration_results: {}", result);
    fcIntegrationResultRepository.save(result);
    clearFcData(api);
    return result;
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
