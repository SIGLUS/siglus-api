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

import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class PhysicalInventory {

  @Setter
  private UUID id;
  private final UUID facilityId;
  private final UUID programId;
  private final UUID eventId;
  private final LocalDate occurredDate;
  private final String signature;

  public Key getKey() {
    return new Key();
  }

  @EqualsAndHashCode
  public class Key {

    private final UUID facilityId = PhysicalInventory.this.facilityId;
    private final UUID programId = PhysicalInventory.this.programId;
    private final UUID eventId = PhysicalInventory.this.eventId;
    private final LocalDate occurredDate = PhysicalInventory.this.occurredDate;

  }

}
