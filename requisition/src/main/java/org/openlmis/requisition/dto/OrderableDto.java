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

package org.openlmis.requisition.dto;

import static org.apache.commons.collections.MapUtils.getString;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.openlmis.requisition.i18n.MessageKeys.CAN_NOT_FIND_PROGRAM_DETAILS_FROM_ORDERABLE;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.exception.ValidationMessageException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class OrderableDto extends BasicOrderableDto {

  public static final String COMMODITY_TYPE_IDENTIFIER = "commodityType";

  private Set<ProgramOrderableDto> programs;
  private DispensableDto dispensable;
  private Map<String, String> identifiers;

  /**
   * Gets ProgramOrderableDto in programs using programId. If it can't be found an exception will be
   * thrown.
   *
   * @param programId program's UUID
   * @throws ValidationMessageException if program orderable can't be found.
   */
  public ProgramOrderableDto getProgramOrderable(UUID programId) {
    return findProgramOrderable(programId)
        .orElseThrow(() ->
            new ValidationMessageException(CAN_NOT_FIND_PROGRAM_DETAILS_FROM_ORDERABLE));
  }

  /**
   * Tries to find ProgramOrderableDto in programs using programId.
   *
   * @param programId program's UUID
   */
  public Optional<ProgramOrderableDto> findProgramOrderable(UUID programId) {
    return Optional
        .ofNullable(programs)
        .orElse(Collections.emptySet())
        .stream()
        .filter(po -> Objects.equals(programId, po.getProgramId()))
        .findFirst();
  }

  /**
   * Returns a value of commodity type identifier. The {@code null} value will be returned if value
   * does not exist or if it is blank (whitespace, empty ("") or null).
   */
  public String getCommodityTypeIdentifier() {
    return defaultIfBlank(getString(identifiers, COMMODITY_TYPE_IDENTIFIER), null);
  }

}
