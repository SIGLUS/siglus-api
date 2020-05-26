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

import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.util.Collection;
import java.util.UUID;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/validReasons")
public class SiglusValidReasonAssignmentController {

  @Autowired
  private SiglusValidReasonAssignmentService siglusValidReasonAssignmentService;

  // [SIGLUS change start]
  // [change reason]: support "All Products" program.
  @GetMapping
  public Collection<ValidReasonAssignmentDto> searchValidReasons(
      @RequestParam(required = false) UUID program,
      @RequestParam(required = false) UUID facilityType,
      @RequestParam(required = false) String reasonType,
      @RequestParam(required = false) UUID reason) {
    if (ALL_PRODUCTS_PROGRAM_ID.equals(program)) {
      return siglusValidReasonAssignmentService
          .getValidReasonsForAllProducts(facilityType, reasonType, reason);
    }
    return siglusValidReasonAssignmentService
        .getValidReasons(program, facilityType, reasonType, reason);
  }
  // [SIGLUS change end]
}
