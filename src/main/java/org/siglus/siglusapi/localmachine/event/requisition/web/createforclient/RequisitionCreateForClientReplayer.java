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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.event.requisition.web.approve.RequisitionInternalApproveReplayer;
import org.siglus.siglusapi.localmachine.event.requisition.web.finalapprove.RequisitionFinalApproveReplayer;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionCreateForClientReplayer {

  private final RequisitionInternalApproveReplayer internalApproveReplayer;
  private final RequisitionFinalApproveReplayer finalApproveReplayer;
  private final RequisitionExtensionRepository requisitionExtensionRepository;

  @EventListener(classes = {RequisitionCreateForClientEvent.class})
  public void replay(RequisitionCreateForClientEvent event) {
    try {
      log.info("start replay requisition create for client, requisition number = "
          + event.getRequisitionFinalApproveEvent().getRequisitionNumber());
      doReplay(event);
      log.info("end replay requisition create for client, requisition number = "
          + event.getRequisitionFinalApproveEvent().getRequisitionNumber());
    } catch (Exception e) {
      log.error("fail to replay requisition create for client, msg = " + e.getMessage(), e);
      throw e;
    }
  }

  public void doReplay(RequisitionCreateForClientEvent event) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionNumber(
        event.getRequisitionInternalApprovedEvent().getRequisitionExtension().getRealRequisitionNumber());
    // if client has created the same period requisition, then delete it,
    // make supplier created requisition for client as the correct
    internalApproveReplayer.deleteIfExistRequisition(requisitionExtension);
    internalApproveReplayer.doReplayForRequisitionInternalApprovedEvent(event.getRequisitionInternalApprovedEvent());
  }
}
