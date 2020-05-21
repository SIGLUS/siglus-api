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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.validate.UserValidator.ACTIVE;
import static org.openlmis.referencedata.validate.UserValidator.EXTRA_DATA;
import static org.openlmis.referencedata.validate.UserValidator.FIRST_NAME;
import static org.openlmis.referencedata.validate.UserValidator.HOME_FACILITY_ID;
import static org.openlmis.referencedata.validate.UserValidator.JOB_TITLE;
import static org.openlmis.referencedata.validate.UserValidator.LAST_NAME;
import static org.openlmis.referencedata.validate.UserValidator.ROLE_ASSIGNMENTS;
import static org.openlmis.referencedata.validate.UserValidator.TIMEZONE;
import static org.openlmis.referencedata.validate.UserValidator.USERNAME;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.dto.RoleAssignmentDto;
import org.openlmis.referencedata.dto.UserDto;
import org.openlmis.referencedata.repository.RoleAssignmentRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.service.RightService;
import org.openlmis.referencedata.testbuilder.UserDataBuilder;
import org.openlmis.referencedata.util.messagekeys.UserMessageKeys;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class UserValidatorTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RightService rightService;

  @Mock
  private RoleAssignmentRepository roleAssignmentRepository;

  @InjectMocks
  private Validator validator = new UserValidator();

  private User user;
  private UserDto userDto;
  private Errors errors;

  @Before
  public void setUp() throws Exception {
    userDto = new UserDto();

    user = new UserDataBuilder().build();
    user.export(userDto);

    errors = new BeanPropertyBindingResult(userDto, "userDto");

    when(rightService.hasRight(RightName.USERS_MANAGE_RIGHT)).thenReturn(true);
  }

  @Test
  public void shouldNotFindErrorsWhenUserIsValid() throws Exception {
    validator.validate(userDto, errors);

    assertThat(errors.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void shouldRejectIfUsernameIsDuplicatedAndIdNull() {
    userDto.setId(null);
    doReturn(mock(User.class))
        .when(userRepository)
        .findOneByUsernameIgnoreCase(userDto.getUsername());

    validator.validate(userDto, errors);

    assertErrorMessage(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_DUPLICATED);
  }

  @Test
  public void shouldNotRejectIfUsernameIsDuplicatedAndIdsAreSame() {
    User old = new User();
    old.setId(userDto.getId());

    doReturn(old)
        .when(userRepository)
        .findOneByUsernameIgnoreCase(userDto.getUsername());

    validator.validate(userDto, errors);
    assertThat(errors.hasFieldErrors(USERNAME)).isFalse();
  }

  @Test
  public void shouldRejectIfUserNameIsDuplicatedAndIdsAreDifferent() {
    User old = new User();
    old.setId(UUID.randomUUID());

    doReturn(old)
        .when(userRepository)
        .findOneByUsernameIgnoreCase(userDto.getUsername());

    validator.validate(userDto, errors);
    assertErrorMessage(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_DUPLICATED);
  }

  @Test
  public void shouldRejectWhenUsernameIsNull() {
    userDto.setUsername(null);

    validator.validate(userDto, errors);

    assertErrorMessage(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_REQUIRED);
  }

  @Test
  public void shouldRejectWhenUsernameIsEmpty() {
    userDto.setUsername("");

    validator.validate(userDto, errors);

    assertErrorMessage(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_REQUIRED);
  }

  @Test
  public void shouldRejectWhenUsernameContainsWhitespace() {
    userDto.setUsername("user name");

    validator.validate(userDto, errors);

    assertErrorMessage(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_INVALID);
  }

  @Test
  public void shouldRejectWhenFirstNameIsNull() {
    userDto.setFirstName(null);

    validator.validate(userDto, errors);

    assertErrorMessage(errors, FIRST_NAME, UserMessageKeys.ERROR_FIRSTNAME_REQUIRED);
  }

  @Test
  public void shouldRejectWhenFirstNameIsEmpty() {
    userDto.setFirstName("");

    validator.validate(userDto, errors);

    assertErrorMessage(errors, FIRST_NAME, UserMessageKeys.ERROR_FIRSTNAME_REQUIRED);
  }

  @Test
  public void shouldRejectWhenFirstNameIsWhitespace() {
    userDto.setFirstName(" ");

    validator.validate(userDto, errors);

    assertErrorMessage(errors, FIRST_NAME, UserMessageKeys.ERROR_FIRSTNAME_REQUIRED);
  }

  @Test
  public void shouldRejectWhenLastNameIsNull() {
    userDto.setLastName(null);

    validator.validate(userDto, errors);

    assertErrorMessage(errors, LAST_NAME, UserMessageKeys.ERROR_LASTNAME_REQUIRED);
  }

  @Test
  public void shouldRejectWhenLastNameIsEmpty() {
    userDto.setLastName("");

    validator.validate(userDto, errors);

    assertErrorMessage(errors, LAST_NAME, UserMessageKeys.ERROR_LASTNAME_REQUIRED);
  }

  @Test
  public void shouldRejectWhenLastNameIsWhitespace() {
    userDto.setLastName(" ");

    validator.validate(userDto, errors);

    assertErrorMessage(errors, LAST_NAME, UserMessageKeys.ERROR_LASTNAME_REQUIRED);
  }

  @Test
  public void shouldNotRejectIfUserHasNoRightForEditAndFieldsWereNotChanged() {
    prepareForValidateInvariants();

    validator.validate(userDto, errors);

    assertThat(errors.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void shouldNotRejectIfUserHasNoRightForEditAndBasicDetailsWereChanged() {
    prepareForValidateInvariants();

    userDto.setFirstName(RandomStringUtils.randomAlphanumeric(5));
    userDto.setLastName(RandomStringUtils.randomAlphanumeric(5));
    validator.validate(userDto, errors);

    assertThat(errors.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void shouldRejectIfUserHasNoRightForEditAndInvariantsWereChanged() {
    prepareForValidateInvariants();

    userDto.setUsername(RandomStringUtils.randomAlphanumeric(10));
    userDto.setJobTitle("test-job-title");
    userDto.setTimezone("test-time-zone");
    userDto.setHomeFacilityId(UUID.randomUUID());
    userDto.setActive(!userDto.isActive());
    userDto.setExtraData(ImmutableMap.of("a", "b"));
    userDto.setRoleAssignments(Sets.newHashSet(new RoleAssignmentDto()));
    validator.validate(userDto, errors);

    assertThat(errors.getErrorCount()).isGreaterThanOrEqualTo(7);
    assertErrorMessage(errors, USERNAME, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
    assertErrorMessage(errors, JOB_TITLE, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
    assertErrorMessage(errors, TIMEZONE, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
    assertErrorMessage(errors, HOME_FACILITY_ID, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
    assertErrorMessage(errors, ACTIVE, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
    assertErrorMessage(errors, EXTRA_DATA, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
    assertErrorMessage(errors, ROLE_ASSIGNMENTS, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  private void prepareForValidateInvariants() {
    when(rightService.hasRight(RightName.USERS_MANAGE_RIGHT)).thenReturn(false);
    when(userRepository.findOne(userDto.getId())).thenReturn(user);
    when(roleAssignmentRepository.findByUser(userDto.getId()))
        .thenReturn(userDto.getRoleAssignments());
  }
}
