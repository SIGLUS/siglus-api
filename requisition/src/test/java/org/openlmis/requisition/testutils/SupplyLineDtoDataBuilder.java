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
import org.apache.commons.lang.RandomStringUtils;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class SupplyLineDtoDataBuilder implements DtoDataBuilder<SupplyLineDto> {

  private UUID id = UUID.randomUUID();
  private SupervisoryNodeDto supervisoryNode = new SupervisoryNodeDto();
  private String description = RandomStringUtils.randomAlphanumeric(5);
  private ProgramDto program = new ProgramDtoDataBuilder().buildAsDto();
  private FacilityDto supplyingFacility = new FacilityDtoDataBuilder().buildAsDto();

  public SupplyLineDtoDataBuilder withSupplyingFacility(FacilityDto supplyingFacility) {
    this.supplyingFacility = supplyingFacility;
    return this;
  }

  public SupplyLineDtoDataBuilder withSupervisoryNode(SupervisoryNodeDto supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
    return this;
  }

  public SupplyLineDtoDataBuilder withProgram(ProgramDto program) {
    this.program = program;
    return this;
  }

  /**
   * Creates new instance of {@link SupplyLineDto}.
   */
  public SupplyLineDto buildAsDto() {
    SupplyLineDto dto = new SupplyLineDto(supervisoryNode, description, program, supplyingFacility);
    dto.setId(id);
    return dto;
  }
}
