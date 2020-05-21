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

/**
 * A default dispensable is the default usage of a dispensable. This class describes a dispensable
 * by a single key called "dispensingUnit".
 */
@Entity
@DiscriminatorValue("default")
@NoArgsConstructor
public class DefaultDispensable extends Dispensable {

  DefaultDispensable(String dispensingUnit) {
    super();
    Objects.requireNonNull(dispensingUnit);
    attributes.put(KEY_DISPENSING_UNIT, dispensingUnit);
  }

  @Override
  public final boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (!(object instanceof DefaultDispensable)) {
      return false;
    }

    return attributes.get(KEY_DISPENSING_UNIT)
        .equalsIgnoreCase(((DefaultDispensable) object).attributes.get(KEY_DISPENSING_UNIT));
  }

  @Override
  public final int hashCode() {
    return attributes.hashCode();
  }

  @Override
  public String toString() {
    return attributes.getOrDefault(KEY_DISPENSING_UNIT, "");
  }
}
