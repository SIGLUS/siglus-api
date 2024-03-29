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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "fromDatabase")
public class StockEvent {

  private final UUID id;
  private final UUID facilityId;
  private final UUID programId;
  private final Instant processedAt;
  private final String signature;
  private final UUID userId;

  public static StockEvent of(UUID facilityId, UUID programId, Instant processedAt, String signature, UUID userId) {
    return new StockEvent(UUID.randomUUID(), facilityId, programId, processedAt, signature, userId);
  }

  public Timestamp getServerProcessedAt() {
    return Timestamp.from(processedAt);
  }

}
