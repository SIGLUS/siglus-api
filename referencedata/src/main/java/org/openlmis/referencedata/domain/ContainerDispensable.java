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
 * A container dispensable describes a dispensable by a single property, size, represented by the
 * key called "sizeCode".
 */
@Entity
@DiscriminatorValue("container")
@NoArgsConstructor
public class ContainerDispensable extends Dispensable {

  ContainerDispensable(String sizeCode) {
    super();
    Objects.requireNonNull(sizeCode);
    attributes.put(KEY_SIZE_CODE, sizeCode);
  }

  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (!(object instanceof ContainerDispensable)) {
      return false;
    }

    return this.attributes.get(KEY_SIZE_CODE)
        .equalsIgnoreCase(((ContainerDispensable) object).attributes.get(KEY_SIZE_CODE));
  }

  @Override
  public int hashCode() {
    return attributes.hashCode();
  }

  @Override
  public String toString() {
    return attributes.getOrDefault(KEY_SIZE_CODE, "");
  }

  public static Dispensable createNew(String sizeCode) {
    String correctSizeCode = (null == sizeCode) ? "" : sizeCode;
    return new ContainerDispensable(correctSizeCode);
  }
}
