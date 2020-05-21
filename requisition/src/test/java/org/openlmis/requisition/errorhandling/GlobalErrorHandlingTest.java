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

package org.openlmis.requisition.errorhandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.i18n.MessageService;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.Message.LocalizedMessage;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class GlobalErrorHandlingTest {
  private static final Locale ENGLISH_LOCALE = Locale.ENGLISH;
  private static final String ERROR_MESSAGE = "error-message";

  @Mock
  private MessageService messageService;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private GlobalErrorHandling errorHandler;

  @Before
  public void setUp() {
    when(messageService.localize(any(Message.class)))
        .thenAnswer(invocation -> {
          Message message = invocation.getArgumentAt(0, Message.class);
          return message.localMessage(messageSource, ENGLISH_LOCALE);
        });
  }

  @Test
  public void shouldHandleDataIntegrityViolation() {
    // given
    String constraintName = "req_prod_fac_per";
    ConstraintViolationException constraintViolation = new ConstraintViolationException(
        null, null, constraintName);
    DataIntegrityViolationException exp = new DataIntegrityViolationException(
        null, constraintViolation);

    // when
    mockMessage(MessageKeys.ERROR_REQUISITION_DUPLICATION);
    LocalizedMessage message = errorHandler.handleDataIntegrityViolation(exp);

    // then
    assertMessage(message, MessageKeys.ERROR_REQUISITION_DUPLICATION);
  }

  @Test
  public void shouldHandleDataIntegrityViolationEvenIfMessageKeyNotExist() {
    // given
    String constraintName = "req_prod_fac_per_def";
    ConstraintViolationException constraintViolation = new ConstraintViolationException(
        null, null, constraintName);
    DataIntegrityViolationException exp = new DataIntegrityViolationException(
        null, constraintViolation);

    // when
    mockMessage(exp.getMessage());
    LocalizedMessage message = errorHandler.handleDataIntegrityViolation(exp);

    // then
    assertMessage(message, exp.getMessage());
  }

  @Test
  public void shouldHandleDataIntegrityViolationEvenIfCauseNotExist() {
    // given
    DataIntegrityViolationException exp = new DataIntegrityViolationException(ERROR_MESSAGE, null);

    // when
    mockMessage(exp.getMessage());
    LocalizedMessage message = errorHandler.handleDataIntegrityViolation(exp);

    // then
    assertMessage(message, exp.getMessage());
  }

  @Test
  public void shouldHandleMessageException() {
    // given
    String messageKey = "key";
    ValidationMessageException exp = new ValidationMessageException(messageKey);

    // when
    mockMessage(messageKey);
    LocalizedMessage message = errorHandler.handleMessageException(exp);

    // then
    assertMessage(message, messageKey);
  }

  private void assertMessage(LocalizedMessage localized, String key) {
    assertThat(localized).hasFieldOrPropertyWithValue("messageKey", key);
    assertThat(localized).hasFieldOrPropertyWithValue("message", ERROR_MESSAGE);
  }

  private void mockMessage(String key, String... params) {
    String[] parameters = params.length == 0 ? null : params;

    when(messageSource.getMessage(key, parameters, ENGLISH_LOCALE))
        .thenReturn(ERROR_MESSAGE);
  }
}
