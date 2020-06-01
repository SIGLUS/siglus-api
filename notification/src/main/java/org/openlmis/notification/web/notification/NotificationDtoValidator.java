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

package org.openlmis.notification.web.notification;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_NOTIFICATION_REQUEST_FIELD_REQUIRED;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_NOTIFICATION_REQUEST_MESSAGES_EMPTY;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_NOTIFICATION_REQUEST_NULL;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_UNSUPPORTED_NOTIFICATION_CHANNEL;

import java.util.Map;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.web.BaseValidator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;

@Component
public class NotificationDtoValidator implements BaseValidator {

  private static final String FIELD_NAME_MESSAGES = "messages";
  
  @Override
  public boolean supports(Class<?> clazz) {
    return NotificationDto.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    verifyArguments(target, errors, ERROR_NOTIFICATION_REQUEST_NULL);
    rejectIfEmptyOrWhitespace(errors, "userId", ERROR_NOTIFICATION_REQUEST_FIELD_REQUIRED);

    if (!errors.hasErrors()) {
      NotificationDto dto = (NotificationDto) target;

      if (CollectionUtils.isEmpty(dto.getMessageMap())) {
        rejectValue(errors, FIELD_NAME_MESSAGES, ERROR_NOTIFICATION_REQUEST_MESSAGES_EMPTY);
      } else {
        validateMessages(errors, dto);
      }
    }
  }

  private void validateMessages(Errors errors, NotificationDto dto) {
    for (Map.Entry<String, MessageDto> entry : dto.getMessageMap().entrySet()) {
      String key = entry.getKey();
      MessageDto message = entry.getValue();
      if (null == NotificationChannel.fromString(key)) {
        errors.rejectValue(FIELD_NAME_MESSAGES, ERROR_UNSUPPORTED_NOTIFICATION_CHANNEL,
            new String[]{key}, ERROR_UNSUPPORTED_NOTIFICATION_CHANNEL);
      }
      if (isBlank(message.getBody())) {
        rejectValue(errors, FIELD_NAME_MESSAGES, ERROR_NOTIFICATION_REQUEST_FIELD_REQUIRED);
      }
    }
  }
}
