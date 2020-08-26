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

import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
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

  @Scheduled(cron = "${fc.receiptplan.cron}", zone = "${time.zoneId}")
  public void fetchReceiptPlansFromFc() throws Exception {
    fetchDataFromFc(ReceiptPlanDto[].class, RECEIPT_PLAN_API);
  }

  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = "${time.zoneId}")
  public void fetchIssueVouchersFromFc() throws Exception {
    fetchDataFromFc(IssueVoucherDto[].class, ISSUE_VOUCHER_API);
  }

  public <T> void fetchDataFromFc(Class<T[]> clazz, String path) throws Exception {
    fetchDataFromFc(clazz, path, "20200501");
  }

  public <T> void fetchDataFromFc(Class<T[]> clazz, String path, String date) throws Exception {
    final long startTime = System.currentTimeMillis();
    initData(clazz);
    for (int page = 1; page <= callFcService.getPageInfoDto().getTotalPages(); page++) {
      callFcService.fetchData(getUrl(path, page, date), clazz);
    }
    log.info("[FC] fetch {} finish, total size: {}, cost: {}ms", clazz.getSimpleName(),
        getTotalSize(clazz), System.currentTimeMillis() - startTime);
  }

  private String getUrl(String path, int page, String date) {
    if (date == null || date.isEmpty()) {
      date = dateHelper.getYesterdayDateStr();
    }
    return domain + path + "?key=" + key + "&psize=20&page=" + page + "&date=" + date;
  }

  private String getUrlWithPeriod(String path, int page) {
    return domain + path + "?key=" + key + "&psize=20&page=" + page+ "period="
        + dateHelper.getCurrentMonthStr();
  }

  private <T> int getTotalSize(Class<T[]> clazz) {
    if (clazz.equals(IssueVoucherDto[].class)) {
      return callFcService.getIssueVouchers().size();
    }
    return callFcService.getReceiptPlans().size();
  }

  private <T> void initData(Class<T[]> clazz) {
    if (clazz.equals(IssueVoucherDto[].class)) {
      callFcService.setIssueVouchers(new ArrayList<>());
    } else {
      callFcService.setReceiptPlans(new ArrayList<>());
    }
    callFcService.setPageInfoDto(new PageInfoDto());
  }

}
