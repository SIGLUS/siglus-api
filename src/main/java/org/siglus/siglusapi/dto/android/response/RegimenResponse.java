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

package org.siglus.siglusapi.dto.android.response;

import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.openlmis.requisition.dto.ProgramDto;
import org.siglus.siglusapi.domain.Regimen;

@Data
public class RegimenResponse {

  private String code;
  private String name;
  private String programCode;
  private String programName;
  private Boolean active;
  private Integer displayOrder;
  private Boolean isCustom;
  private RegimenCategoryResponse category;

  public static RegimenResponse of(Regimen regimen, Map<UUID, ProgramDto> programMap) {
    RegimenResponse resp = new RegimenResponse();
    resp.setCode(regimen.getCode());
    resp.setName(regimen.getName());
    resp.setActive(regimen.getActive());
    resp.setDisplayOrder(regimen.getDisplayOrder());
    resp.setIsCustom(regimen.getIsCustom());
    if (programMap.containsKey(regimen.getProgramId())) {
      ProgramDto program = programMap.get(regimen.getProgramId());
      resp.setProgramCode(program.getCode());
      resp.setProgramName(program.getName());
    }
    resp.setCategory(RegimenCategoryResponse.of(regimen.getRegimenCategory()));
    return resp;
  }

}
