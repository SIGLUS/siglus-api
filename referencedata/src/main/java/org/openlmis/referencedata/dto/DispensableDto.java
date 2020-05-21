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

package org.openlmis.referencedata.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.Dispensable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispensableDto implements Dispensable.Exporter, Dispensable.Importer {

  private String dispensingUnit;
  private String sizeCode;
  private String routeOfAdministration;
  private String displayUnit;

  @Override
  @JsonIgnore
  public void setAttributes(Map<String, String> attributes) {
    dispensingUnit = attributes.get(Dispensable.KEY_DISPENSING_UNIT);
    sizeCode = attributes.get(Dispensable.KEY_SIZE_CODE);
    routeOfAdministration = attributes.get(Dispensable.KEY_ROUTE_OF_ADMINISTRATION);
  }

  @Override
  @JsonIgnore
  public void setToString(String toString) {
    displayUnit = toString;
  }

  @Override
  @JsonIgnore
  public Map<String, String> getAttributes() {
    Map<String, String> attributes = new HashMap<>();

    attributes.put(Dispensable.KEY_DISPENSING_UNIT, dispensingUnit);
    attributes.put(Dispensable.KEY_SIZE_CODE, sizeCode);
    attributes.put(Dispensable.KEY_ROUTE_OF_ADMINISTRATION, routeOfAdministration);

    return attributes;
  }
}
