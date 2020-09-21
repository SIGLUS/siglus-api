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
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.service.client.SiglusIssueVoucherService;
import org.siglus.siglusapi.service.client.SiglusReceiptPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
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


  @Scheduled(cron = "${fc.facility.cron}", zone = TIME_ZONE_ID)
  public void fetchFacility() {
    String date = fcIntegrationResultService.getLatestSuccessDate(FACILITY_API);
    fetchFacilityFromFc(date);
  }

  public void fetchFacilityFromFc(String date) {
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
  public void fetchGeographicZones() {
    String date = fcIntegrationResultService.getLatestSuccessDate(GEOGRAPHIC_ZONE_API);
    fetchGeographicZonesFromFc(date);
  }

  public void fetchGeographicZonesFromFc(String date) {
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(GEOGRAPHIC_ZONE_API, date);
    Boolean finalSuccess = fcGeographicZoneService.processGeographicZones(
        callFcService.getGeographicZones());
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
  public void fetchReceiptPlan() {
    String date = fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API);
    siglusReceiptPlanService.processingReceiptPlans(date);
  }

  public void fetchReceiptPlansFromFc(String date) {
    final long startTime = currentTimeMillis();
    Integer callFcCostTimeInSeconds = fetchDataFromFc(RECEIPT_PLAN_API, date);
    Boolean finalSuccess = fcReceiptPlanService.processReceiptPlans(
        callFcService.getReceiptPlans());
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
  public void fetchIssueVouchersFromFc() {
    String date = fcIntegrationResultService.getLatestSuccessDate(ISSUE_VOUCHER_API);
    issueVoucherService.updateIssueVourch(date);
  }

  public void fetchIssueVouchersFromFc(String date) {
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
  public void fetchCmmsFromFc() {
    String latestSuccessDate;
    String queryDate;
    do {
      final long startTime = currentTimeMillis();
      latestSuccessDate = fcIntegrationResultService.getLatestSuccessDate(CMM_API);
      queryDate = getStartDateForPeriodCall(latestSuccessDate);
      Integer callFcCostTimeInSeconds = fetchDataFromFc(CMM_API, queryDate);
      boolean finalSuccess = fcIntegrationCmmCpService.processCmms(callFcService.getCmms(),
          queryDate);
      FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
          .api(CMM_API)
          .date(queryDate)
          .totalObjectsFromFc(callFcService.getCmms().size())
          .callFcSuccess(true)
          .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
          .finalSuccess(finalSuccess)
          .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
          .build();
      fcIntegrationResultService.recordFcIntegrationResult(resultDto);
    } while (!dateHelper.getCurrentMonthStr().equals(queryDate));
  }

  @Scheduled(cron = "${fc.cp.cron}", zone = TIME_ZONE_ID)
  public void fetchCpsFromFc() {
    String latestSuccessDate;
    String queryDate;
    do {
      final long startTime = currentTimeMillis();
      latestSuccessDate = fcIntegrationResultService.getLatestSuccessDate(CP_API);
      queryDate = getStartDateForPeriodCall(latestSuccessDate);
      Integer callFcCostTimeInSeconds = fetchDataFromFc(CP_API, queryDate);
      boolean finalSuccess = fcIntegrationCmmCpService.processCps(callFcService.getCps(),
          queryDate);
      FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
          .api(CP_API)
          .date(queryDate)
          .totalObjectsFromFc(callFcService.getCps().size())
          .callFcSuccess(true)
          .callFcCostTimeInSeconds(callFcCostTimeInSeconds)
          .finalSuccess(finalSuccess)
          .totalCostTimeInSeconds(getTotalCostTimeInSeconds(startTime))
          .build();
      fcIntegrationResultService.recordFcIntegrationResult(resultDto);
    } while (!dateHelper.getCurrentMonthStr().equals(queryDate));
  }

  @Scheduled(cron = "${fc.program.cron}", zone = TIME_ZONE_ID)
  public void fetchProgramsFromFcForScheduled() {
    String date = fcIntegrationResultService.getLatestSuccessDate(PROGRAM_API);
    fetchProgramsFromFc(date);
  }

  public void fetchProgramsFromFc(String date) {
    final long startTime = currentTimeMillis();
    log.info("date: {}", date);
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
  public void fetchProductsFromFc() {
    String date = fcIntegrationResultService.getLatestSuccessDate(PRODUCT_API);
    fetchProductsFromFc(date);
  }

  public void fetchProductsFromFc(String date) {
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
  public void fetchRegimenFromFcForScheduled() {
    String date = fcIntegrationResultService.getLatestSuccessDate(REGIMEN_API);
    fetchRegimenFromFc(date);
  }

  public void fetchRegimenFromFc(String date) {
    final long startTime = currentTimeMillis();
    log.info("date: {}", date);
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
  public void fetchFacilityTypeFromFcForScheduled() {
    String date = fcIntegrationResultService.getLatestSuccessDate(FACILITY_TYPE_API);
    fetchFacilityTypeFromFc(date);
  }

  public void fetchFacilityTypeFromFc(String date) {
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
    } else {
      return callFcService.getCps().size();
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
