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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.openlmis.referencedata.exception.IntegrityViolationException;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.UnauthorizedException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.LocalizedMessage;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeApprovedProductMessageKeys;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.OrderableMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProcessingScheduleMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.openlmis.referencedata.util.messagekeys.RoleMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupplyLineMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys;
import org.openlmis.referencedata.util.messagekeys.TradeItemMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class RefDataErrorHandling extends BaseHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RefDataErrorHandling.class);

  private static final Map<String, String> CONSTRAINT_MAP = new HashMap<>();
  private static final Map<String, String> SQL_STATES = new HashMap<>();

  static {
    CONSTRAINT_MAP.put("unq_program_code", ProgramMessageKeys.ERROR_CODE_DUPLICATED);
    CONSTRAINT_MAP.put("processing_schedule_name_unique_idx",
        ProcessingScheduleMessageKeys.ERROR_NAME_DUPLICATED);
    CONSTRAINT_MAP.put("processing_schedule_code_unique_idx",
        ProcessingScheduleMessageKeys.ERROR_CODE_DUPLICATED);
    CONSTRAINT_MAP.put("supply_line_unique_program_supervisory_node",
        SupplyLineMessageKeys.ERROR_PROGRAM_SUPERVISORY_NODE_DUPLICATED);
    CONSTRAINT_MAP.put("uk_tradeitems_gtin", TradeItemMessageKeys.ERROR_GTIN_DUPLICATED);
    CONSTRAINT_MAP.put("unq_facility_type_code", FacilityTypeMessageKeys.ERROR_CODE_DUPLICATED);
    CONSTRAINT_MAP.put("unq_facility_code", FacilityMessageKeys.ERROR_CODE_MUST_BE_UNIQUE);
    CONSTRAINT_MAP.put("unq_programid_orderableid_orderableversionnumber",
        OrderableMessageKeys.ERROR_PROGRAMS_DUPLICATED);
    CONSTRAINT_MAP.put("unq_supply_partner_code", SupplyPartnerMessageKeys.ERROR_CODE_DUPLICATED);
    CONSTRAINT_MAP.put("unq_supply_partner_association_programid_supervisorynodeid",
        SupplyPartnerMessageKeys.ERROR_ASSOCIATION_DUPLICATED);
    CONSTRAINT_MAP.put("unq_role_name", RoleMessageKeys.ERROR_MUST_HAVE_A_UNIQUE_NAME);
    CONSTRAINT_MAP.put("unq_ftap", FacilityTypeApprovedProductMessageKeys.ERROR_DUPLICATED);

    // https://www.postgresql.org/docs/9.6/static/errcodes-appendix.html
    SQL_STATES.put("23503", OrderableMessageKeys.ERROR_NOT_FOUND);
  }

  /**
   * Handles data integrity violation and returns status 409 CONFLICT.
   *
   * @param ex the exception to handle
   * @return the error response for the user
   */
  @ExceptionHandler(IntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  @ResponseBody
  public LocalizedMessage handleIntegrityViolationException(IntegrityViolationException ex) {
    LOGGER.error(ex.getMessage());
    return getLocalizedMessage(ex.getMessage());
  }

  /**
   * Handles data integrity violation exception.
   * @param dive the data integrity exception
   * @return the user-oriented error message.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public LocalizedMessage handleDataIntegrityViolation(DataIntegrityViolationException dive) {
    LOGGER.info(dive.getMessage());

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
   * Handles Message exceptions and returns status 400 Bad Request.
   *
   * @param ex the ValidationMessageException to handle
   * @return the error response for the user
   */
  @ExceptionHandler(ValidationMessageException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public LocalizedMessage handleMessageException(ValidationMessageException ex) {
    LOGGER.info(ex.getMessage());
    return getLocalizedMessage(ex.asMessage());
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public LocalizedMessage handleNotFoundException(NotFoundException ex) {
    LOGGER.info(ex.getMessage());
    return getLocalizedMessage(ex.asMessage());
  }

  /**
   * Handles unauthorized exceptions and returns proper response.
   *
   * @param ex Exception to handle.
   * @return ResponseEntity with exception details
   */
  @ExceptionHandler(UnauthorizedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public LocalizedMessage handleUnauthorizedException(UnauthorizedException ex) {
    LOGGER.info(ex.getMessage());
    return getLocalizedMessage(ex.asMessage());
  }

  /**
   * Handles Jpa System Exception.
   * @param exp the Jpa System Exception
   * @return the user-oriented error message.
   */
  @ExceptionHandler(JpaSystemException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public LocalizedMessage handleJpaSystemException(JpaSystemException exp) {
    LOGGER.info(exp.getMessage());

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
}
