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

package org.siglus.siglusapi.web.fc;

import org.siglus.siglusapi.service.fc.FcScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi")
public class FcIntegrationTriggerController {

  public static final String DATE = "date";

  @Autowired
  private FcScheduleService fcScheduleService;

  @PostMapping("/cmms")
  public void fetchCmmsFromFc(@RequestParam(DATE) String date) {
    fcScheduleService.fetchCmmsFromFc(date);
  }

  @PostMapping("/cps")
  public void fetchCpsFromFc(@RequestParam(DATE) String date) {
    fcScheduleService.fetchCpsFromFc(date);
  }

  @PostMapping("/receiptPlans")
  public void processingReceiptPlans(@RequestParam(DATE) String date) {
    fcScheduleService.fetchReceiptPlansFromFc(date);
  }

  @PostMapping("/fcPrograms")
  public void fetchProgramsFromFc(@RequestParam(DATE) String date) {
    fcScheduleService.fetchProgramsFromFc(date);
  }

  @PostMapping("/fcProducts")
  public void fetchProductsFromFc(@RequestParam String date) {
    fcScheduleService.fetchProductsFromFc(date);
  }

  @PostMapping("/fcFacility")
  public void fetchFacilityFromFc(@RequestParam(DATE) String date) {
    fcScheduleService.fetchFacilityFromFc(date);
  }

  @PostMapping("/fcFacilityType")
  public void fetchFacilityTypeFromFc(@RequestParam(DATE) String date) {
    fcScheduleService.fetchFacilityTypeFromFc(date);
  }

  @PostMapping("/issueVouchers")
  public void updateIssueVouchers(@RequestParam(DATE) String date) {
    fcScheduleService.fetchIssueVouchersFromFc(date);
  }

  @PostMapping("/regimens")
  public void fetchRegimensFromFc(@RequestParam(DATE) String date) {
    fcScheduleService.fetchRegimenFromFc(date);
  }

  @PostMapping("/fcGeographicZones")
  public void fetchGeographicZonesFromFc(@RequestParam(DATE) String date) {
    fcScheduleService.fetchGeographicZonesFromFc(date);
  }

}
