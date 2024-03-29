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

package org.siglus.siglusapi.dto.android.db;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class PodLineItem {

  @Setter
  private UUID id;
  private final UUID proofOfDeliveryId;
  private final String notes;
  private final Integer quantityAccepted;
  private final Integer quantityRejected;
  private final UUID orderableId;
  private final UUID lotId;
  private final String vvmStatus;
  private final Boolean userVvm;
  private final UUID rejectionReasonId;
  private final Long orderableVersionNumber;

}
