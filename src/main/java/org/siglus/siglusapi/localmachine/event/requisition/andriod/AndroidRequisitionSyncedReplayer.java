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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @EventListener(value = {AndroidRequisitionSyncedEvent.class})
  public void replay(AndroidRequisitionSyncedEvent event) {
    currentEvent.set(event);
    try {
      simulateUserAuthHelper.simulateNewUserAuth(event.getUserId());
      requisitionCreateService.createRequisition(event.getRequest());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    } finally {
      currentEvent.remove();
    }
  }

  public static Optional<AndroidRequisitionSyncedEvent> getCurrentEvent() {
    return Optional.ofNullable(currentEvent.get());
  }
}
