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
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_VALIDATION_FAIL;
import static org.zalando.problem.Problem.DEFAULT_TYPE;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.siglus.common.constant.DateFormatConstants;
import org.siglus.siglusapi.constant.PodConstants;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.AndroidApiException;
import org.siglus.siglusapi.exception.BaseMessageException;
import org.siglus.siglusapi.exception.OrderNotFoundException;
import org.siglus.siglusapi.exception.UnsupportedProductsException;
import org.siglus.siglusapi.i18n.ExposedMessageSource;
import org.siglus.siglusapi.i18n.MessageKeys;
import org.springframework.context.i18n.LocaleContextHolder;
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
@Order
public class GlobalErrorHandling implements ProblemHandling {

  private static final URI CONSTRAINT_VIOLATION_TYPE = URI.create("/errors/constraint-violation");
  private static final String MESSAGE_KEY = "messageKey";
  private static final String MESSAGE = "message";
  private static final String MESSAGE_IN_ENGLISH = "messageInEnglish";
  private static final String MESSAGE_IN_PORTUGUESE = "messageInPortuguese";
  private static final Map<String, String> CONSTRAINT_MAP = new ConcurrentHashMap<>();
  private static final Set<String> ANDROID_VIOLATIONS;

  static {
    CONSTRAINT_MAP.put("unq_programid_additionalorderableid", MessageKeys.ERROR_ADDITIONAL_ORDERABLE_DUPLICATED);
    HashSet<String> androidViolations = new HashSet<>();
    androidViolations.add("{org.siglus.siglusapi.dto.android.constraint.RequisitionValidStartDate.message}");
    ANDROID_VIOLATIONS = Collections.unmodifiableSet(androidViolations);
  }

  private final ExposedMessageSource messageSource;

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
    List<FieldViolation> fields = violations.stream().map(FieldViolation::new).collect(toList());
    ThrowableProblem problem = fields.stream()
        .filter(fieldViolation -> ANDROID_VIOLATIONS.contains(fieldViolation.getMessageTemplate()))
        .map(androidViolation -> prepare(new LocalizedMessage(androidViolation), throwable, BAD_REQUEST, DEFAULT_TYPE))
        .findAny().orElseGet(
            () -> prepare(ERROR_VALIDATION_FAIL, throwable, status, CONSTRAINT_VIOLATION_TYPE).with("fields", fields)
        ).build();
    return create(throwable, problem, request);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleValidationException(ValidationException exception, NativeWebRequest request) {
    String detail = String.format("%s Caused by %s", exception.getMessage(), exception.getCause());
    ThrowableProblem problem = prepare(exception, BAD_REQUEST, DEFAULT_TYPE).withDetail(detail).build();
    return create(exception, problem, request);
  }

  // --- Start: Handle siglus exception ---
  @ExceptionHandler
  public ResponseEntity<Problem> handleBaseMessageException(BaseMessageException exception, NativeWebRequest request) {
    ThrowableProblem problem = prepare(exception).build();
    return create(exception, problem, request);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleOrderNotFoundException(OrderNotFoundException exception,
      NativeWebRequest request) {
    ThrowableProblem problem = prepare(exception)
        .with(PodConstants.ORDER_NUMBER, exception.getOrderCode())
        .build();
    return create(exception, problem, request);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleUnsupportedProductsException(UnsupportedProductsException exception,
      NativeWebRequest request) {
    ThrowableProblem problem = prepare(exception)
        .with("productCodes", exception.getProductCodes())
        .build();
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
    LocalizedMessage localizedMessage = new LocalizedMessage(messageKey, messageSource);
    return prepare(localizedMessage, throwable, status, type);
  }

  private ProblemBuilder prepare(BaseMessageException throwable) {
    Message message = throwable.asMessage();
    LocalizedMessage localizedMessage = new LocalizedMessage(message, messageSource);
    return prepare(localizedMessage, throwable, BAD_REQUEST, Problem.DEFAULT_TYPE);
  }

  private ProblemBuilder prepare(LocalizedMessage localizedMessage, Throwable throwable, StatusType status, URI type) {
    ProblemBuilder problemBuilder = ProblemHandling.super.prepare(throwable, status, type)
        .with(MESSAGE_KEY, localizedMessage.messageKey)
        .with(MESSAGE, localizedMessage.message)
        .with(MESSAGE_IN_ENGLISH, localizedMessage.messageInEnglish)
        .with(MESSAGE_IN_PORTUGUESE, localizedMessage.messageInPortuguese);
    if (throwable instanceof AndroidApiException) {
      problemBuilder.with("isAndroid", true);
    }
    return problemBuilder;
  }

  @RequiredArgsConstructor
  private static class LocalizedMessage {

    final String messageKey;
    final String message;
    final String messageInEnglish;
    final String messageInPortuguese;

    LocalizedMessage(FieldViolation violation) {
      messageKey = violation.getMessageTemplate();
      message = violation.getMessage();
      messageInEnglish = violation.getMessageInEnglish();
      messageInPortuguese = violation.getMessageInPortuguese();
    }

    LocalizedMessage(Message message, ExposedMessageSource messageSource) {
      messageKey = message.getKey();
      this.message = getLocalizedMessage(message, LocaleContextHolder.getLocale(), messageSource);
      messageInEnglish = getLocalizedMessage(message, Locale.ENGLISH, messageSource);
      messageInPortuguese = getLocalizedMessage(message, DateFormatConstants.PORTUGAL, messageSource);
    }

    LocalizedMessage(String messageKey, ExposedMessageSource messageSource) {
      this(new Message(messageKey), messageSource);
    }

    private String getLocalizedMessage(Message message, Locale locale, ExposedMessageSource messageSource) {
      return messageSource.getMessage(message.getKey(), message.getParams(), locale);
    }

  }

}
