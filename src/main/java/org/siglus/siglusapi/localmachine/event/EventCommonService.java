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

package org.siglus.siglusapi.localmachine.event;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventCommonService {
  private final RequisitionGroupMembersRepository requisitionGroupMembersRepository;

  public UUID getReceiverId(UUID facilityId, UUID programId) {
    List<RequisitionGroupMembersDto> parentFacility =
        requisitionGroupMembersRepository.findParentFacilityByRequisitionGroup(
            facilityId, Collections.singleton(programId));
    if (CollectionUtils.isEmpty(parentFacility)) {
      throw new IllegalStateException(String.format("can't find event's receiver id, facilityId = %s, programId = %s",
          facilityId, programId));
    }
    return parentFacility.get(0).getFacilityId();
  }
}
