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

package org.siglus.siglusapi.domain;

import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_STATUS_ACTIVE;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.fc.ProgramDto;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "program_real_program", schema = "siglusintegration")
public class ProgramRealProgram extends BaseEntity {

  private String realProgramCode;

  private String realProgramName;

  private String programCode;

  private String programName;

  private Boolean active;

  public static ProgramRealProgram from(ProgramDto dto) {
    return ProgramRealProgram
        .builder()
        .realProgramCode(dto.getCode())
        .realProgramName(dto.getDescription())
        .active(isActive(dto))
        .build();
  }

  public static boolean isActive(ProgramDto dto) {
    return PROGRAM_STATUS_ACTIVE.equals(dto.getStatus());
  }
}
