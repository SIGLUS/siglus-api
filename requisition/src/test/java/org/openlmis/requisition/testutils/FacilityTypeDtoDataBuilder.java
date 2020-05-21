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
import org.openlmis.requisition.dto.FacilityTypeDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class FacilityTypeDtoDataBuilder implements DtoDataBuilder<FacilityTypeDto> {

  private UUID id;
  private String code;
  private String name;
  private String description;
  private Integer displayOrder;
  private Boolean active;

  /**
   * Builder for {@link FacilityTypeDto}.
   */
  public FacilityTypeDtoDataBuilder() {
    id = UUID.randomUUID();
    code = "code";
    name = "name";
    description = "description";
    displayOrder = 0;
    active = false;
  }

  @Override
  public FacilityTypeDto buildAsDto() {
    FacilityTypeDto dto = new FacilityTypeDto();
    dto.setId(id);
    dto.setCode(code);
    dto.setName(name);
    dto.setDescription(description);
    dto.setDisplayOrder(displayOrder);
    dto.setActive(active);
    return dto;
  }

  public FacilityTypeDtoDataBuilder withDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
    return this;
  }

  public FacilityTypeDtoDataBuilder withActive(Boolean active) {
    this.active = active;
    return this;
  }
}
