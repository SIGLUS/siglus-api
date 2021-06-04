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

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.response.android.FacilityResponse;
import org.siglus.siglusapi.dto.response.android.ProductSyncResponse;
import org.siglus.siglusapi.service.android.SiglusMeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/android/me")
@RequiredArgsConstructor
public class SiglusMeController {

  private final SiglusMeService service;

  @GetMapping("/facility/products")
  public ProductSyncResponse getFacilityProducts(
      @RequestParam(name = "lastSyncTime", required = false) Instant lastSyncTime) {
    return service.getFacilityProducts(lastSyncTime);
  }

  @GetMapping("/facility")
  public FacilityResponse getFacility() {
    return service.getFacility();
  }

  @PostMapping("/facility/archivedProducts")
  public void archiveAllProducts(@RequestBody List<String> productCodes) {
    service.archiveAllProducts(productCodes);
  }

}
