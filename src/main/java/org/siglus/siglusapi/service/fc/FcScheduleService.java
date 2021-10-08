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

import static com.google.common.collect.Lists.newArrayList;
import static org.siglus.siglusapi.constant.FcConstants.CMM_API;
import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.constant.FcConstants.DATE_FORMAT;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_TYPE_API;
import static org.siglus.siglusapi.constant.FcConstants.GEOGRAPHIC_ZONE_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.MONTH_FORMAT;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;
import static org.siglus.siglusapi.constant.FcConstants.REGIMEN_API;
import static org.siglus.siglusapi.constant.FcConstants.getCmmAndCpApis;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.service.client.SiglusIssueVoucherService;
import org.siglus.siglusapi.service.client.SiglusReceiptPlanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public class FcScheduleService {

  private static final String TIME_ZONE_ID = "${time.zoneId}";
  private static final String SYNCHRONIZING = "SYNCHRONIZING";
  private static final int DELAY = 1000;
  private static final int TIMEOUT = 1;

  @Value("${fc.domain}")
  private String fcDomain;

  @Value("${fc.key}")
  private String fcKey;

  private final CallFcService callFcService;
  private final FcIntegrationResultService fcIntegrationResultService;
  private final FcCmmService fcCmmService;
  private final FcCpService fcCpService;
  private final FcIssueVoucherService fcIssueVoucherService;
  private final FcReceiptPlanService fcReceiptPlanService;
  private final SiglusReceiptPlanService siglusReceiptPlanService;
  private final FcProgramService fcProgramService;
  private final FcProductService fcProductService;
  private final FcFacilityTypeService fcFacilityTypeService;
  private final SiglusIssueVoucherService issueVoucherService;
  private final FcRegimenService fcRegimenService;
  private final FcGeographicZoneService fcGeographicZoneService;
  private final FcFacilityService fcFacilityService;
  private final StringRedisTemplate redisTemplate;


  @Scheduled(cron = "${fc.cmm.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncCmmsScheduler() {
    String redisKey = "syncCmms";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncCmms("");
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("[FC cmm] cmm is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Scheduled(cron = "${fc.cp.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncCpsScheduler() {
    String redisKey = "syncCps";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncCps("");
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("[FC cp] cp is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Scheduled(cron = "${fc.receiptplan.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncReceiptPlansScheduler() {
    String redisKey = "syncReceiptPlans";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        ZonedDateTime lastUpdatedAt = fcIntegrationResultService.getLastUpdatedAt(RECEIPT_PLAN_API);
        siglusReceiptPlanService.processingReceiptPlans(lastUpdatedAt.format(getFormatter(RECEIPT_PLAN_API)));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("[FC receiptPlan] receiptPlans is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }


  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncIssueVouchersScheduler() {
    String redisKey = "syncIssueVouchers";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        ZonedDateTime lastUpdatedAt = fcIntegrationResultService.getLastUpdatedAt(ISSUE_VOUCHER_API);
        issueVoucherService.updateIssueVoucher(lastUpdatedAt.format(getFormatter(ISSUE_VOUCHER_API)));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("[FC issueVoucher] issueVouchers is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }
  
  @Transactional
  public void syncCmms(String date) {
    log.info("[FC cmm] start sync");
    process(CMM_API, date);
  }

  @Transactional
  public void syncCps(String date) {
    log.info("[FC cp] start sync");
    process(CP_API, date);
  }

  @Transactional
  public void syncReceiptPlans(String date) {
    log.info("[FC receiptPlan] start sync");
    process(RECEIPT_PLAN_API, date);
  }

  @Transactional
  public void syncIssueVouchers(String date) {
    log.info("[FC issueVoucher] start sync");
    process(ISSUE_VOUCHER_API, date);
  }

  @Transactional
  public void syncFacilities(String date) {
    log.info("[FC facility] start sync");
    process(FACILITY_API, date);
  }

  @Transactional
  public void syncGeographicZones(String date) {
    log.info("[FC facility] start sync");
    process(GEOGRAPHIC_ZONE_API, date);
  }

  @Transactional
  public void syncPrograms(String date) {
    log.info("[FC program] start sync");
    process(PROGRAM_API, date);
  }

  @Transactional
  public void syncProducts(String date) {
    log.info("[FC product] start sync");
    process(PRODUCT_API, date);
  }

  @Transactional
  public void syncRegimens(String date) {
    log.info("[FC regimen] start sync");
    process(REGIMEN_API, date);
  }

  @Transactional
  public void syncFacilityTypes(String date) {
    log.info("[FC facilityType] start sync");
    process(FACILITY_TYPE_API, date);
  }

  private void process(String api, String date) {
    ZonedDateTime lastUpdatedAt = fcIntegrationResultService.getLastUpdatedAt(api);
    if (StringUtils.isEmpty(date)) {
      date = lastUpdatedAt.format(getFormatter(api));
    }
    fetchData(api, date);
    processAndRecordResult(api, date, lastUpdatedAt);
  }

  private DateTimeFormatter getFormatter(String api) {
    return DateTimeFormatter.ofPattern(getCmmAndCpApis().contains(api) ? MONTH_FORMAT : DATE_FORMAT);
  }

  private void processAndRecordResult(String api, String date, ZonedDateTime lastUpdatedAt) {
    List<? extends ResponseBaseDto> result = Collections.emptyList();
    ProcessDataService processDataService = null;
    if (PRODUCT_API.equals(api)) {
      result = callFcService.getProducts();
      processDataService = fcProductService;
    } else if (REGIMEN_API.equals(api)) {
      result = callFcService.getRegimens();
      processDataService = fcRegimenService;
    } else if (FACILITY_TYPE_API.equals(api)) {
      result = callFcService.getFacilityTypes();
      processDataService = fcFacilityTypeService;
    } else if (PROGRAM_API.equals(api)) {
      result = callFcService.getPrograms();
      processDataService = fcProgramService;
    } else if (FACILITY_API.equals(api)) {
      result = callFcService.getFacilities();
      processDataService = fcFacilityService;
    } else if (GEOGRAPHIC_ZONE_API.equals(api)) {
      result = callFcService.getGeographicZones();
      processDataService = fcGeographicZoneService;
    } else if (CMM_API.equals(api)) {
      result = callFcService.getCmms();
      processDataService = fcCmmService;
    } else if (CP_API.equals(api)) {
      result = callFcService.getCps();
      processDataService = fcCpService;
    } else if (RECEIPT_PLAN_API.equals(api)) {
      result = callFcService.getReceiptPlans();
      processDataService = fcReceiptPlanService;
    } else if (ISSUE_VOUCHER_API.equals(api)) {
      result = callFcService.getIssueVouchers();
      processDataService = fcIssueVoucherService;
    }
    if (result.isEmpty() || processDataService == null) {
      log.info("no new data for {}", api);
      return;
    }
    FcIntegrationResultDto resultDto = processDataService.processData(result, date, lastUpdatedAt);
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  public void fetchData(String api, String date) {
    try {
      initData(api);
      for (int page = 1; page <= callFcService.getPageInfoDto().getTotalPages(); page++) {
        callFcService.fetchData(getUrl(api, page, date), api);
      }
      log.info("[FC] fetch {} finish, total size: {}", api, getTotalSize(api));
    } catch (Exception e) {
      log.error("[FC] fetch api {} failed", api);
      throw e;
    }
  }

  private String getUrl(String path, int page, String date) {
    String url = fcDomain + path + "?key=" + fcKey + "&psize=20&page=" + page + "&";
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
    } else if (GEOGRAPHIC_ZONE_API.equals(api)) {
      return callFcService.getGeographicZones().size();
    } else if (PROGRAM_API.equals(api)) {
      return callFcService.getPrograms().size();
    } else if (PRODUCT_API.equals(api)) {
      return callFcService.getProducts().size();
    } else if (REGIMEN_API.equals(api)) {
      return callFcService.getRegimens().size();
    } else if (FACILITY_API.equals(api)) {
      return callFcService.getFacilities().size();
    } else if (FACILITY_TYPE_API.equals(api)) {
      return callFcService.getFacilityTypes().size();
    } else if (CMM_API.equals(api)) {
      return callFcService.getCmms().size();
    } else if (CP_API.equals(api)) {
      return callFcService.getCps().size();
    } else {
      return -1;
    }
  }

  private void initData(String api) {
    if (RECEIPT_PLAN_API.equals(api)) {
      callFcService.setReceiptPlans(newArrayList());
    } else if (ISSUE_VOUCHER_API.equals(api)) {
      callFcService.setIssueVouchers(newArrayList());
    } else if (CMM_API.equals(api)) {
      callFcService.setCmms(newArrayList());
    } else if (CP_API.equals(api)) {
      callFcService.setCps(newArrayList());
    } else if (FACILITY_API.equals(api)) {
      callFcService.setFacilities(newArrayList());
    } else if (FACILITY_TYPE_API.equals(api)) {
      callFcService.setFacilityTypes(newArrayList());
    } else if (REGIMEN_API.equals(api)) {
      callFcService.setRegimens(newArrayList());
    } else if (GEOGRAPHIC_ZONE_API.equals(api)) {
      callFcService.setGeographicZones(newArrayList());
    } else if (PROGRAM_API.equals(api)) {
      callFcService.setPrograms(newArrayList());
    } else if (PRODUCT_API.equals(api)) {
      callFcService.setProducts(newArrayList());
    }
    callFcService.setPageInfoDto(new PageInfoDto());
  }

}
