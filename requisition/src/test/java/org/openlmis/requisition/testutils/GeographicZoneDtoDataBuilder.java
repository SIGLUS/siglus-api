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
import org.openlmis.requisition.dto.GeographicLevelDto;
import org.openlmis.requisition.dto.GeographicZoneDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class GeographicZoneDtoDataBuilder implements DtoDataBuilder<GeographicZoneDto> {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private GeographicLevelDto level;
  private GeographicZoneDto parent;

  /**
   * Creates builder for creating new instance of {@link GeographicZoneDto}.
   */
  public GeographicZoneDtoDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "Z" + instanceNumber;
    name = "zone " + instanceNumber;
    level = new GeographicLevelDtoDataBuilder().buildAsDto();
    parent = null;
  }

  /**
   * Creates new instance of {@link GeographicZoneDto} with properties.
   * @return created facility.
   */
  public GeographicZoneDto buildAsDto() {
    return new GeographicZoneDto(id, code, name, level, parent);
  }

  public GeographicZoneDtoDataBuilder withLevel(GeographicLevelDto level) {
    this.level = level;
    return this;
  }
}
