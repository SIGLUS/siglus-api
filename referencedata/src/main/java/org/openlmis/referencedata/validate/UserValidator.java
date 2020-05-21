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

import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.dto.RoleAssignmentDto;
import org.openlmis.referencedata.dto.UserDto;
import org.openlmis.referencedata.repository.RoleAssignmentRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.service.RightService;
import org.openlmis.referencedata.util.messagekeys.UserMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * A validator for {@link UserDto} object.
 */
@Component
public class UserValidator implements BaseValidator {

  // User fields
  static final String USERNAME = "username";
  static final String FIRST_NAME = "firstName";
  static final String LAST_NAME = "lastName";
  static final String JOB_TITLE = "jobTitle";
  static final String TIMEZONE = "timezone";
  static final String HOME_FACILITY_ID = "homeFacilityId";
  static final String ACTIVE = "active";
  static final String EXTRA_DATA = "extraData";
  static final String ROLE_ASSIGNMENTS = "roleAssignments";

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RightService rightService;

  @Autowired
  private RoleAssignmentRepository roleAssignmentRepository;

  /**
   * Checks if the given class definition is supported.
   *
   * @param clazz the {@link Class} that this {@link Validator} is being asked if it can {@link
   * #validate(Object, Errors) validate}
   * @return true if {@code clazz} is equal to {@link UserDto} class definition. Otherwise false.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return UserDto.class.equals(clazz);
  }

  /**
   * Validates the {@code target} object, which must be an instance of {@link UserDto} class.
   *
   * @param target the object that is to be validated (never {@code null})
   * @param errors contextual state about the validation process (never {@code null})
   * @see ValidationUtils
   */
  @Override
  public void validate(Object target, Errors errors) {
    verifyArguments(target, errors, UserMessageKeys.ERROR_NULL);

    rejectEmptyValues(errors);
    if (!errors.hasErrors()) {
      UserDto dto = (UserDto) target;

      if (null != dto.getId() && !rightService.hasRight(RightName.USERS_MANAGE_RIGHT)) {
        validateInvariants(dto, errors);
      }

      verifyUsername(dto.getId(), dto.getUsername(), errors);
    }
  }

  private void rejectEmptyValues(Errors errors) {
    rejectIfEmptyOrWhitespace(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_REQUIRED);
    rejectIfEmptyOrWhitespace(errors, FIRST_NAME, UserMessageKeys.ERROR_FIRSTNAME_REQUIRED);
    rejectIfEmptyOrWhitespace(errors, LAST_NAME, UserMessageKeys.ERROR_LASTNAME_REQUIRED);
  }

  private void verifyUsername(UUID id, String username, Errors errors) {
    // user name cannot contains invalid characters
    if (!username.matches("\\w+")) {
      rejectValue(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_INVALID);
    }

    // user name cannot be duplicated
    User db = userRepository.findOneByUsernameIgnoreCase(username);

    if (null != db && (null == id || !id.equals(db.getId()))) {
      rejectValue(errors, USERNAME, UserMessageKeys.ERROR_USERNAME_DUPLICATED);
    }
  }

  private void validateInvariants(UserDto dto, Errors errors) {
    User db = userRepository.findOne(dto.getId());

    rejectIfInvariantWasChanged(errors, USERNAME, db.getUsername(), dto.getUsername());
    rejectIfInvariantWasChanged(errors, JOB_TITLE, db.getJobTitle(), dto.getJobTitle());
    rejectIfInvariantWasChanged(errors, TIMEZONE, db.getTimezone(), dto.getTimezone());
    rejectIfInvariantWasChanged(errors, HOME_FACILITY_ID,
        db.getHomeFacilityId(), dto.getHomeFacilityId());
    rejectIfInvariantWasChanged(errors, ACTIVE, db.isActive(), dto.isActive());
    rejectIfInvariantWasChanged(errors, EXTRA_DATA, db.getExtraData(), dto.getExtraData());

    Set<RoleAssignmentDto> oldRoleAssignments = roleAssignmentRepository.findByUser(dto.getId());
    Set<RoleAssignmentDto> newRoleAssignments = dto.getRoleAssignments();

    rejectIfInvariantWasChanged(errors, ROLE_ASSIGNMENTS, oldRoleAssignments, newRoleAssignments);
  }

  private void rejectIfInvariantWasChanged(Errors errors, String field, Object oldValue,
      Object newValue) {
    rejectIfNotEqual(errors, oldValue, newValue, field, UserMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }
}
