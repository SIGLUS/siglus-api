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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Test;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.exception.VersionMismatchException;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.web.PermissionMessageException;

public class ValidationResultTest {

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageException() {
    ValidationResult result = ValidationResult.failedValidation("validationFailed");

    assertEquals(FailureType.VALIDATION, result.getError().getType());
    result.throwExceptionIfHasErrors();
  }

  @Test(expected = ContentNotFoundMessageException.class)
  public void shouldThrowContentNotFoundMesssageException() {
    ValidationResult result = ValidationResult.notFound("notFound");

    assertEquals(FailureType.NOT_FOUND, result.getError().getType());
    result.throwExceptionIfHasErrors();
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldThrowPermissionMessageException() {
    ValidationResult result = ValidationResult.noPermission("noPermission");

    assertEquals(FailureType.NO_PERMISSION, result.getError().getType());
    result.throwExceptionIfHasErrors();
  }

  @Test(expected = VersionMismatchException.class)
  public void shouldThrowVersionMismatchException() {
    ValidationResult result = ValidationResult.conflict("conflict");

    assertEquals(FailureType.CONFLICT, result.getError().getType());
    result.throwExceptionIfHasErrors();
  }

  @Test
  public void shouldNotThrowExceptionIfHasNoErrors() {
    ValidationResult.success().throwExceptionIfHasErrors();
  }

  @Test
  public void shouldAddErrorsWithCorrectType() {
    ValidationResult result = new ValidationResult();
    result.addError(new Message("validationFailed"), FailureType.VALIDATION);
    result.addError(new Message("validationFailedAgain"), FailureType.VALIDATION);
    result.addError(new Message("youDontHaveAccessAnyways"), FailureType.NO_PERMISSION);

    List<ValidationFailure> actual = result.gerErrors();
    assertNotNull(actual);
    assertEquals(3, actual.size());

    assertEquals(new Message("validationFailed"), actual.get(0).getMessage());
    assertEquals(FailureType.VALIDATION, actual.get(0).getType());

    assertEquals(new Message("validationFailedAgain"), actual.get(1).getMessage());
    assertEquals(FailureType.VALIDATION, actual.get(1).getType());

    assertEquals(new Message("youDontHaveAccessAnyways"), actual.get(2).getMessage());
    assertEquals(FailureType.NO_PERMISSION, actual.get(2).getType());
  }
}
