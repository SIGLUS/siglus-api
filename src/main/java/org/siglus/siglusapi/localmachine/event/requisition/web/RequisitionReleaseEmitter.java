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

package org.siglus.siglusapi.localmachine.event.requisition.web;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequisitionReleaseEmitter {
  private final EventPublisher eventPublisher;
  private final RequisitionRepository requisitionRepository;
  private final EventCommonService eventCommonService;

  public RequisitionReleaseEvent emit(ReleasableRequisitionDto releasableRequisitionDto, UUID authorId) {
    RequisitionReleaseEvent requisitionReleaseEvent = getEvent(releasableRequisitionDto, authorId);
    eventPublisher.emitGroupEvent(requisitionReleaseEvent.getRequisitionNumber(),
        getReceiverId(releasableRequisitionDto.getRequisitionId()), requisitionReleaseEvent);
    return requisitionReleaseEvent;
  }

  private RequisitionReleaseEvent getEvent(ReleasableRequisitionDto releasableRequisitionDto, UUID authorId) {
    return RequisitionReleaseEvent
        .builder()
        .requisitionNumber(eventCommonService.getGroupId(releasableRequisitionDto.getRequisitionId()))
        .supplyingDepotId(releasableRequisitionDto.getSupplyingDepotId())
        .authorId(authorId)
        .build();
  }

  private UUID getReceiverId(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    return requisition.getFacilityId();
  }
}
