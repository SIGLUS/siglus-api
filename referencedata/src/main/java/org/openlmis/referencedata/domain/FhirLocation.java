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

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.BooleanUtils.toBooleanObject;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.Optional;
import org.openlmis.referencedata.domain.BaseEntity.BaseImporter;
import org.openlmis.referencedata.domain.ExtraDataEntity.ExtraDataImporter;

public interface FhirLocation extends BaseImporter, ExtraDataImporter {

  /**
   * Checks if <strong>isManagedExternally</strong> flag is set.
   */
  @JsonIgnore
  default boolean isManagedExternally() {
    Object value = Optional
        .ofNullable(getExtraData())
        .orElse(Collections.emptyMap())
        .get("isManagedExternally");

    if (value instanceof String) {
      String valueAsString = (String) value;
      return !isBlank(valueAsString) && isTrue(toBooleanObject(valueAsString));
    }

    return false;
  }

}
