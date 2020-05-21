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

package org.openlmis.requisition.testutils;

import java.util.UUID;
import org.openlmis.requisition.dto.FacilityOperatorDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class FacilityOperatorDtoDataBuilder implements DtoDataBuilder<FacilityOperatorDto> {

  private UUID id;
  private String code;
  private String name;

  /**
   * Creates builder for creating new instance of {@link FacilityOperatorDto}.
   */
  public FacilityOperatorDtoDataBuilder() {
    this.id = UUID.randomUUID();
    this.code = "operatorCode";
    this.name = "operatorName";
  }

  @Override
  public FacilityOperatorDto buildAsDto() {
    FacilityOperatorDto operator = new FacilityOperatorDto();
    operator.setId(id);
    operator.setCode(code);
    operator.setName(name);
    return operator;
  }
}
