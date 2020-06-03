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

package org.openlmis.fulfillment.web.errorhandler;

import static org.openlmis.fulfillment.i18n.MessageKeys.DATA_INTEGRITY_VIOLATION;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_JASPER_REPORT_CREATION_WITH_MESSAGE;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_REFERENCE_DATA_RETRIEVE;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_DRAT_ORDER_DUPLICATE;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_ORDER_DUPLICATE;
import static org.openlmis.fulfillment.i18n.MessageKeys.TRANSFER_PROPERTIES_DUPLICATE;
import static org.springframework.http.HttpStatus.CONFLICT;

import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import org.hibernate.exception.ConstraintViolationException;
import org.openlmis.fulfillment.i18n.MessageKeys;
import org.openlmis.fulfillment.service.DataRetrievalException;
import org.openlmis.fulfillment.service.DuplicateTransferPropertiesException;
import org.openlmis.fulfillment.service.ExternalApiException;
import org.openlmis.fulfillment.service.IncorrectTransferPropertiesException;
import org.openlmis.fulfillment.service.OrderFileException;
import org.openlmis.fulfillment.service.OrderStorageException;
import org.openlmis.fulfillment.service.ReportingException;
import org.openlmis.fulfillment.util.Message;
import org.openlmis.fulfillment.web.ServerException;
import org.openlmis.fulfillment.web.util.LocalizedMessageDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller advice responsible for handling errors from service layer.
 */
@ControllerAdvice
public class ServiceErrorHandling extends AbstractErrorHandling {

  private static final Map<String, String> CONSTRAINT_MAP = new HashMap<>();
  private static final String CONSTRAINT_VIOLATION = "Constraint violation";

  static {
    CONSTRAINT_MAP.put("shipments_order_unq", SHIPMENT_ORDER_DUPLICATE);
    CONSTRAINT_MAP.put("shipment_drafts_orderid_unq", SHIPMENT_DRAT_ORDER_DUPLICATE);
    CONSTRAINT_MAP.put("transfer_properties_facility_id_unique", TRANSFER_PROPERTIES_DUPLICATE);
  }

  @ExceptionHandler(OrderFileException.class)
  public Message.LocalizedMessage handleOrderFileGenerationError(OrderFileException ex) {
    return logErrorAndRespond("Unable to generate the order file", ex);
  }

  @ExceptionHandler(ReportingException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handlerReportingException(ReportingException ex) {
    return logErrorAndRespond("Reporting error", ex);
  }

  /**
   * Handles data integrity violation exception.
   *
   * @param ex the data integrity exception
   * @return the user-oriented error message.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Message.LocalizedMessage> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    if (ex.getCause() instanceof ConstraintViolationException) {
      ConstraintViolationException cause = (ConstraintViolationException) ex.getCause();
      String messageKey = CONSTRAINT_MAP.get(cause.getConstraintName());
      if (messageKey != null) {
        logger.error(CONSTRAINT_VIOLATION, ex);
        return new ResponseEntity<>(getLocalizedMessage(
            new Message(messageKey)), HttpStatus.BAD_REQUEST);
      } else {
        return new ResponseEntity<>(logErrorAndRespond(CONSTRAINT_VIOLATION,
            MessageKeys.CONSTRAINT_VIOLATION, ex.getMessage()), HttpStatus.BAD_REQUEST);
      }
    }

    return new ResponseEntity<>(logErrorAndRespond("Data integrity violation",
        DATA_INTEGRITY_VIOLATION, ex.getMessage()), CONFLICT);
  }

  /**
   * Handles the {@link DataRetrievalException} which we were unable to retrieve
   * reference data due to a communication error.
   *
   * @param ex the exception that caused the issue
   * @return the error response
   */
  @ExceptionHandler(DataRetrievalException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleRefDataException(DataRetrievalException ex) {
    return logErrorAndRespond(
        "Error fetching from reference data",
        ERROR_REFERENCE_DATA_RETRIEVE, ex.getResource(), ex.getStatus().toString(), ex.getResponse()
    );
  }

  /**
   * Handles the {@link ServerException} that indicates server error.
   *
   * @param ex the exception that caused the issue
   * @return the error response
   */
  @ExceptionHandler(ServerException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleServerException(ServerException ex) {
    logger.error("An internal error occurred", ex);
    return getLocalizedMessage(ex.getErrorMessage());
  }

  @ExceptionHandler(OrderStorageException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleOrderStorageException(OrderStorageException ex) {
    return logErrorAndRespond("Unable to store the order", ex);
  }

  @ExceptionHandler(DuplicateTransferPropertiesException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleDuplicateTransferPropertiesException(
      DuplicateTransferPropertiesException ex) {
    return logErrorAndRespond("Duplicate facility transfer properties", ex);
  }

  @ExceptionHandler(IncorrectTransferPropertiesException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleIncorrectTransferPropertiesException(
      IncorrectTransferPropertiesException ex) {
    return logErrorAndRespond("Incorrect facility transfer properties", ex);
  }

  /**
   * Handles the {@link JRException} which may be thrown during Jasper report generation.
   *
   * @param err exception that caused the issue
   * @return error response
   */
  @ExceptionHandler(JRException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public Message.LocalizedMessage handleJrRuntimeException(JRException err) {
    return logErrorAndRespond(
        "Error during Jasper Report generation",
        ERROR_JASPER_REPORT_CREATION_WITH_MESSAGE,
        err.getMessage()
    );
  }

  @ExceptionHandler(ExternalApiException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public LocalizedMessageDto handleExternalApiException(ExternalApiException ex) {
    logger.error("An external api error occurred", ex);
    return ex.getMessageLocalized();
  }



}
