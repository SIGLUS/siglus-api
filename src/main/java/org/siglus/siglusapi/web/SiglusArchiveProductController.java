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

import java.util.Set;
import java.util.UUID;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/archivedproducts")
public class SiglusArchiveProductController {

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @PostMapping("/{orderableId}/archive")
  public void archiveProduct(@PathVariable UUID orderableId) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @PostMapping("/{orderableId}/activate")
  public void activateProduct(@PathVariable UUID orderableId) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    archiveProductService.activateProduct(facilityId, orderableId);
  }

  @GetMapping
  public Set<String> searchArchivedProducts(@RequestParam UUID facilityId) {
    return archiveProductService.searchArchivedProducts(facilityId);
  }

}
