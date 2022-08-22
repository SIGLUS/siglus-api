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
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.LotLocationDto;
import org.siglus.siglusapi.service.SiglusLotLocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/siglusapi/locations")
public class SiglusLotLocationController {

  private final SiglusLotLocationService lotLocationService;

  @GetMapping
  public List<LotLocationDto> searchLotLocaionDto(
      @RequestParam(required = false) List<UUID> orderableIds, @RequestParam(required = false) boolean extraData) {
    return lotLocationService.searchLotLocationDtos(orderableIds, extraData);
  }

  @GetMapping("/facility")
  public List<FacilityLocationsDto> searchLocationsByFacility() {
    return lotLocationService.searchLocationsByFacility();
  }
}
