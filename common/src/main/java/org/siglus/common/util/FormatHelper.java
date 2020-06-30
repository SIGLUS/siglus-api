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

package org.siglus.common.util;

import java.util.UUID;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.springframework.stereotype.Component;

@Component
public final class FormatHelper {

  // private or protected constructor
  // because all public fields and methods are static
  private FormatHelper() {
  }

  public static UUID formatId(String id, String fieldName) {
    if (id == null) {
      return null;
    }
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException ex) {
      throw new ValidationMessageException(ex,
          new Message("UUID {0} parameter has wrong format for field {1}",
              id, fieldName));
    }
  }

}
