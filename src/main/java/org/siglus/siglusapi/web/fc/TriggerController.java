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
  public void syncCmms(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncCmms();
    } else {
      fcScheduleService.syncCmms(date);
    }
  }

  @PostMapping("/cps")
  public void syncCps(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncCps();
    } else {
      fcScheduleService.syncCps(date);
    }
  }

  @PostMapping("/receiptPlans")
  public void syncReceiptPlans(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncReceiptPlans();
    } else {
      fcScheduleService.syncReceiptPlans(date);
    }
  }

  @PostMapping("/programs")
  public void syncPrograms(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncPrograms();
    } else {
      fcScheduleService.syncPrograms(date);
    }
  }

  @PostMapping("/products")
  public void syncProducts(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncProducts();
    } else {
      fcScheduleService.syncProducts(date);
    }
  }

  @PostMapping("/facilities")
  public void syncFacilities(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncFacilities();
    } else {
      fcScheduleService.syncFacilities(date);
    }
  }

  @PostMapping("/facilityTypes")
  public void syncFacilityTypes(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncFacilityTypes();
    } else {
      fcScheduleService.syncFacilityTypes(date);
    }
  }

  @PostMapping("/issueVouchers")
  public void syncIssueVouchers(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncIssueVouchers();
    } else {
      fcScheduleService.syncIssueVouchers(date);
    }
  }

  @PostMapping("/regimens")
  public void syncRegimens(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncRegimens();
    } else {
      fcScheduleService.syncRegimens(date);
    }
  }

  @PostMapping("/geographicZones")
  public void syncGeographicZones(@RequestParam(value = DATE, required = false) String date) {
    if (StringUtils.isEmpty(date)) {
      fcScheduleService.syncGeographicZones();
    } else {
      fcScheduleService.syncGeographicZones(date);
    }
  }

}
