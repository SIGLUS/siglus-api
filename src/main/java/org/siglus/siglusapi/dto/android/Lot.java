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
import javax.annotation.Nullable;
import lombok.Data;

@Data
public class Lot {

  private final String code;

  private final LocalDate expirationDate;

  private Lot(String code, LocalDate expirationDate) {
    this.code = code;
    this.expirationDate = expirationDate;
  }

  public static Lot of(String code, @Nullable java.sql.Date expirationDate) {
    if (code == null) {
      return null;
    }
    return new Lot(code, expirationDate == null ? null : expirationDate.toLocalDate());
  }
}
