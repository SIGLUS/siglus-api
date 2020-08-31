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
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;

import java.util.ArrayList;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcScheduleService {

  @Value("${fc.domain}")
  private String domain;

  @Value("${fc.key}")
  private String key;

  @Autowired
  private SiglusDateHelper dateHelper;

  @Autowired
  private CallFcService callFcService;

  @Autowired
  private FcIntegrationResultService fcIntegrationResultService;

  @Scheduled(cron = "${fc.receiptplan.cron}", zone = "${time.zoneId}")
  public void fetchReceiptPlansFromFc() {
    final long startTime = System.currentTimeMillis();
    String date = dateHelper.getYesterdayDateStr();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(RECEIPT_PLAN_API, date);
    fcIntegrationResultService.recordFcIntegrationResult(RECEIPT_PLAN_API, date, true,
        callFcCostTimeInSeconds, true, getTotalCostTimeInSeconds(startTime));
  }

  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = "${time.zoneId}")
  public void fetchIssueVouchersFromFc() {
    String date = dateHelper.getYesterdayDateStr();
    fetchDataFromFc(ISSUE_VOUCHER_API, date);
  }

  public Integer fetchDataFromFc(String api, String date) {
    try {
      final long startTime = System.currentTimeMillis();
      initData(api);
      if (date == null || date.isEmpty()) {
        date = dateHelper.getYesterdayDateStr();
      }
      for (int page = 1; page <= callFcService.getPageInfoDto().getTotalPages(); page++) {
        callFcService.fetchData(getUrl(api, page, date), api);
      }
      long costTime = System.currentTimeMillis() - startTime;
      log.info("[FC] fetch {} finish, total size: {}, cost: {}ms", api, getTotalSize(api),
          costTime);
      return Math.toIntExact(costTime / 1000);
    } catch (Exception e) {
      fcIntegrationResultService.recordCallFcFailed(api, date);
      throw e;
    }
  }

  private String getUrl(String path, int page, String date) {
    return domain + path + "?key=" + key + "&psize=20&page=" + page + "&date=" + date;
  }

  @Ignore
  private String getUrlWithPeriod(String path, int page) {
    return domain + path + "?key=" + key + "&psize=20&page=" + page + "period="
        + dateHelper.getCurrentMonthStr();
  }

  private int getTotalSize(String api) {
    if (ISSUE_VOUCHER_API.equals(api)) {
      return callFcService.getIssueVouchers().size();
    } else if (RECEIPT_PLAN_API.equals(api)) {
      return callFcService.getReceiptPlans().size();
    }
    return -1;
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
    return Math.toIntExact((System.currentTimeMillis() - startTime) / 1000);
  }
}
