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
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class GeographicLevelDtoDataBuilder implements DtoDataBuilder<GeographicLevelDto> {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private Integer levelNumber;

  /**
   * Creates builder for creating new instance of {@link GeographicLevelDto}.
   */
  public GeographicLevelDtoDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "GL" + instanceNumber;
    name = "geographic level " + instanceNumber;
    levelNumber = 1;
  }

  /**
   * Creates new instance of {@link GeographicLevelDto} with properties.
   * @return created facility.
   */
  public GeographicLevelDto buildAsDto() {
    return new GeographicLevelDto(id, code, name, levelNumber);
  }
}
