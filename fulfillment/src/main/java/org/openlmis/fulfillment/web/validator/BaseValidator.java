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

package org.openlmis.fulfillment.web.validator;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.openlmis.fulfillment.i18n.MessageKeys.MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO;
import static org.openlmis.fulfillment.i18n.MessageKeys.MUST_CONTAIN_VALUE;

import java.util.List;
import org.openlmis.fulfillment.i18n.MessageService;
import org.openlmis.fulfillment.util.Message;
import org.springframework.beans.factory.annotation.Autowired;

abstract class BaseValidator {

  @Autowired
  private MessageService messageService;

  void rejectIfNull(List<Message.LocalizedMessage> errors, Object value, String field) {
    if (null == value) {
      errors.add(getErrorMessage(MUST_CONTAIN_VALUE, field));
    }
  }

  void rejectIfLessThanZero(List<Message.LocalizedMessage> errors, Number value,
                            String field) {
    if (null == value) {
      errors.add(getErrorMessage(MUST_CONTAIN_VALUE, field));
    } else if (value.doubleValue() < 0) {
      errors.add(getErrorMessage(MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO, field));
    }
  }

  void rejectIfBlank(List<Message.LocalizedMessage> errors, String value, String field) {
    if (isBlank(value)) {
      errors.add(getErrorMessage(MUST_CONTAIN_VALUE, field));
    }
  }

  protected Message.LocalizedMessage getErrorMessage(String key, Object... params) {
    return messageService.localize(new Message(key, params));
  }

}
