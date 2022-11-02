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

package org.siglus.siglusapi.localmachine.event.requisition.andriod;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.BaseDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.exception.InvalidProgramCodeException;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AndroidRequisitionSyncedEmitter {

  private final EventPublisher eventPublisher;
  private final SiglusAuthenticationHelper authHelper;
  private final SiglusProgramService siglusProgramService;
  private final EventCommonService baseEventCommonService;

  public AndroidRequisitionSyncedEvent emit(RequisitionCreateRequest request, UUID requisitionId) {
    UserDto user = authHelper.getCurrentUser();
    AndroidRequisitionSyncedEvent event = AndroidRequisitionSyncedEvent.builder()
        .facilityId(user.getHomeFacilityId())
        .userId(user.getId())
        .requisitionId(requisitionId)
        .request(request)
        .build();

    UUID programId = siglusProgramService.getProgramByCode(request.getProgramCode()).map(BaseDto::getId)
        .orElseThrow(() -> InvalidProgramCodeException.requisition(request.getProgramCode()));

    eventPublisher.emitGroupEvent(
        baseEventCommonService.getGroupId(requisitionId),
        baseEventCommonService.getReceiverId(user.getHomeFacilityId(), programId), event);
    return event;
  }
}
