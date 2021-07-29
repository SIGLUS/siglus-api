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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.i18n.MessageKeys;
import org.siglus.common.util.Message;
import org.siglus.common.util.Message.LocalizedMessage;
import org.siglus.siglusapi.errorhandling.message.ValidationFailField;
import org.siglus.siglusapi.exception.NotAcceptableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
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
public class GlobalErrorHandling extends AbstractErrorHandling implements ProblemHandling {

  private static final String MESSAGE_KEY = "messageKey";
  private static final String MESSAGE = "message";
  private static final Map<String, String> CONSTRAINT_MAP = new ConcurrentHashMap<>();

  static {
    CONSTRAINT_MAP.put("unq_widget_code", MessageKeys.ERROR_WIDGET_CODE_DUPLICATED);
    CONSTRAINT_MAP.put("unq_programid_additionalorderableid", MessageKeys.ERROR_ADDITIONAL_ORDERABLE_DUPLICATED);
  }

  private static final URI CONSTRAINT_VIOLATION_TYPE = URI.create("/errors/constraint-violation");

  @ExceptionHandler(NotAcceptableException.class)
  @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
  @ResponseBody
  public Message.LocalizedMessage handlePermissionException(NotAcceptableException ex) {
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleDataIntegrityViolation(ValidationMessageException exception,
      NativeWebRequest request) {
    LocalizedMessage localizedMessage = getLocalizedMessage(exception);
    ThrowableProblem problem = prepare(exception, BAD_REQUEST, DEFAULT_TYPE)
        .with(MESSAGE_KEY, localizedMessage.getMessageKey())
        .with(MESSAGE, localizedMessage.getMessage())
        .build();
    return create(exception, problem, request);
  }

  @ExceptionHandler
  public ResponseEntity<Problem> handleDataIntegrityViolation(DataIntegrityViolationException exception,
      NativeWebRequest request) {
    String messageKey = null;
    String message = null;
    if (exception.getCause() instanceof ConstraintViolationException) {
      ConstraintViolationException cause = (ConstraintViolationException) exception.getCause();
      messageKey = CONSTRAINT_MAP.get(cause.getConstraintName());
      if (messageKey != null) {
        message = getLocalizedMessage(new Message(messageKey)).getMessage();
      }
    }
    if (messageKey == null) {
      messageKey = exception.getMessage();
    }
    ThrowableProblem problem = prepare(exception, BAD_REQUEST, DEFAULT_TYPE)
        .with(MESSAGE_KEY, messageKey)
        .with(MESSAGE, message != null ? message : messageKey)
        .build();
    return create(exception, problem, request);
  }

  @Override
  public ResponseEntity<Problem> newConstraintViolationProblem(Throwable throwable, Collection<Violation> violations,
      NativeWebRequest request) {
    StatusType status = defaultConstraintViolationStatus();
    LocalizedMessage localizedMessage = getLocalizedMessage(new Message(ERROR_VALIDATION_FAIL));
    List<ValidationFailField> fields = violations.stream().map(ValidationFailField::new).collect(toList());
    ThrowableProblem problem = prepare(throwable, status, CONSTRAINT_VIOLATION_TYPE)
        .with(MESSAGE_KEY, localizedMessage.getMessageKey())
        .with(MESSAGE, localizedMessage.getMessage())
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

  @Override
  public void log(Throwable throwable, Problem problem, NativeWebRequest request, HttpStatus status) {
    if (throwable instanceof javax.validation.ConstraintViolationException) {
      ArrayList<ValidationFailField> fields = (ArrayList<ValidationFailField>) problem.getParameters().get("fields");
      if (fields != null) {
        fields.forEach(field -> log.error("{}: {}", field.getPropertyPath(), field.getMessage()));
      }
      return;
    }
    log.error(status.getReasonPhrase(), throwable);
  }

}
