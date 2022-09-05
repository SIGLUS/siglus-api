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

package org.siglus.siglusapi.web.android;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.siglus.siglusapi.constant.AndroidConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.response.CreateStockCardResponse;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.service.android.MeService;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Slf4j
@RestController
@RequestMapping("/api/siglusapi/android/me")
@RequiredArgsConstructor
public class SiglusMeController {

  private final MeService service;

  @GetMapping("/facility")
  public FacilityResponse getFacility() {
    return service.getCurrentFacility();
  }

  @PostMapping("/facility/archivedProducts")
  @ResponseStatus(NO_CONTENT)
  public void archiveAllProducts(@RequestBody List<String> productCodes) {
    service.archiveAllProducts(productCodes);
  }

  @GetMapping("/facility/products")
  public ProductSyncResponse getFacilityProducts(
      @RequestParam(name = "lastSyncTime", required = false) Long lastSyncTime) {
    Instant lastSync = lastSyncTime != null ? Instant.ofEpochMilli(lastSyncTime) : null;
    return service.getFacilityProducts(lastSync);
  }

  @PostMapping("/facility/stockCards")
  @ResponseStatus(CREATED)
  public CreateStockCardResponse createStockCards(@RequestBody @Valid @NotEmpty List<StockCardCreateRequest> requests) {
    return service.createStockCards(requests);
  }

  @GetMapping("/facility/stockCards")
  public FacilityProductMovementsResponse getFacilityStockCards(
      @RequestParam(value = "startTime", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      @Nullable LocalDate since,
      @RequestParam(value = "endTime", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      @Nullable LocalDate tillExclusive) {
    return service.getProductMovements(since, tillExclusive);
  }

  @DeleteMapping("/facility/stockCards")
  @ResponseStatus(NO_CONTENT)
  public void deleteAdditionalOrderable(@RequestBody List<StockCardDeleteRequest> requests) {
    service.deleteStockCardByProduct(requests);
  }

  @PostMapping("/facility/cmms")
  @ResponseStatus(NO_CONTENT)
  public void updateCmmsForFacility(@RequestBody List<HfCmmDto> hfCmmDtos) {
    service.processHfCmms(hfCmmDtos);
  }

  @PostMapping("/facility/requisitions")
  @ResponseStatus(CREATED)
  public void createRequisition(@RequestBody @Valid RequisitionCreateRequest request) {
    try {
      // TODO: need delete add implement ( 2022/9/2 by kourengang)
      log.info("createRequisitionJson: "
          + PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER.writeValueAsString(request));
    } catch (JsonProcessingException e) {
      log.error("request serial failed, msg = " + e.getMessage());
    }
    service.createRequisition(request);
  }

  @GetMapping("/facility/requisitions")
  public RequisitionResponse getRequisitionResponse(@RequestParam(value = "startDate") String startDate) {
    return service.getRequisitionResponse(startDate);
  }

  @GetMapping("/facility/pods")
  public List<PodResponse> getProofsOfDelivery(
      @RequestParam(name = "shippedOnly", defaultValue = "false") boolean shippedOnly,
      @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate since) {
    return service.getProofsOfDeliveryWithFilter(since, shippedOnly);
  }

  @PatchMapping("/facility/pod")
  public PodResponse confirmPod(@RequestBody @Valid PodRequest podRequest) {
    return service.confirmPod(podRequest);
  }

  @PostMapping("/app-info")
  @ResponseStatus(NO_CONTENT)
  public void processAppInfo(HttpServletRequest httpServletRequest) {
    AppInfo appInfo = new AppInfo();
    BeanUtils.copyProperties(AndroidConstants.getAndroidHeader(httpServletRequest), appInfo);
    service.processAppInfo(appInfo);
  }

  @PostMapping("/resync-info")
  @ResponseStatus(NO_CONTENT)
  public void processResyncInfo(HttpServletRequest httpServletRequest) {
    AndroidHeader header = AndroidConstants.getAndroidHeader(httpServletRequest);
    service.processResyncInfo(header);
  }
}