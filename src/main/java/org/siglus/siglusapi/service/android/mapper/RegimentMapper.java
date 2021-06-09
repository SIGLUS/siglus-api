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

package org.siglus.siglusapi.service.android.mapper;

import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openlmis.requisition.dto.ProgramDto;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.dto.android.response.RegimenResponse;

@Mapper(componentModel = "spring", uses = RegimentCategoryMapper.class)
public interface RegimentMapper {

  @Mapping(target = "programCode", source = "programId", qualifiedByName = "toProgramCode")
  @Mapping(target = "programName", source = "programId", qualifiedByName = "toProgramName")
  @Mapping(target = "category", source = "regimenCategory")
  RegimenResponse toResponse(Regimen domain, @Context Map<UUID, ProgramDto> allPrograms);


  @Named("toProgramCode")
  default String toProgramCode(UUID programId, @Context Map<UUID, ProgramDto> allPrograms) {
    ProgramDto program = allPrograms.get(programId);
    if (program == null) {
      return null;
    }
    return program.getCode();
  }

  @Named("toProgramName")
  default String toProgramName(UUID programId, @Context Map<UUID, ProgramDto> allPrograms) {
    ProgramDto program = allPrograms.get(programId);
    if (program == null) {
      return null;
    }
    return program.getName();
  }

}
