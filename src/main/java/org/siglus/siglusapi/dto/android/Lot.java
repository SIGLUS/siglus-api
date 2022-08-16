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

package org.siglus.siglusapi.dto.android;

import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.siglus.common.util.Uuid5Generator;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Lot {

  private final String code;

  private final LocalDate expirationDate;

  @Nonnull
  public static Lot of(String code, @Nullable LocalDate expirationDate) {
    return new Lot(code, expirationDate);
  }

  @Nullable
  public static Lot fromDatabase(String code, @Nullable java.sql.Date expirationDate) {
    if (code == null) {
      return null;
    }
    return new Lot(code, expirationDate == null ? null : expirationDate.toLocalDate());
  }

  public String getLotUniqueKey() {
    return this.getCode() + this.getExpirationDate();
  }

  public UUID getUUid() {
    return Uuid5Generator.fromUtf8(getLotUniqueKey());
  }

}
