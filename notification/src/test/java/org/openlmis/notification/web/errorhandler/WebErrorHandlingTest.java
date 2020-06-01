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

package org.openlmis.notification.web.errorhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_CONSTRAINT;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_SEND_REQUEST;
import static org.openlmis.notification.i18n.MessageKeys.PERMISSION_MISSING;

import java.util.Locale;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.apache.commons.lang.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.notification.i18n.Message;
import org.openlmis.notification.i18n.Message.LocalizedMessage;
import org.openlmis.notification.i18n.MessageService;
import org.openlmis.notification.service.ServerException;
import org.openlmis.notification.web.MissingPermissionException;
import org.openlmis.notification.web.NotFoundException;
import org.openlmis.notification.web.ValidationException;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class WebErrorHandlingTest {
  private static final Locale ENGLISH_LOCALE = Locale.ENGLISH;
  private static final String MESSAGE_KEY = "key";
  private static final String ERROR_MESSAGE = "error-message";

  @Mock
  private MessageService messageService;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private WebErrorHandling errorHandler;

  @Before
  public void setUp() {
    when(messageService.localize(any(Message.class)))
        .thenAnswer(invocation -> {
          Message message = invocation.getArgumentAt(0, Message.class);
          return message.localMessage(messageSource, ENGLISH_LOCALE);
        });
  }

  @Test
  public void shouldHandleHttpStatusCodeException() {
    // given
    HttpStatusCodeException exp = mock(HttpStatusCodeException.class);
    when(exp.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(exp.getResponseBodyAsString()).thenReturn("body");

    // when
    mockMessage(ERROR_SEND_REQUEST, "400", "body");
    LocalizedMessage message = errorHandler.handleHttpStatusCodeException(exp);

    // then
    assertMessage(message, ERROR_SEND_REQUEST);
  }

  @Test
  public void shouldHandleMissingPermissionException() {
    // given
    MissingPermissionException exp = new MissingPermissionException(MESSAGE_KEY);

    // when
    mockMessage(PERMISSION_MISSING, MESSAGE_KEY);
    LocalizedMessage message = errorHandler.handleMissingPermissionException(exp);

    // then
    assertMessage(message, PERMISSION_MISSING);
  }

  @Test
  public void shouldHandleValidationException() {
    // given
    ValidationException exp = new ValidationException(MESSAGE_KEY);

    // when
    mockMessage(MESSAGE_KEY);
    LocalizedMessage message = errorHandler.handleValidationException(exp);

    // then
    assertMessage(message, MESSAGE_KEY);
  }

  @Test
  public void shouldHandleNotFoundException() {
    // given
    NotFoundException exp = new NotFoundException(MESSAGE_KEY);

    // when
    mockMessage(MESSAGE_KEY);
    LocalizedMessage message = errorHandler.handleNotFoundException(exp);

    // then
    assertMessage(message, MESSAGE_KEY);
  }

  @Test
  public void shouldHandlePersistenceException() {
    // given
    PersistenceException exp = mock(PersistenceException.class);

    // when
    mockMessage(ERROR_CONSTRAINT);
    LocalizedMessage message = errorHandler.handlePersistenceException(exp);

    // then
    assertMessage(message, ERROR_CONSTRAINT);
  }

  @Test
  public void shouldHandleDataIntegrityViolationException() {
    for (Map.Entry<String, String> entry : WebErrorHandling.CONSTRAINT_MAP.entrySet()) {
      ConstraintViolationException cause = mock(ConstraintViolationException.class);
      when(cause.getConstraintName()).thenReturn(entry.getKey());

      testDataIntegrityViolation(cause, entry.getValue());
    }
  }

  @Test
  public void shouldHandleDataIntegrityViolationExceptionWithUnhandledConstraint() {
    ConstraintViolationException cause = mock(ConstraintViolationException.class);
    when(cause.getConstraintName()).thenReturn("my_test_constraint_name");

    testDataIntegrityViolation(cause, null);
  }

  @Test
  public void shouldHandleDataIntegrityViolationExceptionWithoutConstraintViolationCause() {
    NullPointerException cause = mock(NullPointerException.class);

    testDataIntegrityViolation(cause, null);
  }

  @Test
  public void shouldHandleDataIntegrityViolationExceptionWithoutCause() {
    testDataIntegrityViolation(null, null);
  }

  private void testDataIntegrityViolation(Exception cause, String messageKey) {
    // given
    String nonNullMessageKey = StringUtils.defaultIfBlank(messageKey, "my_test_message_key");

    DataIntegrityViolationException exp = mock(DataIntegrityViolationException.class);

    when(exp.getCause()).thenReturn(cause);

    if (null == messageKey) {
      when(exp.getMessage()).thenReturn(nonNullMessageKey);
    }

    // when
    mockMessage(nonNullMessageKey);
    LocalizedMessage message = errorHandler.handleDataIntegrityViolation(exp);

    // then
    assertMessage(message, nonNullMessageKey);
  }

  @Test
  public void shouldHandleServerException() {
    // given
    ServerException exp = new ServerException(null, MESSAGE_KEY);

    // when
    mockMessage(MESSAGE_KEY);
    LocalizedMessage message = errorHandler.handleServerException(exp);

    // then
    assertMessage(message, MESSAGE_KEY);
  }

  private void assertMessage(LocalizedMessage localized, String key) {
    assertThat(localized)
        .hasFieldOrPropertyWithValue("messageKey", key);
    assertThat(localized)
        .hasFieldOrPropertyWithValue("message", ERROR_MESSAGE);
  }

  private void mockMessage(String key, String... params) {
    if (params.length == 0) {
      when(messageSource.getMessage(key, new Object[0], ENGLISH_LOCALE))
          .thenReturn(ERROR_MESSAGE);
      when(messageSource.getMessage(key, null, ENGLISH_LOCALE))
          .thenReturn(ERROR_MESSAGE);
    } else {
      when(messageSource.getMessage(key, params, ENGLISH_LOCALE))
          .thenReturn(ERROR_MESSAGE);
    }
  }

}
