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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/fc")
public class TriggerController {

  public static final String DATE = "date";

  @Autowired
  private FcScheduleService fcScheduleService;

  @PostMapping("/cmms")
  public void syncCmmFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncCmmFromFc();
    } else {
      fcScheduleService.syncCmmFromFc(date);
    }
  }

  @PostMapping("/cps")
  public void syncCpFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncCpFromFc();
    } else {
      fcScheduleService.syncCpFromFc(date);
    }
  }

  @PostMapping("/receiptPlans")
  public void syncReceiptPlanFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncReceiptPlanFromFc();
    } else {
      fcScheduleService.syncReceiptPlanFromFc(date);
    }
  }

  @PostMapping("/programs")
  public void syncProgramFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncProgramFromFc();
    } else {
      fcScheduleService.syncProgramFromFc(date);
    }
  }

  @PostMapping("/products")
  public void syncProductFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncProductFromFc();
    } else {
      fcScheduleService.syncProductFromFc(date);
    }
  }

  @PostMapping("/facilities")
  public void syncFacilityFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncFacilityFromFc();
    } else {
      fcScheduleService.syncFacilityFromFc(date);
    }
  }

  @PostMapping("/facilityTypes")
  public void syncFacilityTypeFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncFacilityTypeFromFc();
    } else {
      fcScheduleService.syncFacilityTypeFromFc(date);
    }
  }

  @PostMapping("/issueVouchers")
  public void syncIssueVoucherFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncIssueVoucherFromFc();
    } else {
      fcScheduleService.syncIssueVoucherFromFc(date);
    }
  }

  @PostMapping("/regimens")
  public void syncRegimenFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncRegimenFromFc();
    } else {
      fcScheduleService.syncRegimenFromFc(date);
    }
  }

  @PostMapping("/geographicZones")
  public void syncGeographicZoneFromFc(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncGeographicZoneFromFc();
    } else {
      fcScheduleService.syncGeographicZoneFromFc(date);
    }
  }

}
