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

package org.openlmis.notification.web;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import org.openlmis.notification.i18n.Message;
import org.openlmis.notification.i18n.MessageKeys;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public interface BaseValidator extends Validator {

  /**
   * Rejects if the value of the field has changed.
   *
   * @param errors  the errors object
   * @param oldData  the old value
   * @param newData  the new value
   * @param field  the name of the field
   * @param message  the message to reject with
   */
  default void rejectIfNotEqual(Errors errors, Object oldData, Object newData, String field,
      String message) {
    if (!Objects.equals(oldData, newData)) {
      rejectValue(errors, field, message);
    }
  }

  default void rejectIfEmptyOrWhitespace(Errors errors, String field, String message) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, field, message, message);
  }

  default void rejectValue(Errors errors, String field, String message) {
    errors.rejectValue(field, message, message);
  }

  /**
   * Verifies that the given object is not null.
   *
   * @param target  the object to be tested
   * @param errors  the errors object
   * @param errorNull  the message key to be set if the object is null
   */
  default void verifyArguments(Object target, Errors errors, String errorNull) {
    Message targetMessage = new Message(errorNull);
    checkNotNull(target, targetMessage.toString());
    Message errorsMessage = new Message(MessageKeys.ERROR_CONTEXTUAL_STATE_NULL);
    checkNotNull(errors, errorsMessage.toString());
  }

}
