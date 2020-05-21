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

package org.openlmis.referencedata.errorhandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Locale;
import javax.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.exception.IntegrityViolationException;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.UnauthorizedException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.i18n.MessageService;
import org.openlmis.referencedata.util.LocalizedMessage;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.OrderableMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class RefDataErrorHandlingTest {
  private static final Locale ENGLISH_LOCALE = Locale.ENGLISH;
  private static final String ERROR_MESSAGE = "error-message";

  @Mock
  private MessageService messageService;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private RefDataErrorHandling errorHandler;

  @Before
  public void setUp() {
    when(messageService.localize(any(Message.class)))
        .thenAnswer(invocation -> {
          Message message = invocation.getArgumentAt(0, Message.class);
          return message.localMessage(messageSource, ENGLISH_LOCALE);
        });
  }

  @Test
  public void shouldHandleIntegrityViolationException() {
    // given
    IntegrityViolationException exp = new IntegrityViolationException(ERROR_MESSAGE, null);

    // when
    mockMessage(ERROR_MESSAGE + ":");
    LocalizedMessage message = errorHandler.handleIntegrityViolationException(exp);

    // then
    assertMessage(message, ERROR_MESSAGE + ":");
  }

  @Test
  public void shouldHandleDataIntegrityViolation() {
    // given
    String constraintName = "unq_program_code";
    ConstraintViolationException constraintViolation = new ConstraintViolationException(
        null, null, constraintName);
    DataIntegrityViolationException exp = new DataIntegrityViolationException(
        null, constraintViolation);

    // when
    mockMessage(ProgramMessageKeys.ERROR_CODE_DUPLICATED);
    LocalizedMessage message = errorHandler.handleDataIntegrityViolation(exp);

    // then
    assertMessage(message, ProgramMessageKeys.ERROR_CODE_DUPLICATED);
  }

  @Test
  public void shouldHandleDataIntegrityViolationEvenIfMessageKeyNotExist() {
    // given
    String constraintName = "unq_program_code_abc_def";
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
    ValidationMessageException exp = new ValidationMessageException(ERROR_MESSAGE);

    // when
    mockMessage(ERROR_MESSAGE);
    LocalizedMessage message = errorHandler.handleMessageException(exp);

    // then
    assertMessage(message, ERROR_MESSAGE);
  }

  @Test
  public void shouldHandleNotFoundException() {
    // given
    NotFoundException exp = new NotFoundException(ERROR_MESSAGE);

    // when
    mockMessage(ERROR_MESSAGE);
    LocalizedMessage message = errorHandler.handleNotFoundException(exp);

    // then
    assertMessage(message, ERROR_MESSAGE);
  }

  @Test
  public void shouldHandleUnauthorizedException() {
    // given
    UnauthorizedException exp = new UnauthorizedException(new Message(ERROR_MESSAGE));

    // when
    mockMessage(ERROR_MESSAGE);
    LocalizedMessage message = errorHandler.handleUnauthorizedException(exp);

    // then
    assertMessage(message, ERROR_MESSAGE);
  }

  @Test
  public void shouldHandleSqlException() {
    // given
    String messageKey = OrderableMessageKeys.ERROR_NOT_FOUND;
    JpaSystemException exp = new JpaSystemException(
        new PersistenceException(
            new SQLException("sql error", "23503", new NullPointerException(messageKey))));

    // when
    mockMessage(messageKey);
    LocalizedMessage message = errorHandler.handleJpaSystemException(exp);

    // then
    assertMessage(message, messageKey);
  }

  @Test
  public void shouldReturnNormalMessageIfExceptionIsNotPersistenceException() {
    // given
    JpaSystemException exp = new JpaSystemException(new NullPointerException(ERROR_MESSAGE));

    // when
    mockMessage(exp.getMessage());
    LocalizedMessage message = errorHandler.handleJpaSystemException(exp);

    // then
    assertMessage(message, exp.getMessage());
  }

  @Test
  public void shouldReturnNormalMessageIfExceptionIsNotSqlException() {
    // given
    JpaSystemException exp = new JpaSystemException(
        new PersistenceException(new NullPointerException(ERROR_MESSAGE)));

    // when
    mockMessage(exp.getMessage());
    LocalizedMessage message = errorHandler.handleJpaSystemException(exp);

    // then
    assertMessage(message, exp.getMessage());
  }

  @Test
  public void shouldReturnNormalMessageIfSqlStatusIsNotRecognizable() {
    // given
    JpaSystemException exp = new JpaSystemException(
        new PersistenceException(
            new SQLException("sql error", "10", new NullPointerException(ERROR_MESSAGE))));

    // when
    mockMessage(exp.getMessage());
    LocalizedMessage message = errorHandler.handleJpaSystemException(exp);

    // then
    assertMessage(message, exp.getMessage());
  }

  private void assertMessage(LocalizedMessage localized, String key) {
    assertThat(localized)
        .hasFieldOrPropertyWithValue(LocalizedMessage.MESSAGE_KEY_FIELD, key);
    assertThat(localized)
        .hasFieldOrPropertyWithValue(LocalizedMessage.MESSAGE_FIELD, ERROR_MESSAGE);
  }

  private void mockMessage(String key, String... params) {
    when(messageSource.getMessage(key, params, ENGLISH_LOCALE))
        .thenReturn(ERROR_MESSAGE);
  }
}
