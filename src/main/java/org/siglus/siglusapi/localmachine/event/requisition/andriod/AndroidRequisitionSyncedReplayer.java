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

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.siglus.siglusapi.exception.InvalidProgramCodeException;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.android.RequisitionCreateService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AndroidRequisitionSyncedReplayer {

  @VisibleForTesting
  public static final ThreadLocal<AndroidRequisitionSyncedEvent> currentEvent = ThreadLocal.withInitial(() -> null);
  private final SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  private final RequisitionCreateService requisitionCreateService;
  private final SiglusRequisitionRepository requisitionRepository;
  private final SiglusProgramService siglusProgramService;

  @EventListener(value = {AndroidRequisitionSyncedEvent.class})
  public void replay(AndroidRequisitionSyncedEvent event) {
    currentEvent.set(event);
    try {
      simulateUserAuthHelper.simulateNewUserAuth(event.getUserId());
      Requisition existRequisition = findExistRequisition(event);
      if (existRequisition == null) {
        requisitionCreateService.createRequisition(event.getRequest());
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    } finally {
      currentEvent.remove();
    }
  }

  private Requisition findExistRequisition(AndroidRequisitionSyncedEvent event) {
    String programCode = event.getRequest().getProgramCode();
    UUID programId = siglusProgramService.getProgramByCode(programCode)
        .map(org.openlmis.requisition.dto.BaseDto::getId)
        .orElseThrow(() -> InvalidProgramCodeException.requisition(programCode));
    UUID processingPeriodId = requisitionCreateService.getPeriodId(event.getRequest());
    return requisitionRepository.findOneByFacilityIdAndProgramIdAndProcessingPeriodId(
        event.getFacilityId(),
        programId,
        processingPeriodId);
  }

  public static Optional<AndroidRequisitionSyncedEvent> getCurrentEvent() {
    return Optional.ofNullable(currentEvent.get());
  }
}
