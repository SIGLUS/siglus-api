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

package org.openlmis.referencedata.dto;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Lot;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LotDto extends BaseDto implements Lot.Exporter, Lot.Importer {

  private String lotCode;
  private boolean active;
  private UUID tradeItemId;
  private LocalDate expirationDate;
  private LocalDate manufactureDate;

  /**
   * Create new set of LotDto based on given iterable of {@link Lot}.
   *
   * @param lots list of {@link Lot}
   * @return new list of LotDto.
   */
  public static List<LotDto> newInstance(Iterable<Lot> lots) {
    List<LotDto> lotDtos = new LinkedList<>();
    lots.forEach(lot -> lotDtos.add(newInstance(lot)));
    return lotDtos;
  }

  /**
   * Creates new instance based on given {@link Lot}.
   *
   * @param lot instance of Lot.
   * @return new instance of LotDto.
   */
  public static LotDto newInstance(Lot lot) {
    if (lot == null) {
      return null;
    }
    LotDto lotDto = new LotDto();
    lot.export(lotDto);

    return lotDto;
  }
}
