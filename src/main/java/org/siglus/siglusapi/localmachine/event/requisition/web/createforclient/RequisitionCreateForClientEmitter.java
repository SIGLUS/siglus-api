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

package org.siglus.siglusapi.localmachine.event.requisition.web.createforclient;

import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.REQUISITION_CREATE_FOR_CLIENT;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.requisition.web.approve.RequisitionInternalApproveEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.approve.RequisitionInternalApprovedEvent;
import org.siglus.siglusapi.localmachine.event.requisition.web.finalapprove.RequisitionFinalApproveEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.finalapprove.RequisitionFinalApproveEvent;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionCreateForClientEmitter {

  private final RequisitionInternalApproveEmitter requisitionInternalApproveEmitter;
  private final RequisitionFinalApproveEmitter requisitionFinalApproveEmitter;
  private final EventPublisher eventPublisher;

  public RequisitionCreateForClientEvent emit(UUID requisitionId) {
    RequisitionCreateForClientEvent event = getEvent(requisitionId);
    eventPublisher.emitGroupEvent(
        event.getRequisitionFinalApproveEvent().getRequisitionNumber(),
        event.getRequisitionInternalApprovedEvent().getRequisition().getFacilityId(),
        event,
        REQUISITION_CREATE_FOR_CLIENT);
    return event;
  }

  private RequisitionCreateForClientEvent getEvent(UUID requisitionId) {
    RequisitionInternalApprovedEvent internalApprovedEvent = requisitionInternalApproveEmitter.getEvent(requisitionId);
    RequisitionFinalApproveEvent finalApproveEvent = requisitionFinalApproveEmitter.getEvent(requisitionId);
    return RequisitionCreateForClientEvent.builder()
        .requisitionInternalApprovedEvent(internalApprovedEvent)
        .requisitionFinalApproveEvent(finalApproveEvent)
        .build();
  }
}
