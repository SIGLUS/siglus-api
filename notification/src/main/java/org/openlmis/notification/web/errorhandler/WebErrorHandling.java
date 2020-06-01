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

import static org.openlmis.notification.i18n.MessageKeys.ERROR_CONSTRAINT;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_EMAIL_DUPLICATED;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_NOTIFICATION_CHANNEL_DUPLICATED;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_SEND_REQUEST;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_VERIFICATION_EMAIL_DUPLICATED;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.openlmis.notification.i18n.Message;
import org.openlmis.notification.service.ServerException;
import org.openlmis.notification.web.MissingPermissionException;
import org.openlmis.notification.web.NotFoundException;
import org.openlmis.notification.web.ValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Controller advice responsible for handling errors from web layer.
 */
@ControllerAdvice
public class WebErrorHandling extends AbstractErrorHandling {

  static final Map<String, String> CONSTRAINT_MAP = new HashMap<>();

  static {
    CONSTRAINT_MAP.put("unq_contact_details_email", ERROR_EMAIL_DUPLICATED);
    CONSTRAINT_MAP.put(
        "unq_email_verification_tokens_emailaddress", ERROR_VERIFICATION_EMAIL_DUPLICATED);
    CONSTRAINT_MAP.put(
        "unq_notification_messages_notificationid_channel", ERROR_NOTIFICATION_CHANNEL_DUPLICATED);
  }

  /**
   * Handles the {@link HttpStatusCodeException} which signals a problems with sending a request.
   *
   * @return the localized message
   */
  @ExceptionHandler(HttpStatusCodeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleHttpStatusCodeException(HttpStatusCodeException ex) {
    return logErrorAndRespond(
        "Unable to send a request", ex, new Message(
            ERROR_SEND_REQUEST,
            ex.getStatusCode().toString(), ex.getResponseBodyAsString()
        )
    );
  }

  /**
   * Handles the {@link MissingPermissionException} which signals unauthorized access.
   *
   * @return the localized message
   */
  @ExceptionHandler(MissingPermissionException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public Message.LocalizedMessage handleMissingPermissionException(MissingPermissionException ex) {
    return getLocalizedMessage(ex.asMessage());
  }

  /**
   * Handles the {@link ValidationException} which signals a validation problems.
   *
   * @return the localized message
   */
  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleValidationException(ValidationException ex) {
    return getLocalizedMessage(ex.asMessage());
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public Message.LocalizedMessage handleNotFoundException(NotFoundException ex) {
    return getLocalizedMessage(ex.asMessage());
  }

  /**
   * Handles persistence exception.
   *
   * @param ex the persistence exception
   * @return the user-oriented error message.
   */
  @ExceptionHandler(PersistenceException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handlePersistenceException(PersistenceException ex) {
    logger.error(ex.getMessage());
    return getLocalizedMessage(new Message(ERROR_CONSTRAINT));
  }

  /**
   * Handles data integrity violation exception.
   *
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

      if (null != messageKey) {
        return getLocalizedMessage(messageKey);
      }
    }

    return getLocalizedMessage(dive.getMessage());
  }

  @ExceptionHandler(ServerException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleServerException(ServerException ex) {
    logger.error("An internal error occurred", ex);
    return getLocalizedMessage(ex.asMessage());
  }

}
