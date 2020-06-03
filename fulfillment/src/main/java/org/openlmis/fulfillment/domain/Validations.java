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

package org.openlmis.fulfillment.domain;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.openlmis.fulfillment.i18n.MessageKeys.MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO;
import static org.openlmis.fulfillment.i18n.MessageKeys.MUST_CONTAIN_VALUE;

import org.openlmis.fulfillment.web.ValidationException;

final class Validations {

  private Validations() {
    throw new UnsupportedOperationException();
  }

  static void throwIfBlank(String value, String field) {
    throwIfNull(value, field);

    if (isBlank(value)) {
      throw new ValidationException(MUST_CONTAIN_VALUE, field);
    }
  }

  static void throwIfNull(Object value, String field) {
    if (null == value) {
      throw new ValidationException(MUST_CONTAIN_VALUE, field);
    }
  }

  static void throwIfLessThanZero(Number value, String field) {
    if (value.doubleValue() < 0) {
      throw new ValidationException(MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO, field);
    }
  }

  static void throwIfLessThanZeroOrNull(Number value, String field) {
    throwIfNull(value, field);
    throwIfLessThanZero(value, field);
  }
}
