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

import static java.util.Objects.isNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.requisition.domain.AvailableRequisitionColumnOption;

@Getter
@Setter
@EqualsAndHashCode
public final class AvailableRequisitionColumnOptionDto implements
    AvailableRequisitionColumnOption.Importer, AvailableRequisitionColumnOption.Exporter {

  private UUID id;

  private String optionName;

  private String optionLabel;

  /**
   * Create new set of AvailableRequisitionColumnOptionDto
   * based on given set of {@link AvailableRequisitionColumnOption}.
   *
   * @param options set of {@link AvailableRequisitionColumnOption}
   * @return new set of AvailableRequisitionColumnOptionDto.
   */
  public static Set<AvailableRequisitionColumnOptionDto> newInstance(
      Iterable<AvailableRequisitionColumnOption> options) {
    if (isNull(options)) {
      return null;
    }
    Set<AvailableRequisitionColumnOptionDto> optionDtos = new HashSet<>();
    options.forEach(t -> optionDtos.add(newInstance(t)));
    return optionDtos;
  }


  /**
   * Create new instance of AvailableRequisitionColumnOptionDto based
   * on given {@link AvailableRequisitionColumnOption}.
   *
   * @param option instance of AvailableRequisitionColumnOption
   * @return new instance of AvailableRequisitionColumnOptionDto.
   */
  public static AvailableRequisitionColumnOptionDto newInstance(
      AvailableRequisitionColumnOption option) {
    if (isNull(option)) {
      return null;
    }
    AvailableRequisitionColumnOptionDto optionDto = new AvailableRequisitionColumnOptionDto();
    option.export(optionDto);
    return optionDto;
  }
}
