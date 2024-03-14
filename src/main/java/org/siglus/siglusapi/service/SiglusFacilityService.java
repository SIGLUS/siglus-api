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

package org.siglus.siglusapi.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.RemovedLotDto;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusFacilityService {

  private final RequisitionGroupMembersRepository requisitionGroupMembersRepository;
  private final SiglusFacilityRepository siglusFacilityRepository;
  private final FacilityConfigHelper facilityConfigHelper;
  private final SiglusLotService siglusLotService;

  public List<RequisitionGroupMembersDto> searchFacilityRequisitionGroup(UUID id, Set<UUID> programIds) {
    return requisitionGroupMembersRepository.findParentFacilityByRequisitionGroup(id, programIds);
  }

  public Map<UUID, String> getFacilityIdToName(Set<UUID> facilityIds) {
    return siglusFacilityRepository.findFacilityBasicInfoByIds(facilityIds).stream()
        .collect(Collectors.toMap(Facility::getId, Facility::getName));
  }

  public void removeExpiredLots(UUID facilityId, List<RemovedLotDto> lots) {
    boolean hasLocation = facilityConfigHelper.isLocationManagementEnabled(facilityId);
    if (hasLocation && lots.stream().anyMatch(RemovedLotDto::hasLocation)) {
      throw new BusinessDataException(Message.createFromMessageKeyStr("Missing Location"));
    }
    lots.forEach(lot -> lot.setFacilityId(facilityId));
    siglusLotService.removeExpiredLots(lots, hasLocation);
  }
}
