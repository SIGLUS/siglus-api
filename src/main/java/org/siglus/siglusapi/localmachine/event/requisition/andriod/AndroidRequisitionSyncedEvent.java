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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.localmachine.EventPayload;

@Getter
@Setter
@Builder
@EventPayload
@NoArgsConstructor
public class AndroidRequisitionSyncedEvent {
  private UUID facilityId;
  private UUID userId;
  private UUID requisitionId;
  private RequisitionCreateRequest request;
  private String requisitionNumberPrefix;
  private Integer requisitionNumber;

  public AndroidRequisitionSyncedEvent(UUID facilityId, UUID userId, UUID requisitionId,
      RequisitionCreateRequest request, String requisitionNumberPrefix, Integer requisitionNumber) {
    this.facilityId = facilityId;
    this.userId = userId;
    this.requisitionId = requisitionId;
    this.request = request;
    this.requisitionNumberPrefix = requisitionNumberPrefix;
    this.requisitionNumber = requisitionNumber;
  }
}
