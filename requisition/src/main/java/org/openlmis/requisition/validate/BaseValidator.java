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

package org.openlmis.requisition.validate;

import static org.apache.commons.lang3.StringUtils.length;

import java.util.Collection;
import java.util.regex.Pattern;
import org.openlmis.requisition.utils.Message;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

abstract class BaseValidator implements Validator {
  // Regex is: UTF-8 exclude space, then UTF-8 include space
  // Note the following regex isn't exactly representing UTF-8, as it does not support Unicode code
  // points outside of the BMP (basic multilingual plane), like emojis. A more complex regex would
  // need to be found in order to support full UTF-8.
  private static final String UTF8_REGEX = "^[\\u0021-\\uFFFF][\\u0020-\\uFFFF]+$";
  private static final Pattern UTF8_PATTERN = Pattern.compile(UTF8_REGEX);

  protected <T> void rejectIfNotContains(Errors errors, Collection<T> collection, T value,
                                         String field, Message message) {
    if (!collection.contains(value)) {
      rejectValue(errors, field, message);
    }
  }

  protected void rejectIfLengthTooLong(Errors errors, String value, int max, String field,
                                       Message message) {
    if (length(value) > max) {
      rejectValue(errors, field, message);
    }
  }

  protected void rejectIfNotUtf8(Errors errors, String value, String field, Message message) {
    if (null == value || !UTF8_PATTERN.matcher(value).find()) {
      rejectValue(errors, field, message);
    }
  }

  protected void rejectValue(Errors errors, String field, Message message) {
    errors.rejectValue(field, message.toString());
  }

}
