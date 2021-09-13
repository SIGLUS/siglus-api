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

package org.siglus.siglusapi.web;

import java.time.LocalDate;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.dto.FcProofOfDeliveryDto;
import org.siglus.siglusapi.dto.FcRequisitionDto;
import org.siglus.siglusapi.dto.fc.FacilityStockMovementResponse;
import org.siglus.siglusapi.service.SiglusFcIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/siglusapi/integration")
public class SiglusFcIntegrationController {

  @Autowired
  private SiglusFcIntegrationService siglusFcIntegrationService;

  @GetMapping("/requisitions")
  public Page<FcRequisitionDto> searchRequisitions(
      @DateTimeFormat(pattern = "yyyyMMdd") @RequestParam LocalDate date,
      Pageable pageable) {
    if (Pagination.NO_PAGINATION == pageable.getPageSize()) {
      pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, 20);
    }
    return siglusFcIntegrationService.searchRequisitions(date, pageable);
  }

  @GetMapping("/stockMovements")
  public Page<FacilityStockMovementResponse> searchStockMovements(
      @DateTimeFormat(pattern = "yyyyMMdd") @RequestParam LocalDate date, Pageable pageable) {
    if (Pagination.NO_PAGINATION == pageable.getPageSize()) {
      pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, 20);
    }
    return siglusFcIntegrationService.searchStockMovements(date, pageable);
  }

  @GetMapping("/pods")
  public Page<FcProofOfDeliveryDto> searchProofOfDelivery(
      @DateTimeFormat(pattern = "yyyyMMdd") @RequestParam LocalDate date,
      Pageable pageable) {
    if (Pagination.NO_PAGINATION == pageable.getPageSize()) {
      pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, 20);
    }
    return siglusFcIntegrationService.searchProofOfDelivery(date, pageable);
  }

}
