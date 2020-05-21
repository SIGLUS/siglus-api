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

package org.openlmis.referencedata.validate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.SystemNotification;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.dto.SystemNotificationDto;
import org.openlmis.referencedata.dto.UserObjectReferenceDto;
import org.openlmis.referencedata.repository.SystemNotificationRepository;
import org.openlmis.referencedata.testbuilder.SystemNotificationDataBuilder;
import org.openlmis.referencedata.testbuilder.UserDataBuilder;
import org.openlmis.referencedata.util.messagekeys.SystemNotificationMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ValidationMessageKeys;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class SystemNotificationValidatorTest {

  private static final String AUTHOR = "author";
  private static final String MESSAGE = "message";
  private static final String CREATED_DATE = "createdDate";
  private static final String START_DATE = "startDate";
  private static final String EXPIRY_DATE = "expiryDate";

  @Mock
  private SystemNotificationRepository systemNotificationRepository;

  @InjectMocks
  private Validator validator = new SystemNotificationValidator();
  private SystemNotification systemNotification;
  private SystemNotificationDto systemNotificationDto = new SystemNotificationDto();
  private Errors errors;
  private User author;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    author = new UserDataBuilder().build();
    systemNotification = new SystemNotificationDataBuilder().withAuthor(author).build();
    systemNotification.export(systemNotificationDto);

    errors = new BeanPropertyBindingResult(systemNotificationDto, "systemNotificationDto");
  }

  @Test
  public void shouldRejectSystemNotificationWithEmptyAuthor() {
    systemNotificationDto.setAuthor(new UserObjectReferenceDto());

    validator.validate(systemNotificationDto, errors);

    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertErrorMessage(errors, AUTHOR, SystemNotificationMessageKeys.ERROR_AUTHOR_REQUIRED);
  }

  @Test
  public void shouldRejectSystemNotificationWithEmptyCreatedDate() {
    when(systemNotificationRepository.findOne(systemNotification.getId()))
        .thenReturn(systemNotification);

    systemNotificationDto.setCreatedDate(null);

    validator.validate(systemNotificationDto, errors);

    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertErrorMessage(errors, CREATED_DATE,
        SystemNotificationMessageKeys.ERROR_CREATED_DATE_REQUIRED);
  }

  @Test
  public void shouldRejectSystemNotificationWithExpiryDateBeforeStartDate() {
    systemNotificationDto.setExpiryDate(systemNotification.getStartDate().minusDays(1));

    validator.validate(systemNotificationDto, errors);

    assertTrue(errors.hasErrors());
    assertEquals(2, errors.getErrorCount());
    assertErrorMessage(errors, START_DATE,
        SystemNotificationMessageKeys.ERROR_START_DATE_AFTER_EXPIRY_DATE);
    assertErrorMessage(errors, EXPIRY_DATE,
        SystemNotificationMessageKeys.ERROR_EXPIRY_DATE_BEFORE_START_DATE);
  }

  @Test
  public void shouldRejectSystemNotificationWithEmptyMessage() {
    systemNotificationDto.setMessage(null);

    validator.validate(systemNotificationDto, errors);

    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertErrorMessage(errors, MESSAGE, SystemNotificationMessageKeys.ERROR_MESSAGE_REQUIRED);
  }

  @Test
  public void shouldAcceptIfUpdatingExistingSystemNotification() {
    systemNotification.setMessage("new message");
    systemNotification.setStartDate(ZonedDateTime.now().minusYears(10));
    systemNotification.setExpiryDate(ZonedDateTime.now().plusYears(10));

    when(systemNotificationRepository.findOne(systemNotification.getId()))
        .thenReturn(systemNotification);

    systemNotification.export(systemNotificationDto);
    validator.validate(systemNotificationDto, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  public void shouldRejectSystemNotificationIfInvariantChanged() {
    SystemNotification savedNotification = new SystemNotificationDataBuilder()
        .withAuthor(author)
        .build();

    when(systemNotificationRepository.findOne(systemNotification.getId()))
        .thenReturn(savedNotification);

    systemNotification.setCreatedDate(ZonedDateTime.now().plusDays(10));
    systemNotification.setAuthor(new UserDataBuilder().build());

    systemNotification.export(systemNotificationDto);
    validator.validate(systemNotificationDto, errors);

    assertTrue(errors.hasErrors());
    assertEquals(2, errors.getErrorCount());
    assertErrorMessage(errors, CREATED_DATE, ValidationMessageKeys.ERROR_IS_INVARIANT);
    assertErrorMessage(errors, AUTHOR, ValidationMessageKeys.ERROR_IS_INVARIANT);
  }

  @Test
  public void shouldAcceptValidSystemNotification() {
    validator.validate(systemNotificationDto, errors);

    assertFalse(errors.hasErrors());
  }

}
