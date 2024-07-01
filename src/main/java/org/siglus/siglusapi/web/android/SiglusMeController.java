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

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.siglus.siglusapi.constant.android.AndroidConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.android.RequisitionStatusDto;
import org.siglus.siglusapi.dto.android.enumeration.TestProject;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.PatientLineItemColumnRequest;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.request.TestConsumptionLineItemRequest;
import org.siglus.siglusapi.dto.android.response.CreateStockCardResponse;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
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
    service.createRequisition(request);
  }

  @GetMapping("/facility/requisitions")
  public RequisitionResponse getRequisitionResponse(@RequestParam(value = "startDate") String startDate) {
    RequisitionResponse response = service.getRequisitionResponse(startDate);
    return convertRequisitionResponseToV1(response);
  }

  @PostMapping("/facility/requisitions/status")
  public List<RequisitionStatusDto> getRegularRequisitionStatusResponse(
      @RequestBody @Size(min = 1) List<RequisitionStatusDto> request) {
    return service.getRegularRequisitionStatus(request);
  }

  @GetMapping("/facility/pods")
  public List<PodResponse> getProofsOfDelivery(
      @RequestParam(name = "shippedOnly", defaultValue = "false") boolean shippedOnly,
      @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate since) {
    return service.getProofsOfDeliveryWithFilter(since, shippedOnly);
  }

  @PatchMapping("/facility/pod")
  public PodResponse confirmPod(@RequestBody @Valid PodRequest podRequest) {
    return service.confirmPod(podRequest, false);
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

  /**
   * Old Android version can't parser V2 response, so it needs convert V2 to V1 for it
   * New Android version uses /api/v2/siglusapi/android/me to instead
   *
   * @param response RequisitionResponse
   * @return RequisitionResponse
   */
  private RequisitionResponse convertRequisitionResponseToV1(RequisitionResponse response) {
    Set<String> v2TestProjects = new HashSet<>();
    v2TestProjects.add(TestProject.DUOTESTEHIVSIFILIS.name());
    v2TestProjects.add(TestProject.HEPATITEBTESTES.name());
    v2TestProjects.add(TestProject.TDRORALDEHIV.name());
    v2TestProjects.add(TestProject.NEWTEST.name());
    response.getRequisitionResponseList().forEach(requisition -> {
      // TestConsumptionLineItems
      List<TestConsumptionLineItemRequest> testConsumptions = requisition.getTestConsumptionLineItems().stream()
          .filter(lineItem -> !v2TestProjects.contains(lineItem.getTestProject()))
          .collect(Collectors.toList());
      requisition.setTestConsumptionLineItems(testConsumptions);
      // patientLineItemsRequest
      requisition.getPatientLineItems().forEach(patientLineItemsRequest -> {
        List<PatientLineItemColumnRequest> filterColumns = patientLineItemsRequest.getColumns()
            .stream().filter(columnRequest -> Objects.nonNull(columnRequest.getValue()))
            .collect(Collectors.toList());
        patientLineItemsRequest.setColumns(filterColumns);
      });
    });
    return response;
  }
}
