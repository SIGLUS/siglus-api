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

package org.openlmis.referencedata.domain;

import java.util.Objects;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * A vaccine dispensable describes a dispensable by two properties, number of doses, represented
 * by the key called "sizeCode", and how it is administered into the body, represented by the key
 * "routeOfAdministration".
 */
@Entity
@DiscriminatorValue("vaccine")
@NoArgsConstructor
public class VaccineDispensable extends Dispensable {

  VaccineDispensable(String sizeCode, String routeOfAdministration) {
    super();
    Objects.requireNonNull(sizeCode);
    Objects.requireNonNull(routeOfAdministration);
    attributes.put(KEY_SIZE_CODE, sizeCode);
    attributes.put(KEY_ROUTE_OF_ADMINISTRATION, routeOfAdministration);
  }

  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (!(object instanceof VaccineDispensable)) {
      return false;
    }

    return this.attributes.get(KEY_SIZE_CODE).equalsIgnoreCase(
        ((VaccineDispensable) object).attributes.get(KEY_SIZE_CODE))
        && this.attributes.get(KEY_ROUTE_OF_ADMINISTRATION).equalsIgnoreCase(
            ((VaccineDispensable) object).attributes.get(KEY_ROUTE_OF_ADMINISTRATION));
  }

  @Override
  public int hashCode() {
    return attributes.hashCode();
  }

  @Override
  public String toString() {
    return StringUtils.joinWith(",",
        attributes.getOrDefault(KEY_SIZE_CODE, ""),
        attributes.getOrDefault(KEY_ROUTE_OF_ADMINISTRATION, ""));
  }
}
