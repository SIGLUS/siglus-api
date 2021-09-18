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
import static java.lang.System.currentTimeMillis;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.service.client.SiglusIssueVoucherService;
import org.siglus.siglusapi.service.client.SiglusReceiptPlanService;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public class FcScheduleService {

  private static final String TIME_ZONE_ID = "${time.zoneId}";
  private static final String SYNCHRONIZING = "SYNCHRONIZING";
  private static final int DELAY = 1000;
  private static final int TIMEOUT = 2;

  @Value("${fc.domain}")
  private String fcDomain;

  @Value("${fc.key}")
  private String fcKey;

  @Autowired
  private CallFcService callFcService;

  @Autowired
  private FcIntegrationResultService fcIntegrationResultService;

  @Autowired
  private FcIntegrationCmmCpService fcIntegrationCmmCpService;

  @Autowired
  private FcIssueVoucherService fcIssueVoucherService;

  @Autowired
  private FcReceiptPlanService fcReceiptPlanService;

  @Autowired
  private SiglusReceiptPlanService siglusReceiptPlanService;

  @Autowired
  private SiglusDateHelper dateHelper;

  @Autowired
  private FcProgramService fcProgramService;

  @Autowired
  private FcProductService fcProductService;

  @Autowired
  private FcFacilityTypeService facilityTypeService;

  @Autowired
  private SiglusIssueVoucherService issueVoucherService;

  @Autowired
  private FcRegimenService fcRegimenService;

  @Autowired
  private FcGeographicZoneService fcGeographicZoneService;

  @Autowired
  private FcFacilityService facilityService;

  @Autowired
  private StringRedisTemplate redisTemplate;


  @Scheduled(cron = "${fc.facility.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncFacilityFromFc() {
    String redisKey = "syncFacilityFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(FACILITY_API);
        syncFacilityFromFc(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncFacilityFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  public void syncFacilityFromFc(String date) {
    log.info("schedule start syncFacilityFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(FACILITY_API, date);
    Boolean finalSuccess = facilityService.processFacility(
        callFcService.getFacilities());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(FACILITY_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getFacilities().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.geographiczone.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncGeographicZoneFromFc() {
    String redisKey = "syncGeographicZoneFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(GEOGRAPHIC_ZONE_API);
        syncGeographicZoneFromFc(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncGeographicZoneFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncGeographicZoneFromFc(String date) {
    log.info("schedule start syncGeographicZoneFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(GEOGRAPHIC_ZONE_API, date);
    Boolean finalSuccess = fcGeographicZoneService.processGeographicZones(callFcService.getGeographicZones());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(GEOGRAPHIC_ZONE_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getGeographicZones().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.receiptplan.cron}", zone = TIME_ZONE_ID)
  public void syncReceiptPlanFromFc() {
    String redisKey = "syncReceiptPlanFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API);
        siglusReceiptPlanService.processingReceiptPlans(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncReceiptPlanFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncReceiptPlanFromFc(String date) {
    log.info("schedule start syncReceiptPlanFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(RECEIPT_PLAN_API, date);
    Boolean finalSuccess = fcReceiptPlanService.processReceiptPlans(callFcService.getReceiptPlans());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(RECEIPT_PLAN_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getReceiptPlans().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncIssueVoucherFromFc() {
    String redisKey = "syncIssueVoucherFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(ISSUE_VOUCHER_API);
        issueVoucherService.updateIssueVoucher(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncIssueVoucherFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncIssueVoucherFromFc(String date) {
    log.info("schedule start syncIssueVoucherFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(ISSUE_VOUCHER_API, date);
    Boolean finalSuccess = fcIssueVoucherService.processIssueVouchers(
        callFcService.getIssueVouchers());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(ISSUE_VOUCHER_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getIssueVouchers().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .errorMessage(String.join(";",
            fcIssueVoucherService.getStatusErrorIssueVoucherNumber()))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.cmm.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncCmmFromFc() {
    String redisKey = "syncCmmFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncCmmFromFc(
            getStartDateForPeriodCall(fcIntegrationResultService.getLatestSuccessDate(CMM_API)));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncCmmFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncCmmFromFc(String date) {
    log.info("schedule start syncCmmFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(CMM_API, date);
    boolean finalSuccess = fcIntegrationCmmCpService.processCmms(callFcService.getCmms(), date);
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
  @Transactional
  public void syncCpFromFc() {
    String redisKey = "syncCpFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncCpFromFc(
            getStartDateForPeriodCall(fcIntegrationResultService.getLatestSuccessDate(CP_API)));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncCpFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncCpFromFc(String date) {
    log.info("schedule start syncCpFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(CP_API, date);
    boolean finalSuccess = fcIntegrationCmmCpService.processCps(callFcService.getCps(), date);
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

  @Scheduled(cron = "${fc.program.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncProgramFromFc() {
    String redisKey = "syncProgramFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncProgramFromFc(fcIntegrationResultService.getLatestSuccessDate(PROGRAM_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncProgramFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncProgramFromFc(String date) {
    log.info("schedule start syncProgramFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(PROGRAM_API, date);
    boolean finalSuccess = fcProgramService.processPrograms(callFcService.getPrograms());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(PROGRAM_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getPrograms().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.product.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncProductFromFc() {
    String redisKey = "syncProductFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncProductFromFc(fcIntegrationResultService.getLatestSuccessDate(PRODUCT_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncProductFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncProductFromFc(String date) {
    log.info("schedule start syncProductFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(PRODUCT_API, date);
    boolean finalSuccess = fcProductService.processProductData(callFcService.getProducts());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(PRODUCT_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getProducts().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.regimen.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncRegimenFromFc() {
    String redisKey = "syncRegimenFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncRegimenFromFc(fcIntegrationResultService.getLatestSuccessDate(REGIMEN_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncRegimenFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncRegimenFromFc(String date) {
    log.info("schedule start syncRegimenFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(REGIMEN_API, date);
    boolean finalSuccess = fcRegimenService.processRegimens(callFcService.getRegimens());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(REGIMEN_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getRegimens().size())
        .callFcSuccess(true)
        .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
        .finalSuccess(finalSuccess)
        .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.facilitytype.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncFacilityTypeFromFc() {
    String redisKey = "syncFacilityTypeFromFc";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncFacilityTypeFromFc(fcIntegrationResultService.getLatestSuccessDate(FACILITY_TYPE_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncFacilityTypeFromFc is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncFacilityTypeFromFc(String date) {
    log.info("schedule start syncFacilityTypeFromFc");
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(FACILITY_TYPE_API, date);
    boolean finalSuccess = facilityTypeService.processFacilityTypes(
        callFcService.getFacilityTypes());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(FACILITY_TYPE_API)
        .date(date)
        .totalObjectsFromFc(callFcService.getFacilityTypes().size())
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
      log.info("[FC] fetch {} finish, total size: {}, cost: {}ms", api, getTotalSize(api), costTime);
      return Math.toIntExact(costTime / 1000);
    } catch (Exception e) {
      fcIntegrationResultService.recordCallFcFailed(api, date);
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

  private Integer getTotalCostTimeInSeconds(long startTime) {
    return Math.toIntExact((currentTimeMillis() - startTime) / 1000);
  }

  private String getStartDateForPeriodCall(String date) {
    if (date.equals(dateHelper.getCurrentMonthStr())) {
      return date;
    }
    String[] splitDate = date.split("-");
    LocalDate lastEndDate = LocalDate.of(Integer.parseInt(splitDate[1]),
        Integer.parseInt(splitDate[0]), 1);
    return lastEndDate.plusMonths(1).format(DateTimeFormatter.ofPattern("MM-yyyy"));
  }

}
