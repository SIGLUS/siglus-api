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

import static org.openlmis.requisition.errorhandling.FailureType.CONFLICT;
import static org.openlmis.requisition.errorhandling.FailureType.FIELD_VALIDATION;
import static org.openlmis.requisition.errorhandling.FailureType.NOT_FOUND;
import static org.openlmis.requisition.errorhandling.FailureType.NO_PERMISSION;
import static org.openlmis.requisition.errorhandling.FailureType.VALIDATION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openlmis.requisition.exception.BindingResultException;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.exception.VersionMismatchException;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.web.PermissionMessageException;

@SuppressWarnings("PMD.TooManyMethods")
public class ValidationResult {

  private List<ValidationFailure> errors;

  public ValidationResult() {
    this.errors = new ArrayList<>();
  }

  /**
   * Creates a ValidationResult for a check that failed on validation.
   *
   * @param messageKey a message key to include in the error or exception
   * @param msgParameters parameters for the given message
   * @return a ValidationResult instance containing validation error
   */
  public static ValidationResult failedValidation(String messageKey, Object... msgParameters) {
    ValidationResult result = new ValidationResult();
    result.addError(new Message(messageKey, msgParameters), VALIDATION);
    return result;
  }

  /**
   * Creates a ValidationResult for a check that failed due to a reference to
   * an instance that did not exist.
   *
   * @param messageKey a message key to include in the error or exception
   * @param msgParameters parameters for the given message
   * @return a ValidationResult instance containing not found error
   */
  public static ValidationResult notFound(String messageKey, Object... msgParameters) {
    ValidationResult result = new ValidationResult();
    result.addError(new Message(messageKey, msgParameters), NOT_FOUND);
    return result;
  }

  /**
   * Creates a ValidationResult for a check that failed due to a user not having
   * permissions to access the resource.
   *
   * @param messageKey a message key to include in the error or exception
   * @param msgParameters parameters for the given message
   * @return a ValidationResult instance containing permission error
   */
  public static ValidationResult noPermission(String messageKey, Object... msgParameters) {
    ValidationResult result = new ValidationResult();
    result.addError(new Message(messageKey, msgParameters), NO_PERMISSION);
    return result;
  }

  /**
   * Creates a ValidationResult for a check that failed due to a conflict.
   *
   * @param messageKey a message key to include in the error or exception
   * @param msgParameters parameters for the given message
   * @return a ValidationResult instance containing conflict error
   */
  public static ValidationResult conflict(String messageKey, Object... msgParameters) {
    ValidationResult result = new ValidationResult();
    result.addError(new Message(messageKey, msgParameters), CONFLICT);
    return result;
  }

  /**
   * Creates a ValidationResult for a check that failed for specific entity fields.
   *
   * @param fieldErrors a map containing errors for specific fields
   * @return a ValidationResult instance containing field validation error
   */
  public static ValidationResult fieldErrors(Map<String, Message> fieldErrors) {
    ValidationResult result = new ValidationResult();
    ValidationFailure failure = new ValidationFailure(null, fieldErrors, FIELD_VALIDATION);
    result.errors.add(failure);
    return result;
  }

  /**
   * Creates a ValidationResult for a check that succeeded.

   * @return a ValidationResult instance representing successful validation
   */
  public static ValidationResult success() {
    return new ValidationResult();
  }

  /**
   * Adds a new error to this validation result.
   *
   * @param message a message key to include in the error or exception
   * @param type a type of an error
   */
  public void addError(Message message, FailureType type) {
    this.errors.add(new ValidationFailure(message, new HashMap<>(), type));
  }

  /**
   * Adds all validation errors from another validation to this one.
   * @param validationResult a validation result of another check
   */
  public void addValidationResult(ValidationResult validationResult) {
    this.errors.addAll(validationResult.gerErrors());
  }

  /**
   * Checks if there are any errors in this validation result.
   * @return true if there are any errors; false otherwise
   */
  public boolean hasErrors() {
    return !isSuccess();
  }

  /**
   * Checks if there are no errors in this validation result.
   * @return true if there are no errors; false otherwise
   */
  public boolean isSuccess() {
    return errors.isEmpty();
  }

  /**
   * Retrieves all errors of this validation result.
   * @return a list of errors
   */
  public List<ValidationFailure> gerErrors() {
    return errors;
  }

  /**
   * Retrieves first error from this validation result. It will return null if there are no errors.
   * @return the first error of this validation result
   */
  public ValidationFailure getError() {
    return errors.isEmpty() ? null : errors.get(0);
  }

  /**
   * Throws exception for the first encountered error. The exception class depends on the type of
   * the error.
   */
  public void throwExceptionIfHasErrors() {
    if (hasErrors()) {
      ValidationFailure failure = getError();

      switch (failure.getType()) {
        case VALIDATION:
          throw new ValidationMessageException(failure.getMessage());
        case NOT_FOUND:
          throw new ContentNotFoundMessageException(failure.getMessage());
        case NO_PERMISSION:
          throw new PermissionMessageException(failure.getMessage());
        case CONFLICT:
          throw new VersionMismatchException(failure.getMessage());
        case FIELD_VALIDATION:
          throw new BindingResultException(failure.getFieldErrors());
        default:
      }
    }
  }
}
