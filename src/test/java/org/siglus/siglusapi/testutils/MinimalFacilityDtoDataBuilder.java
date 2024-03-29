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

package org.siglus.siglusapi.testutils;

import org.openlmis.requisition.dto.FacilityTypeDto;
import org.openlmis.requisition.dto.GeographicZoneDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.siglus.siglusapi.testutils.api.DtoDataBuilder;

public class MinimalFacilityDtoDataBuilder implements DtoDataBuilder<MinimalFacilityDto> {
  private String code;
  private String name;
  private Boolean active;
  private GeographicZoneDto geographicZone;
  private FacilityTypeDto type;

  /**
   * Builder for {@link MinimalFacilityDto}.
   */
  public MinimalFacilityDtoDataBuilder() {
    code = "code";
    name = "name";
    active = true;
    geographicZone = new GeographicZoneDtoDataBuilder().buildAsDto();
    type = new FacilityTypeDtoDataBuilder().buildAsDto();
  }

  @Override
  public MinimalFacilityDto buildAsDto() {
    MinimalFacilityDto dto = new MinimalFacilityDto();
    dto.setCode(code);
    dto.setName(name);
    dto.setActive(active);
    dto.setGeographicZone(geographicZone);
    dto.setType(type);
    return dto;
  }
}
