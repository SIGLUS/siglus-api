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

package org.siglus.siglusapi.service.android;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.exception.RequisitionAlreadyCreatedBySupplierFacilityException;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEmitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MeCreateRequisitionService {

  private final RequisitionCreateService requisitionCreateService;
  private final AndroidRequisitionSyncedEmitter androidRequisitionSyncedEmitter;

  @Transactional
  public UUID createRequisition(RequisitionCreateRequest request) {
    UUID requisitionId;
    try {
      requisitionId = requisitionCreateService.createRequisition(request);
    } catch (Exception e) {
      if (e.getCause() instanceof RequisitionAlreadyCreatedBySupplierFacilityException) {
        throw (RequisitionAlreadyCreatedBySupplierFacilityException) e.getCause();
      }
      throw e;
    }
    if (requisitionId != null) {
      androidRequisitionSyncedEmitter.emit(request, requisitionId);
    }
    return requisitionId;
  }
}
