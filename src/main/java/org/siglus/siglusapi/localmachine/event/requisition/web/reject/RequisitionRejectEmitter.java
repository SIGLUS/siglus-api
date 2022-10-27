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

package org.siglus.siglusapi.localmachine.event.requisition.web.reject;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionRejectEmitter {

  private final SiglusAuthenticationHelper authHelper;
  private final EventPublisher eventPublisher;
  private final EventCommonService baseEventCommonService;
  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  public void emit(UUID requisitionId, UUID facilityId) {
    RequisitionRejectEvent event = new RequisitionRejectEvent();
    String requisitionNumber = siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId);
    event.setRequisitionNumber(requisitionNumber);
    event.setUserId(authHelper.getCurrentUser().getId());
    eventPublisher.emitGroupEvent(baseEventCommonService.getGroupId(requisitionId), facilityId, event);
  }
}
