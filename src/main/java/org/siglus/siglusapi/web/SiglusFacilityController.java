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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.LocationStatusDto;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.exception.InvalidReasonException;
import org.siglus.siglusapi.repository.dto.LotStockDto;
import org.siglus.siglusapi.service.SiglusFacilityService;
import org.siglus.siglusapi.service.SiglusLotLocationService;
import org.siglus.siglusapi.service.SiglusLotService;
import org.siglus.siglusapi.web.request.RemoveLotsRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/siglusapi/facility")
public class SiglusFacilityController {

  private final SiglusFacilityService siglusFacilityService;

  private final SiglusLotService siglusLotService;

  private final SiglusLotLocationService lotLocationService;

  @GetMapping("/{id}/requisitionGroup")
  public List<RequisitionGroupMembersDto> searchFacilityRequisitionGroup(@PathVariable UUID id,
      @RequestParam Set<UUID> programIds) {
    return siglusFacilityService.searchFacilityRequisitionGroup(id, programIds);
  }

  @GetMapping("/{id}/lots")
  public List<LotStockDto> searchExpiredLots(@PathVariable("id") UUID id,
                                             @RequestParam Boolean expired) {
    if (!expired) {
      throw new InvalidReasonException("un-support param 'expired = false'");
    }
    return siglusLotService.getExpiredLots(id);
  }

  @PostMapping("/{id}/lots/remove")
  public void removeExpiredLots(@PathVariable("id") UUID id,
                                @Validated @RequestBody RemoveLotsRequest request) {
    if (Objects.equals(request.getLotType(), RemoveLotsRequest.EXPIRED)) {
      siglusFacilityService.removeExpiredLots(id,
          request.getLots(), request.getSignature(), request.getDocumentNumber());
      return;
    }
    throw new InvalidReasonException("un-support lotType: " + request.getLotType());
  }

  @GetMapping("/{id}/locations")
  public List<LocationStatusDto> searchLocationStatus(@PathVariable UUID id) {
    return lotLocationService.searchLocationStatus(id);
  }
}
