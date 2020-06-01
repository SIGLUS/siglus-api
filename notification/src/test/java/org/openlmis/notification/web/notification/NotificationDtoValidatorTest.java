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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_NOTIFICATION_REQUEST_FIELD_REQUIRED;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_NOTIFICATION_REQUEST_MESSAGES_EMPTY;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_UNSUPPORTED_NOTIFICATION_CHANNEL;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.NotificationMessage;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.util.NotificationDataBuilder;
import org.openlmis.notification.web.BaseValidatorTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class NotificationDtoValidatorTest extends BaseValidatorTest {
  private NotificationDtoValidator validator = new NotificationDtoValidator();
  private NotificationDto request = new NotificationDto();
  private Errors errors;

  @Before
  public void setUp() {
    Notification notification = new NotificationDataBuilder()
        .withMessage(NotificationChannel.EMAIL, "body", "subject")
        .buildAsNew();
    notification.export(request);

    errors = new BeanPropertyBindingResult(request, "request");
  }

  @Test
  public void shouldValidate() {
    validator.validate(request, errors);
    assertThat(errors.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void shouldRejectIfUserIdIsNull() {
    request.setUserId(null);

    validator.validate(request, errors);
    assertErrorMessage(errors, "userId", ERROR_NOTIFICATION_REQUEST_FIELD_REQUIRED);
  }

  @Test
  public void shouldRejectIfMessagesAreNotSet() {
    request.setMessages(Collections.emptyList());

    validator.validate(request, errors);
    assertErrorMessage(errors, "messages", ERROR_NOTIFICATION_REQUEST_MESSAGES_EMPTY);
  }

  @Test
  public void shouldRejectIfMessageBodyIsEmpty() {
    request.setMessages(Collections.singletonList(
        new NotificationMessage(NotificationChannel.EMAIL, "")));

    validator.validate(request, errors);
    assertErrorMessage(errors, "messages", ERROR_NOTIFICATION_REQUEST_FIELD_REQUIRED);
  }

  @Test
  public void shouldRejectIfChannelIsNotSupported() {
    request.setMessageMap(Collections.singletonMap("ab", new MessageDto()));

    validator.validate(request, errors);
    assertErrorMessage(errors, "messages", ERROR_UNSUPPORTED_NOTIFICATION_CHANNEL);
  }
}
