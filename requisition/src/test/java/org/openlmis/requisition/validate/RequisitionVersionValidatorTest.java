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

package org.openlmis.requisition.validate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.errorhandling.FailureType;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.springframework.http.HttpHeaders;

@RunWith(MockitoJUnitRunner.class)
public class RequisitionVersionValidatorTest {

  private RequisitionVersionValidator requisitionVersionValidator
      = new RequisitionVersionValidator();

  @Mock
  private Requisition incomingReq;

  @Mock
  private Requisition existingReq;

  @Mock
  private HttpServletRequest request;

  @Test
  public void shouldAcceptCorrectModifiedDate() {
    ZonedDateTime dateModified = ZonedDateTime.now();

    ValidationResult result =
        testModifiedDateValidation(dateModified, ZonedDateTime.from(dateModified));
    assertTrue(result.isSuccess());
  }

  @Test
  public void shouldAcceptWithNoModifiedDate() {
    ZonedDateTime dateModified = ZonedDateTime.now();

    ValidationResult result = testModifiedDateValidation(null, dateModified);
    assertTrue(result.isSuccess());
  }

  @Test
  public void shouldRejectFutureModifiedDate() {
    ZonedDateTime dateModified = ZonedDateTime.now();
    ZonedDateTime incomingDate = dateModified.plusYears(1);

    ValidationResult result = testModifiedDateValidation(incomingDate, dateModified);
    assertTrue(result.hasErrors());
    assertEquals(FailureType.CONFLICT, result.getError().getType());
  }

  @Test
  public void shouldRejectPastModifiedDate() {
    ZonedDateTime dateModified = ZonedDateTime.now();
    ZonedDateTime incomingDate = dateModified.minusMinutes(5);

    ValidationResult result = testModifiedDateValidation(incomingDate, dateModified);
    assertTrue(result.hasErrors());
    assertEquals(FailureType.CONFLICT, result.getError().getType());
  }

  @Test
  public void shouldPassValidationIfHeaderIsNotSet() {
    ValidationResult result = testVersionValidation(null, null);

    assertTrue(result.isSuccess());
  }

  @Test
  public void shouldPassValidationIfVersionsAreTheSame() {
    ValidationResult result = testVersionValidation("W/7", 7L);

    assertTrue(result.isSuccess());
  }

  @Test
  public void shouldFailValidationIfVersionsAreDifferent() {
    ValidationResult result = testVersionValidation("W/5", 7L);

    assertTrue(result.hasErrors());
    assertEquals(FailureType.CONFLICT, result.getError().getType());
  }

  private ValidationResult testVersionValidation(String etagVersion, Long requisitionVersion) {
    when(request.getHeader(HttpHeaders.IF_MATCH)).thenReturn(etagVersion);
    when(existingReq.getVersion()).thenReturn(requisitionVersion);

    return requisitionVersionValidator.validateEtagVersionIfPresent(request, existingReq);
  }

  private ValidationResult testModifiedDateValidation(ZonedDateTime incomingDate, ZonedDateTime
      databaseDate) {
    when(incomingReq.getModifiedDate()).thenReturn(incomingDate);
    when(existingReq.getModifiedDate()).thenReturn(databaseDate);
    when(existingReq.getId()).thenReturn(UUID.randomUUID());
    return requisitionVersionValidator.validateRequisitionTimestamps(
        incomingReq.getModifiedDate(), existingReq);
  }
}
