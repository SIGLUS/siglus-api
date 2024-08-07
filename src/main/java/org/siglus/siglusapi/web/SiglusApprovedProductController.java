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

import java.util.List;
import java.util.UUID;

import org.openlmis.requisition.dto.ApprovedProductDto;
import org.siglus.siglusapi.dto.ProgramProductDto;
import org.siglus.siglusapi.service.SiglusApprovedProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/approvedProducts")
public class SiglusApprovedProductController {

  @Autowired
  private SiglusApprovedProductService siglusApprovedProductService;

  @Deprecated
  @GetMapping
  public List<ApprovedProductDto> approvedProductDtos(@RequestParam UUID facilityId,
                                                      @RequestParam UUID programId) {
    return siglusApprovedProductService.getApprovedProducts(facilityId, programId);
  }

  @GetMapping("/brief")
  public List<ProgramProductDto> approvedProductResponse(@RequestParam UUID facilityId,
                                                         @RequestParam UUID programId,
                                                         @RequestParam(required = false) boolean excludeKit) {
    return siglusApprovedProductService.getApprovedProductsForFacility(facilityId, programId, excludeKit);
  }
}
