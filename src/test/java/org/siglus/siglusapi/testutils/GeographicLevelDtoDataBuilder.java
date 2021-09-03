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

import java.util.UUID;
import org.openlmis.stockmanagement.dto.referencedata.GeographicLevelDto;

public class GeographicLevelDtoDataBuilder {

  private UUID id;
  private String code;
  private String name;
  private Integer levelNumber;

  public GeographicLevelDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public GeographicLevelDtoDataBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  public GeographicLevelDtoDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public GeographicLevelDtoDataBuilder withLevelNumber(Integer levelNumber) {
    this.levelNumber = levelNumber;
    return this;
  }

  /**
   * Creates new instance of {@link GeographicLevelDto} with properties.
   *
   * @return created geographic level dto.
   */
  public GeographicLevelDto build() {
    GeographicLevelDto geographicLevelDto = new GeographicLevelDto(code, name, levelNumber);
    geographicLevelDto.setId(id);
    return geographicLevelDto;
  }
}
