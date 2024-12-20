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
import static org.siglus.siglusapi.constant.FcConstants.DISTRICT_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_TYPE_API;
import static org.siglus.siglusapi.constant.FcConstants.GEOGRAPHIC_ZONE_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.MONTH_FORMAT;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_API;
import static org.siglus.siglusapi.constant.FcConstants.PROVINCE_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;
import static org.siglus.siglusapi.constant.FcConstants.REGIMEN_API;
import static org.siglus.siglusapi.constant.FcConstants.REGION_API;
import static org.siglus.siglusapi.constant.FcConstants.getCmmAndCpApis;
import static org.siglus.siglusapi.constant.FcConstants.getFcNewApis;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.siglus.siglusapi.domain.FcIntegrationChanges;
import org.siglus.siglusapi.domain.FcIntegrationResult;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.FcIntegrationChangesRepository;
import org.siglus.siglusapi.service.client.SiglusIssueVoucherService;
import org.siglus.siglusapi.service.client.SiglusReceiptPlanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("!localmachine")
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

  @Value("${fc.domain.new}")
  private String fcNewDomain;

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
  private final FcIntegrationChangesRepository fcIntegrationChangesRepository;
  private final FcRegionService fcRegionService;
  private final FcProvinceService fcProvinceService;
  private final FcDistrictService fcDistrictService;

  @Scheduled(cron = "${fc.receiptplan.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncReceiptPlansScheduler() {
    String redisKey = "syncReceiptPlans";
    processTask(redisKey, () -> {
      ZonedDateTime lastUpdatedAt = fcIntegrationResultService.getLastUpdatedAt(RECEIPT_PLAN_API);
      siglusReceiptPlanService.processingReceiptPlans(
          lastUpdatedAt.format(getFormatter(RECEIPT_PLAN_API)));
    });
  }

  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncIssueVouchersScheduler() {
    String redisKey = "syncIssueVouchers";
    processTask(redisKey, () -> {
      ZonedDateTime lastUpdatedAt = fcIntegrationResultService.getLastUpdatedAt(ISSUE_VOUCHER_API);
      issueVoucherService.updateIssueVoucher(lastUpdatedAt.format(getFormatter(ISSUE_VOUCHER_API)));
    });
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
    log.info("[FC geographic] start sync");
    process(REGION_API, date);
    process(PROVINCE_API, date);
    process(DISTRICT_API, date);
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

  private void processTask(String redisKey, Runnable task) {
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        try {
          redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
          task.run();
          Thread.sleep(DELAY);
        } finally {
          redisTemplate.delete(redisKey);
        }
      } else {
        log.info("[FC {}] {} is synchronizing by another thread.", redisKey, redisKey);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
    }
  }

  private void process(String api, String date) {
    ZonedDateTime lastUpdatedAt = fcIntegrationResultService.getLastUpdatedAt(api);
    if (StringUtils.isEmpty(date)) {
      date = lastUpdatedAt.format(getFormatter(api));
    } else if (getCmmAndCpApis().contains(api)) {
      LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_FORMAT));
      date = localDate.format(getFormatter(api));
    }
    if (getFcNewApis().contains(api)) {
      fetchDataForNewFc(api, date);
    } else {
      fetchData(api, date);
    }
    processAndRecordResult(api, date, lastUpdatedAt);
  }

  private DateTimeFormatter getFormatter(String api) {
    return DateTimeFormatter.ofPattern(
        getCmmAndCpApis().contains(api) ? MONTH_FORMAT : DATE_FORMAT);
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
    } else if (REGION_API.equals(api)) {
      result = callFcService.getRegions();
      processDataService = fcRegionService;
    } else if (PROVINCE_API.equals(api)) {
      result = callFcService.getProvinces();
      processDataService = fcProvinceService;
    } else if (DISTRICT_API.equals(api)) {
      result = callFcService.getDistrict();
      processDataService = fcDistrictService;
    }
    if (result.isEmpty() || processDataService == null) {
      log.info("no new data for {}", api);
      return;
    }
    FcIntegrationResultDto resultDto = processDataService.processData(result, date, lastUpdatedAt);
    FcIntegrationResult fcIntegrationResult = fcIntegrationResultService.recordFcIntegrationResult(
        resultDto);
    recordFcIntegrationChanges(fcIntegrationResult, resultDto.getFcIntegrationChanges());
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

  public void fetchDataForNewFc(String api, String date) {
    try {
      initData(api);
      for (int page = 0; page <= callFcService.getPageInfoDto().getTotalPages(); page++) {
        callFcService.fetchDataForNewFc(getNewUrl(api, page, date), api, callFcService.getClassByApi(api));
      }
      log.info("[FC] fetch {} finish, total size: {}", api, getTotalSize(api));
    } catch (Exception e) {
      log.error("[FC] fetch api {} failed", api);
      throw e;
    }
  }

  private void recordFcIntegrationChanges(FcIntegrationResult fcIntegrationResult,
      List<FcIntegrationChanges> fcIntegrationChanges) {
    if (fcIntegrationResult == null || CollectionUtils.isEmpty(fcIntegrationChanges)) {
      return;
    }
    fcIntegrationChanges.forEach(content -> {
      content.setResultId(fcIntegrationResult.getId());
      fcIntegrationChangesRepository.save(content);
    });
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

  private String getNewUrl(String path, int page, String date) {
    String url = fcNewDomain + path + "?key=" + fcKey + "&psize=20&page=" + page + "&";
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
    } else if (REGION_API.equals(api)) {
      return callFcService.getRegions().size();
    } else if (PROVINCE_API.equals(api)) {
      return callFcService.getProvinces().size();
    } else if (DISTRICT_API.equals(api)) {
      return callFcService.getDistrict().size();
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
    } else if (REGION_API.equals(api)) {
      callFcService.setRegions(newArrayList());
    } else if (PROVINCE_API.equals(api)) {
      callFcService.setProvinces(newArrayList());
    } else if (DISTRICT_API.equals(api)) {
      callFcService.setDistrict(newArrayList());
    }
    callFcService.setPageInfoDto(new PageInfoDto());
  }

}
