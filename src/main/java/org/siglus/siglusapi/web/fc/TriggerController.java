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

import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.siglus.siglusapi.service.fc.FcScheduleService;
import org.siglus.siglusapi.validator.FcQueryDate;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!localmachine")
@RestController
@RequestMapping("/api/siglusapi/fc")
@RequiredArgsConstructor
@Validated
public class TriggerController {

  private static final String DATE = "date";
  private final FcScheduleService fcScheduleService;

  @PostMapping("/cmms")
  public void syncCmms(@NotEmpty @FcQueryDate @RequestParam(DATE) String date) {
    fcScheduleService.syncCmms(date);
  }

  @PostMapping("/cps")
  public void syncCps(@NotEmpty @FcQueryDate @RequestParam(DATE) String date) {
    fcScheduleService.syncCps(date);
  }

  @PostMapping("/receiptPlans")
  public void syncReceiptPlans(@FcQueryDate @RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncReceiptPlans(date);
  }

  @PostMapping("/issueVouchers")
  public void syncIssueVouchers(@FcQueryDate @RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncIssueVouchers(date);
  }

  @PostMapping("/programs")
  public void syncPrograms(@FcQueryDate @RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncPrograms(date);
  }

  @PostMapping("/products")
  public void syncProducts(@FcQueryDate @RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncProducts(date);
  }

  @PostMapping("/facilities")
  public void syncFacilities(@RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncFacilities(date);
  }

  @PostMapping("/facilityTypes")
  public void syncFacilityTypes(@FcQueryDate @RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncFacilityTypes(date);
  }

  @PostMapping("/regimens")
  public void syncRegimens(@FcQueryDate @RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncRegimens(date);
  }

  @PostMapping("/geographicZones")
  public void syncGeographicZones(@FcQueryDate @RequestParam(value = DATE, required = false) String date) {
    fcScheduleService.syncGeographicZones(date);
  }

}
