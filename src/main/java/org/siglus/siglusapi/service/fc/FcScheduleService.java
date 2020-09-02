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

import static java.lang.System.currentTimeMillis;
import static org.siglus.siglusapi.constant.FcConstants.CMM_API;
import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcScheduleService {

  private static final String TIME_ZONE_ID = "${time.zoneId}";

  @Value("${fc.domain}")
  private String domain;

  @Value("${fc.key}")
  private String key;

  @Autowired
  private CallFcService callFcService;

  @Autowired
  private FcIntegrationResultService fcIntegrationResultService;

  @Autowired
  private FcIntegrationCmmService fcIntegrationCmmService;

  @Autowired
  private FcIntegrationCpService fcIntegrationCpService;

  @Autowired
  private FcIssueVoucherService fcIssueVoucherService;

  @Scheduled(cron = "${fc.receiptplan.cron}", zone = TIME_ZONE_ID)
  public void fetchReceiptPlansFromFc() {
    final long startTime = currentTimeMillis();
    String date = fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API);
    Integer callFcCostTimeInSeconds = fetchDataFromFc(RECEIPT_PLAN_API, date);
    // do business process here, call your own service, use `callFcService.getReceiptPlans()`
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(RECEIPT_PLAN_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getReceiptPlans().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(true)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = TIME_ZONE_ID)
  public void fetchIssueVouchersFromFc() {
    final long startTime = currentTimeMillis();
    String date = fcIntegrationResultService.getLatestSuccessDate(ISSUE_VOUCHER_API);
    Integer callFcCostTimeInSeconds = fetchDataFromFc(ISSUE_VOUCHER_API, date);
    Boolean finalSuccess = fcIssueVoucherService
        .createIssueVouchers(callFcService.getIssueVouchers());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(ISSUE_VOUCHER_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getIssueVouchers().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.cmm.cron}", zone = TIME_ZONE_ID)
  public void fetchCmmsFromFc() {
    final long startTime = currentTimeMillis();
    String date = fcIntegrationResultService.getLatestSuccessDate(CMM_API);
    Integer callFcCostTimeInSeconds = fetchDataFromFc(CMM_API, date);
    boolean finalSuccess = fcIntegrationCmmService.dealCmmData(callFcService.getCmms());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(CMM_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getCmms().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.cp.cron}", zone = TIME_ZONE_ID)
  public void fetchCpsFromFc() {
    final long startTime = currentTimeMillis();
    String date = fcIntegrationResultService.getLatestSuccessDate(CP_API);
    Integer callFcCostTimeInSeconds = fetchDataFromFc(CP_API, date);
    boolean finalSuccess = fcIntegrationCpService.dealCpData(callFcService.getCps());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(CP_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getCps().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  public Integer fetchDataFromFc(String api, String date) {
    try {
      final long startTime = currentTimeMillis();
      initData(api);
      for (int page = 1; page <= callFcService.getPageInfoDto().getTotalPages(); page++) {
        callFcService.fetchData(getUrl(api, page, date), api);
      }
      long costTime = currentTimeMillis() - startTime;
      log.info("[FC] fetch {} finish, total size: {}, cost: {}ms", api, getTotalSize(api),
          costTime);
      return Math.toIntExact(costTime / 1000);
    } catch (Exception e) {
      fcIntegrationResultService.recordCallFcFailed(api, date);
      throw e;
    }
  }

  private String getUrl(String path, int page, String date) {
    String url = domain + path + "?key=" + key + "&psize=20&page=" + page + "&";
    if (date.contains("-")) {
      url += "period=" + date;
    } else {
      url += "date=" + date;
    }
    return url;
  }

  private int getTotalSize(String api) {
    if (ISSUE_VOUCHER_API.equals(api)) {
      return callFcService.getIssueVouchers().size();
    } else if (RECEIPT_PLAN_API.equals(api)) {
      return callFcService.getReceiptPlans().size();
    } else if (CMM_API.equals(api)) {
      return callFcService.getCmms().size();
    } else {
      return callFcService.getCps().size();
    }
  }

  private void initData(String api) {
    if (RECEIPT_PLAN_API.equals(api)) {
      callFcService.setReceiptPlans(new ArrayList<>());
    } else if (ISSUE_VOUCHER_API.equals(api)) {
      callFcService.setIssueVouchers(new ArrayList<>());
    } else if (CMM_API.equals(api)) {
      callFcService.setCmms(new ArrayList<>());
    } else if (CP_API.equals(api)) {
      callFcService.setCps(new ArrayList<>());
    }
    callFcService.setPageInfoDto(new PageInfoDto());
  }

  private Integer getTotalCostTimeInSeconds(long startTime) {
    return Math.toIntExact((currentTimeMillis() - startTime) / 1000);
  }
}
