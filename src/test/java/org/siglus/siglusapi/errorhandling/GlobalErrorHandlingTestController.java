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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.errorhandling.GlobalErrorHandlingMvcTest.ERROR_MESSAGE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_INVENTORY_CONFLICT_SUB_DRAFT;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import org.hibernate.exception.ConstraintViolationException;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProductSubDraftConflictDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
class GlobalErrorHandlingTestController {

  @GetMapping("/validation-message")
  public void validationMessage() {
    String messageKey = "key";
    throw new ValidationMessageException(messageKey);
  }

  @GetMapping("/data-integrity")
  public void dataIntegrity() {
    String constraintName = "unq_programid_additionalorderableid";
    ConstraintViolationException constraintViolation = new ConstraintViolationException(null, null, constraintName);
    throw new DataIntegrityViolationException(null, constraintViolation);
  }

  @GetMapping("/data-integrity-with-non-existed-key")
  public void dataIntegrityWithNonExistedKeyCause() {
    String constraintName = "not_existed";
    ConstraintViolationException constraintViolation = new ConstraintViolationException(null, null, constraintName);
    throw new DataIntegrityViolationException("error: not_existed", constraintViolation);
  }

  @GetMapping("/data-integrity-without-cause")
  public void dataIntegrityWithoutCause() {
    throw new DataIntegrityViolationException(ERROR_MESSAGE, null);
  }

  @GetMapping("/constraint-violation")
  public void constraintViolation() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    violations.add(violation);
    String messageText = "text|text|text|text";
    when(violation.getMessage()).thenReturn(messageText);
    String propertyPathText = "propertyPath";
    Path mockedPath = mock(Path.class);
    when(mockedPath.toString()).thenReturn(propertyPathText);
    when(violation.getPropertyPath()).thenReturn(mockedPath);
    throw new javax.validation.ConstraintViolationException(violations);
  }

  @GetMapping("/business-error")
  public void businessError() {
    throw new BusinessDataException(new Message(ERROR_INVENTORY_CONFLICT_SUB_DRAFT),
        Lists.newArrayList(ProductSubDraftConflictDto.builder()
            .conflictWith("Draft 1")
            .conflictWithSubDraftId(UUID.randomUUID())
            .orderableId(UUID.randomUUID())
            .productCode("code")
            .productName("name")
            .build(), ProductSubDraftConflictDto.builder()
            .conflictWith("Draft 2")
            .conflictWithSubDraftId(UUID.randomUUID())
            .orderableId(UUID.randomUUID())
            .productCode("code2")
            .productName("name2")
            .build()));
  }

}