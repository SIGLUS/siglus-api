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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.money.Money;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.serializer.MoneyDeserializer;
import org.openlmis.referencedata.serializer.MoneySerializer;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProgramOrderableDto implements ProgramOrderable.Importer, ProgramOrderable.Exporter {

  private UUID programId;

  private UUID orderableDisplayCategoryId;

  private String orderableCategoryDisplayName;

  private Integer orderableCategoryDisplayOrder;

  private boolean active;

  private boolean fullSupply;

  private int displayOrder;

  private Integer dosesPerPatient;

  @JsonSerialize(using = MoneySerializer.class)
  @JsonDeserialize(using = MoneyDeserializer.class)
  private Money pricePerPack;

  /**
   * Create new list of ProgramOrderableDto based on given list of {@link ProgramOrderable}.
   *
   * @param programOrderables list of {@link ProgramOrderable}
   * @return new list of ProgramOrderableDto.
   */
  public static Set<ProgramOrderableDto> newInstance(
      Iterable<ProgramOrderable> programOrderables) {

    Set<ProgramOrderableDto> programOrderableDtos = new HashSet<>();
    programOrderables.forEach(po -> programOrderableDtos.add(newInstance(po)));
    return programOrderableDtos;
  }

  /**
   * Creates new instance based on given {@link ProgramOrderable}.
   *
   * @param po instance of ProgramOrderable
   * @return new instance of ProgramOrderableDto.
   */
  public static ProgramOrderableDto newInstance(ProgramOrderable po) {
    if (po == null) {
      return null;
    }
    ProgramOrderableDto programDto = new ProgramOrderableDto();
    po.export(programDto);

    return programDto;
  }
}
