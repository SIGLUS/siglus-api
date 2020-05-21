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

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DUPLICATE_STATUS_CHANGE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_PROGRAM_FACILITY_TYPE_ASSIGNMENT_EXISTS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_DUPLICATION;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_DUPLICATION_WITH_NODE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_TEMPLATE_ASSIGNMENT;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_TEMPLATE_NAME_DUPLICATION;
import static org.openlmis.requisition.i18n.MessageKeys.VERSION_MISMATCH;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.openlmis.requisition.dto.LocalizedMessageDto;
import org.openlmis.requisition.exception.AuthenticationMessageException;
import org.openlmis.requisition.exception.ExternalApiException;
import org.openlmis.requisition.exception.IdempotencyKeyException;
import org.openlmis.requisition.exception.ServerException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.exception.VersionMismatchException;
import org.openlmis.requisition.service.DataRetrievalException;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.web.PermissionMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Global error handling for all controllers in the service.
 * Contains common error handling mappings.
 */
@SuppressWarnings("PMD.TooManyMethods")
@ControllerAdvice
public class GlobalErrorHandling extends AbstractErrorHandling {
  private static final Map<String, String> CONSTRAINT_MAP = new HashMap<>();
  private static final Map<String, String> SQL_STATES = new HashMap<>();

  static {
    CONSTRAINT_MAP.put("requisition_template_name_unique_idx", ERROR_TEMPLATE_NAME_DUPLICATION);
    CONSTRAINT_MAP.put("req_tmpl_asgmt_prog_fac_type_tmpl_unique_idx", ERROR_TEMPLATE_ASSIGNMENT);
    CONSTRAINT_MAP.put("req_tmpl_asgmt_prog_tmpl_unique_idx", ERROR_TEMPLATE_ASSIGNMENT);
    CONSTRAINT_MAP.put(
        "req_tmpl_asgmt_prog_fac_type_unique_idx", ERROR_PROGRAM_FACILITY_TYPE_ASSIGNMENT_EXISTS
    );
    CONSTRAINT_MAP.put("req_prod_fac_per", ERROR_REQUISITION_DUPLICATION);
    CONSTRAINT_MAP.put("req_prod_fac_per_node", ERROR_REQUISITION_DUPLICATION_WITH_NODE);

    // https://www.postgresql.org/docs/9.6/static/errcodes-appendix.html
    SQL_STATES.put("23505", ERROR_DUPLICATE_STATUS_CHANGE);
  }

  /**
   * Handles data integrity violation exception.
   * @param dive the data integrity exception
   * @return the user-oriented error message.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleDataIntegrityViolation(
      DataIntegrityViolationException dive) {
    logger.info(dive.getMessage());

    if (dive.getCause() instanceof ConstraintViolationException) {
      ConstraintViolationException cause = (ConstraintViolationException) dive.getCause();
      String messageKey = CONSTRAINT_MAP.get(cause.getConstraintName());
      if (messageKey != null) {
        return getLocalizedMessage(new Message(messageKey));
      }
    }

    return getLocalizedMessage(dive.getMessage());
  }

  /**
   * Handles Jpa System Exception.
   * @param exp the Jpa System Exception
   * @return the user-oriented error message.
   */
  @ExceptionHandler(JpaSystemException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleJpaSystemException(JpaSystemException exp) {
    logger.info(exp.getMessage());

    if (exp.getCause() instanceof PersistenceException) {
      PersistenceException persistence = (PersistenceException) exp.getCause();

      if (persistence.getCause() instanceof SQLException) {
        SQLException sql = (SQLException) persistence.getCause();
        String message = SQL_STATES.get(sql.getSQLState());

        if (null != message) {
          return getLocalizedMessage(message);
        }
      }
    }

    return getLocalizedMessage(exp.getMessage());
  }

  /**
   * Handles Message exceptions and returns status 500.
   *
   * @param ex the DataRetrievalException to handle
   * @return the error response for the user
   */
  @ExceptionHandler(DataRetrievalException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleDataRetrievalException(DataRetrievalException ex) {
    logger.error(String.format("Unable to retrieve %s. Error code: %d, response message: %s",
        ex.getResource(), ex.getStatus().value(), ex.getResponse()));
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler(ServerException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleServerException(ServerException ex) {
    logger.error("An internal error occurred", ex);
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler(ExternalApiException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public LocalizedMessageDto handleServerException(ExternalApiException ex) {
    logger.error("An external api error occurred", ex);
    return ex.getMessageLocalized();
  }

  /**
   * Handles Message exceptions and returns status 400 Bad Request.
   *
   * @param ex the ValidationMessageException to handle
   * @return the error response for the user
   */
  @ExceptionHandler(ValidationMessageException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleMessageException(ValidationMessageException ex) {
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler(AuthenticationMessageException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ResponseBody
  public Message.LocalizedMessage handleAuthenticationException(AuthenticationMessageException ex) {
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler(PermissionMessageException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public Message.LocalizedMessage handlePermissionException(PermissionMessageException ex) {
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler(VersionMismatchException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  @ResponseBody
  public Message.LocalizedMessage handlePermissionException(VersionMismatchException ex) {
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler(IdempotencyKeyException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  @ResponseBody
  public Message.LocalizedMessage handleIdempotencyException(IdempotencyKeyException ex) {
    return getLocalizedMessage(ex);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  @ResponseBody
  public Message.LocalizedMessage handleOptimisticLockingFailure(
      ObjectOptimisticLockingFailureException ex) {
    logger.info("Optimistic locking violation occured.", ex);
    return getLocalizedMessage(new Message(VERSION_MISMATCH));
  }
}
