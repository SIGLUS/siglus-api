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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.dto.FcProofOfDeliveryDto;
import org.siglus.siglusapi.dto.FcRequisitionDto;
import org.siglus.siglusapi.service.SiglusFcIntegrationService;
import org.siglus.siglusapi.service.fc.FcScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/integration")
public class SiglusFcIntegrationController {

  @Autowired
  private SiglusFcIntegrationService siglusFcIntegrationService;

  @Autowired
  private FcScheduleService scheduleService;

  @GetMapping("/requisitions")
  public Page<FcRequisitionDto> searchRequisitions(@RequestParam String date, Pageable pageable)
      throws ParseException {
    DateFormat format = new SimpleDateFormat("yyyyMMdd");
    format.setLenient(false);
    format.parse(date);
    if (Pagination.NO_PAGINATION == pageable.getPageSize()) {
      pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, 20);
    }
    return siglusFcIntegrationService.searchRequisitions(date, pageable);
  }

  @GetMapping("/pods")
  public Page<FcProofOfDeliveryDto> searchProofOfDelivery(@RequestParam String date,
      Pageable pageable) {
    if (Pagination.NO_PAGINATION == pageable.getPageSize()) {
      pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, 20);
    }
    return siglusFcIntegrationService.searchProofOfDelivery(date, pageable);
  }

  @PostMapping("/issueVouchers")
  public void updateIssueVouchers(@RequestParam("date") String beginDate) {
    scheduleService.fetchIssueVouchersFromFc();
  }
}
