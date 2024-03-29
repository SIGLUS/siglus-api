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

package org.siglus.siglusapi.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.springframework.beans.BeanUtils;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramOrderablesExtensionDto {
  private UUID orderableId;

  private String programCode;

  private String programName;

  private String realProgramCode;

  private String realProgramName;

  private Boolean showInReport;

  private String unit;

  public static ProgramOrderablesExtensionDto from(ProgramOrderablesExtension extension) {
    ProgramOrderablesExtensionDto dto = new ProgramOrderablesExtensionDto();
    BeanUtils.copyProperties(extension, dto);
    return dto;
  }
}
