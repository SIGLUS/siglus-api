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
import static java.util.Comparator.comparing;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;
import org.siglus.siglusapi.service.client.SiglusIssueVoucherService;
import org.siglus.siglusapi.service.client.SiglusReceiptPlanService;
import org.siglus.siglusapi.util.SiglusDateHelper;
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
  private static final int TIMEOUT = 2;

  @Value("${fc.domain}")
  private String fcDomain;

  @Value("${fc.key}")
  private String fcKey;

  private final CallFcService callFcService;
  private final FcIntegrationResultService fcIntegrationResultService;
  private final FcIntegrationCmmCpService fcIntegrationCmmCpService;
  private final FcIssueVoucherService fcIssueVoucherService;
  private final FcReceiptPlanService fcReceiptPlanService;
  private final SiglusReceiptPlanService siglusReceiptPlanService;
  private final SiglusDateHelper dateHelper;
  private final FcProgramService fcProgramService;
  private final FcProductService fcProductService;
  private final FcFacilityTypeService facilityTypeService;
  private final SiglusIssueVoucherService issueVoucherService;
  private final FcRegimenService fcRegimenService;
  private final FcGeographicZoneService fcGeographicZoneService;
  private final FcFacilityService facilityService;
  private final StringRedisTemplate redisTemplate;

  @Scheduled(cron = "${fc.facility.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncFacilities() {
    String redisKey = "syncFacilities";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(FACILITY_API);
        syncFacilities(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncFacilities is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  public void syncFacilities(String date) {
    log.info("schedule start syncFacilities");
    fetchData(FACILITY_API, date);
    Boolean finalSuccess = facilityService.processFacility(callFcService.getFacilities());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(FACILITY_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getFacilities().size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.geographiczone.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncGeographicZones() {
    String redisKey = "syncGeographicZones";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(GEOGRAPHIC_ZONE_API);
        syncGeographicZones(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncGeographicZones is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncGeographicZones(String date) {
    log.info("schedule start syncGeographicZones");
    fetchData(GEOGRAPHIC_ZONE_API, date);
    Boolean finalSuccess = fcGeographicZoneService.processGeographicZones(callFcService.getGeographicZones());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(GEOGRAPHIC_ZONE_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getGeographicZones().size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.receiptplan.cron}", zone = TIME_ZONE_ID)
  public void syncReceiptPlans() {
    String redisKey = "syncReceiptPlans";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API);
        siglusReceiptPlanService.processingReceiptPlans(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncReceiptPlans is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncReceiptPlans(String date) {
    log.info("schedule start syncReceiptPlans");
    fetchData(RECEIPT_PLAN_API, date);
    Boolean finalSuccess = fcReceiptPlanService.processReceiptPlans(callFcService.getReceiptPlans());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(RECEIPT_PLAN_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getReceiptPlans().size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncIssueVouchers() {
    String redisKey = "syncIssueVouchers";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        String date = fcIntegrationResultService.getLatestSuccessDate(ISSUE_VOUCHER_API);
        issueVoucherService.updateIssueVoucher(date);
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncIssueVouchers is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncIssueVouchers(String date) {
    log.info("schedule start syncIssueVouchers");
    fetchData(ISSUE_VOUCHER_API, date);
    Boolean finalSuccess = fcIssueVoucherService.processIssueVouchers(callFcService.getIssueVouchers());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(ISSUE_VOUCHER_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getIssueVouchers().size())
        .finalSuccess(finalSuccess)
        .errorMessage(String.join(";", fcIssueVoucherService.getStatusErrorIssueVoucherNumber()))
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.cmm.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncCmms() {
    String redisKey = "syncCmms";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncCmms(getStartDateForPeriodCall(fcIntegrationResultService.getLatestSuccessDate(CMM_API)));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncCmms is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncCmms(String date) {
    log.info("schedule start syncCmms");
    fetchData(CMM_API, date);
    boolean finalSuccess = fcIntegrationCmmCpService.processCmms(callFcService.getCmms(), date);
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(CMM_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getCmms().size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.cp.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncCps() {
    String redisKey = "syncCps";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncCps(getStartDateForPeriodCall(fcIntegrationResultService.getLatestSuccessDate(CP_API)));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncCps is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncCps(String date) {
    log.info("schedule start syncCps");
    fetchData(CP_API, date);
    boolean finalSuccess = fcIntegrationCmmCpService.processCps(callFcService.getCps(), date);
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(CP_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getCps().size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.program.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncPrograms() {
    String redisKey = "syncPrograms";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncPrograms(fcIntegrationResultService.getLatestSuccessDate(PROGRAM_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncPrograms is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncPrograms(String date) {
    log.info("schedule start syncPrograms");
    fetchData(PROGRAM_API, date);
    boolean finalSuccess = fcProgramService.processPrograms(callFcService.getPrograms());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(PROGRAM_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getPrograms().size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.product.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncProducts() {
    String redisKey = "syncProducts";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncProducts(fcIntegrationResultService.getLatestSuccessDate(PRODUCT_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncProducts is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncProducts(String date) {
    log.info("schedule start syncProducts");
    fetchData(PRODUCT_API, date);
    List<ProductInfoDto> products = callFcService.getProducts();
    String nextStartDate;
    if (products.isEmpty()) {
      nextStartDate = date;
    } else {
      ZonedDateTime lastUpdatedAt = products.stream()
          .max(comparing(ProductInfoDto::getLastUpdatedAt))
          .orElseThrow(EntityNotFoundException::new)
          .getLastUpdatedAt();
      nextStartDate = lastUpdatedAt.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    boolean finalSuccess = fcProductService.processProductData(products);
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(PRODUCT_API)
        .startDate(date)
        .nextStartDate(nextStartDate)
        .totalObjectsFromFc(products.size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.regimen.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncRegimens() {
    String redisKey = "syncRegimens";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncRegimens(fcIntegrationResultService.getLatestSuccessDate(REGIMEN_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncRegimens is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncRegimens(String date) {
    log.info("schedule start syncRegimens");
    fetchData(REGIMEN_API, date);
    boolean finalSuccess = fcRegimenService.processRegimens(callFcService.getRegimens());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(REGIMEN_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getRegimens().size())
        .finalSuccess(finalSuccess)
        .build();
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);
  }

  @Scheduled(cron = "${fc.facilitytype.cron}", zone = TIME_ZONE_ID)
  @Transactional
  public void syncFacilityTypes() {
    String redisKey = "syncFacilityTypes";
    try {
      if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, SYNCHRONIZING))) {
        redisTemplate.opsForValue().set(redisKey, SYNCHRONIZING, TIMEOUT, TimeUnit.HOURS);
        syncFacilityTypes(fcIntegrationResultService.getLatestSuccessDate(FACILITY_TYPE_API));
        Thread.sleep(DELAY);
        redisTemplate.delete(redisKey);
      } else {
        log.info("syncFacilityTypes is synchronizing by another thread.");
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      redisTemplate.delete(redisKey);
      Thread.currentThread().interrupt();
    }
  }

  @Transactional
  public void syncFacilityTypes(String date) {
    log.info("schedule start syncFacilityTypes");
    fetchData(FACILITY_TYPE_API, date);
    boolean finalSuccess = facilityTypeService.processFacilityTypes(callFcService.getFacilityTypes());
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(FACILITY_TYPE_API)
        .startDate(date)
        .totalObjectsFromFc(callFcService.getFacilityTypes().size())
        .finalSuccess(finalSuccess)
        .build();
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

  private String getStartDateForPeriodCall(String date) {
    if (date.equals(dateHelper.getCurrentMonthStr())) {
      return date;
    }
    String[] splitDate = date.split("-");
    LocalDate lastEndDate = LocalDate.of(Integer.parseInt(splitDate[1]), Integer.parseInt(splitDate[0]), 1);
    return lastEndDate.plusMonths(1).format(DateTimeFormatter.ofPattern("MM-yyyy"));
  }

}
