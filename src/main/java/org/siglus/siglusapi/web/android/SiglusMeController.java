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
import static org.springframework.http.HttpStatus.OK;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.siglus.siglusapi.constant.AndroidConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.android.constraint.stockcard.LotStockConsistentWithExisted;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductConsistentWithAllLots;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByLot;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByProduct;
import org.siglus.siglusapi.dto.android.group.PerformanceGroup;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.service.android.SiglusMeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/siglusapi/android/me")
@RequiredArgsConstructor
public class SiglusMeController {

  private final SiglusMeService service;

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
  public void createStockCards(
      @RequestBody
      @Valid @Validated(PerformanceSequence.class)
      @NotEmpty
      @StockOnHandConsistentWithQuantityByProduct
      @StockOnHandConsistentWithQuantityByLot
      @ProductConsistentWithAllLots
      @LotStockConsistentWithExisted(groups = PerformanceGroup.class)
          List<StockCardCreateRequest> requests) {
    service.createStockCards(requests);
  }

  @GetMapping("/facility/stockCards")
  @ResponseStatus(OK)
  @ResponseBody
  public FacilityProductMovementsResponse getFacilityStockCards(
      @RequestParam(value = "startTime") String startTime,
      @RequestParam(value = "endTime") String endTime) {
    return service.getProductMovements(startTime, endTime);
  }

  @PutMapping(value = "/facility/cmms")
  @ResponseStatus(NO_CONTENT)
  public void updateCmmsForFacility(@RequestBody List<HfCmmDto> hfCmmDtos) {
    service.processHfCmms(hfCmmDtos);
  }

  @PostMapping("/facility/requisitions")
  @ResponseStatus(CREATED)
  public void createRequisition(@RequestBody @Valid RequisitionCreateRequest request) {
    service.createRequisition(request);
  }

  @PostMapping("/app-info")
  @ResponseStatus(NO_CONTENT)
  public void processAppInfo(HttpServletRequest httpServletRequest)
      throws InvocationTargetException, IllegalAccessException {
    AppInfo appInfo = new AppInfo();
    BeanUtils.copyProperties(appInfo, AndroidConstants.getAndroidHeader(httpServletRequest));
    service.processAppInfo(appInfo);
  }

}
