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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.fc.CmmDto;
import org.siglus.siglusapi.dto.fc.CpDto;
import org.siglus.siglusapi.dto.fc.FcFacilityDto;
import org.siglus.siglusapi.dto.fc.FcFacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneNationalDto;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;
import org.siglus.siglusapi.dto.fc.ProgramDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.dto.fc.RegimenDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@Getter
@Setter
@SuppressWarnings("PMD.CyclomaticComplexity")
public class CallFcService {

  private List<IssueVoucherDto> issueVouchers = new ArrayList<>();
  private List<ReceiptPlanDto> receiptPlans = new ArrayList<>();
  private List<CmmDto> cmms = new ArrayList<>();
  private List<CpDto> cps = new ArrayList<>();
  private List<ProgramDto> programs = new ArrayList<>();
  private List<ProductInfoDto> products = new ArrayList<>();
  private List<RegimenDto> regimens = new ArrayList<>();
  private List<FcFacilityDto> facilities = new ArrayList<>();
  private List<FcFacilityTypeDto> facilityTypes = new ArrayList<>();
  private List<FcGeographicZoneNationalDto> geographicZones = new ArrayList<>();
  private PageInfoDto pageInfoDto = new PageInfoDto();
  private static final Map<String, Class> apiToClassMap = new HashMap<>();

  @Autowired
  RestTemplate remoteRestTemplate;

  static {
    apiToClassMap.put(ISSUE_VOUCHER_API, IssueVoucherDto[].class);
    apiToClassMap.put(RECEIPT_PLAN_API, ReceiptPlanDto[].class);
    apiToClassMap.put(PROGRAM_API, ProgramDto[].class);
    apiToClassMap.put(PRODUCT_API, ProductInfoDto[].class);
    apiToClassMap.put(REGIMEN_API, RegimenDto[].class);
    apiToClassMap.put(CMM_API, CmmDto[].class);
    apiToClassMap.put(CP_API, CpDto[].class);
    apiToClassMap.put(FACILITY_TYPE_API, FcFacilityTypeDto[].class);
    apiToClassMap.put(FACILITY_API, FcFacilityDto[].class);
    apiToClassMap.put(GEOGRAPHIC_ZONE_API, FcGeographicZoneNationalDto[].class);
  }

  @Retryable(value = Exception.class, backoff = @Backoff(delay = 2000, multiplier = 2))
  public void fetchData(String url, String api) {
    String param = url.split("psize=20&")[1];
    log.info("[FC] fetch {}: {}", api, param);
    try {
      ResponseEntity<Object[]> responseEntity = remoteRestTemplate.getForEntity(url,
          getClassByApi(api));
      Object[] body = responseEntity.getBody();
      if (body.length == 0) {
        log.info("[FC] fetch {}: no result returned from fc", api);
        return;
      }
      setPageInfo(responseEntity.getHeaders());
      updateResponseResult(api, body);
    } catch (Exception e) {
      log.warn("[FC] fetch {} {} failed: {}, retry...", api, param, e.getMessage());
      throw e;
    }
  }

  @Recover
  public void recover(RuntimeException e) {
    log.error("[FC] call fc api failed with retry 5 times, message: {}", e.getMessage());
    throw e;
  }

  public Class getClassByApi(String api) {
    if (apiToClassMap.containsKey(api)) {
      return apiToClassMap.get(api);
    }
    return null;
  }

  private void updateResponseResult(String api, Object[] body) {
    if (RECEIPT_PLAN_API.equals(api)) {
      this.receiptPlans.addAll(Arrays.asList((ReceiptPlanDto[]) body));
    } else if (ISSUE_VOUCHER_API.equals(api)) {
      this.issueVouchers.addAll(Arrays.asList((IssueVoucherDto[]) body));
    } else if (REGIMEN_API.equals(api)) {
      this.regimens.addAll(Arrays.asList((RegimenDto[]) body));
    } else if (CMM_API.equals(api)) {
      this.cmms.addAll(Arrays.asList((CmmDto[]) body));
    } else if (CP_API.equals(api)) {
      this.cps.addAll(Arrays.asList((CpDto[]) body));
    } else if (PROGRAM_API.equals(api)) {
      this.programs.addAll(Arrays.asList((ProgramDto[]) body));
    } else if (PRODUCT_API.equals(api)) {
      this.products.addAll(Arrays.asList((ProductInfoDto[]) body));
    } else if (FACILITY_TYPE_API.equals(api)) {
      this.facilityTypes.addAll(Arrays.asList((FcFacilityTypeDto[]) body));
    } else if (FACILITY_API.equals(api)) {
      this.facilities.addAll(Arrays.asList((FcFacilityDto[]) body));
    } else if (GEOGRAPHIC_ZONE_API.equals(api)) {
      this.geographicZones.addAll(Arrays.asList((FcGeographicZoneNationalDto[]) body));
    }
  }

  private void setPageInfo(HttpHeaders headers) {
    int totalObjects = Integer.parseInt(headers.get("TotalObjects").get(0));
    int totalPages = Integer.parseInt(headers.get("TotalPages").get(0));
    int pageNumber = Integer.parseInt(headers.get("PageNumber").get(0));
    int pageSize = Integer.parseInt(headers.get("PSize").get(0));
    this.pageInfoDto = PageInfoDto.builder()
        .totalObjects(totalObjects)
        .totalPages(totalPages)
        .pageNumber(pageNumber)
        .pageSize(pageSize)
        .build();
    if (pageNumber == 1) {
      log.info("[FC] page info: {}", pageInfoDto);
    }
  }

}
