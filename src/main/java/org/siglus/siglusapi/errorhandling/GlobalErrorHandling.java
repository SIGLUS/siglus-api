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

package org.siglus.siglusapi.errorhandling;

import static java.util.stream.Collectors.toList;
import static org.siglus.common.i18n.MessageKeys.ERROR_VALIDATION_FAIL;
import static org.zalando.problem.Problem.DEFAULT_TYPE;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.siglus.common.constant.DateFormatConstants;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.i18n.MessageKeys;
import org.siglus.siglusapi.constant.PodConstants;
import org.siglus.siglusapi.errorhandling.exception.OrderNotFoundException;
import org.siglus.siglusapi.errorhandling.message.ValidationFailField;
import org.siglus.siglusapi.i18n.ExposedMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemBuilder;
import org.zalando.problem.StatusType;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.validation.Violation;

/**
 * Global error handling for all controllers in the service. Contains common error handling mappings.
 */
@ControllerAdvice
@ParametersAreNonnullByDefault
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalErrorHandling implements ProblemHandling {

  private static final String MESSAGE_KEY = "messageKey";
  private static final String MESSAGE = "message";
  private static final String MESSAGE_IN_ENGLISH = "messageInEnglish";
  private static final String MESSAGE_IN_PORTUGUESE = "messageInPortuguese";
  private static final Map<String, String> CONSTRAINT_MAP = new ConcurrentHashMap<>();

  static {
    CONSTRAINT_MAP.put("unq_programid_additionalorderableid", MessageKeys.ERROR_ADDITIONAL_ORDERABLE_DUPLICATED);
  }

  private static final URI CONSTRAINT_VIOLATION_TYPE = URI.create("/errors/constraint-violation");

  private final ExposedMessageSource messageSource;

  @ExceptionHandler
  public ResponseEntity<Problem> handleGenericError(ValidationMessageException exception, NativeWebRequest request) {
    String messageKey = exception.asMessage().getKey();
    ThrowableProblem problem = prepare(messageKey, exception, BAD_REQUEST, DEFAULT_TYPE).build();
    return create(exception, problem, request);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleDataIntegrityViolation(DataIntegrityViolationException exception,
      NativeWebRequest request) {
    String messageKey = null;
    if (exception.getCause() instanceof ConstraintViolationException) {
      ConstraintViolationException cause = (ConstraintViolationException) exception.getCause();
      messageKey = CONSTRAINT_MAP.get(cause.getConstraintName());
    }
    if (messageKey == null) {
      messageKey = exception.getMessage();
    }
    ThrowableProblem problem = prepare(messageKey, exception, BAD_REQUEST, DEFAULT_TYPE).build();
    return create(exception, problem, request);
  }

  @Override
  public ResponseEntity<Problem> newConstraintViolationProblem(Throwable throwable, Collection<Violation> violations,
      NativeWebRequest request) {
    StatusType status = defaultConstraintViolationStatus();
    List<ValidationFailField> fields = violations.stream().map(ValidationFailField::new).collect(toList());
    ThrowableProblem problem = prepare(ERROR_VALIDATION_FAIL, throwable, status, CONSTRAINT_VIOLATION_TYPE)
        .with("fields", fields)
        .build();
    return create(throwable, problem, request);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleValidationException(ValidationException exception, NativeWebRequest request) {
    String detail = String.format("%s Caused by %s", exception.getMessage(), exception.getCause());
    ThrowableProblem problem = prepare(exception, BAD_REQUEST, DEFAULT_TYPE).withDetail(detail).build();
    return create(exception, problem, request);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleOrderNotFoundException(OrderNotFoundException exception,
      NativeWebRequest request) {
    String messageKey = exception.asMessage().getKey();
    ThrowableProblem problem = prepare(messageKey, exception, NOT_FOUND, DEFAULT_TYPE).with(PodConstants.ORDER_NUMBER,
        exception.getOrderCode()).build();
    return create(exception, problem, request);
  }

  @Override
  public void log(Throwable throwable, Problem problem, NativeWebRequest request, HttpStatus status) {
    if (throwable instanceof javax.validation.ConstraintViolationException) {
      return;
    }
    log.error(status.getReasonPhrase(), throwable);
  }

  public ProblemBuilder prepare(String messageKey, Throwable throwable, StatusType status, URI type) {
    String localizedMessage = getLocalizedMessage(messageKey, LocaleContextHolder.getLocale());
    String messageInEnglish = getLocalizedMessage(messageKey, Locale.ENGLISH);
    String messageInPortuguese = getLocalizedMessage(messageKey, DateFormatConstants.PORTUGAL);
    return prepare(throwable, status, type)
        .with(MESSAGE_KEY, messageKey)
        .with(MESSAGE, localizedMessage)
        .with(MESSAGE_IN_ENGLISH, messageInEnglish)
        .with(MESSAGE_IN_PORTUGUESE, messageInPortuguese);
  }

  private String getLocalizedMessage(String key, Locale locale) {
    return messageSource.getMessage(key, null, locale);
  }

}
