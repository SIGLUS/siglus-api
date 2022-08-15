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
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi")
public class SiglusValidSourceDestinationController {

  @Autowired
  private SiglusValidSourceDestinationService validSourceDestinationService;

  @GetMapping("/validDestinations")
  public Collection<ValidSourceDestinationDto> searchValidDestinations(@RequestParam UUID programId,
      @RequestParam UUID facilityId) {
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      return validSourceDestinationService.findDestinationsForAllPrograms(facilityId);
    }
    return validSourceDestinationService.findDestinationsForOneProgram(programId, facilityId);
  }

  @GetMapping("/validSources")
  public Collection<ValidSourceDestinationDto> searchValidSources(@RequestParam UUID programId,
      @RequestParam UUID facilityId) {
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      return validSourceDestinationService.findSourcesForAllPrograms(facilityId);
    }
    return validSourceDestinationService.findSourcesForOneProgram(programId, facilityId);
  }

}
