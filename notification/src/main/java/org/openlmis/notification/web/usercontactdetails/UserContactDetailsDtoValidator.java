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

package org.openlmis.notification.web.usercontactdetails;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.apache.commons.validator.routines.EmailValidator;
import org.openlmis.notification.domain.EmailVerificationToken;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.i18n.MessageKeys;
import org.openlmis.notification.repository.EmailVerificationTokenRepository;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.openlmis.notification.service.referencedata.UserDto;
import org.openlmis.notification.service.referencedata.UserReferenceDataService;
import org.openlmis.notification.web.BaseValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * A validator for {@link UserContactDetailsDto} object.
 */
@Component
public class UserContactDetailsDtoValidator implements BaseValidator {

  @Autowired
  private UserContactDetailsRepository repository;

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private EmailVerificationTokenRepository emailVerificationTokenRepository;

  static final String REFERENCE_DATA_USER_ID = "referenceDataUserId";
  static final String EMAIL = "emailDetails.email";
  static final String EMAIL_VERIFIED = "emailDetails.emailVerified";

  /**
   * Checks if the given class definition is supported.
   *
   * @param clazz the {@link Class} that this {@link Validator} is being asked if it can {@link
   * #validate(Object, Errors) validate}
   * @return true if {@code clazz} is equal to {@link UserContactDetailsDto}. Otherwise false.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return UserContactDetailsDto.class.equals(clazz);
  }

  /**
   * Validates the {@code target} object, which must be an instance of
   * {@link UserContactDetailsDto} class.
   *
   * @param target the object that is to be validated (never {@code null})
   * @param errors contextual state about the validation process (never {@code null})
   * @see ValidationUtils
   */
  @Override
  public void validate(Object target, Errors errors) {
    UserContactDetailsDto dto = (UserContactDetailsDto) target;

    verifyEmail(dto, errors);
    verifyReferenceDataUserId(dto, errors);
    verifyInvariants(dto, errors);
  }

  private void verifyEmail(UserContactDetailsDto dto, Errors errors) {
    EmailDetailsDto emailDetails = dto.getEmailDetails();
    if (null == emailDetails || isBlank(emailDetails.getEmail())) {
      return;
    }

    if (!EmailValidator.getInstance().isValid(emailDetails.getEmail())) {
      rejectValue(errors, EMAIL, MessageKeys.ERROR_EMAIL_INVALID);
    }

    if (isEmailDuplicated(dto)) {
      rejectValue(errors, EMAIL, MessageKeys.ERROR_EMAIL_DUPLICATED);
    }
  }

  private boolean isEmailDuplicated(UserContactDetailsDto dto) {
    EmailDetailsDto emailDetails = dto.getEmailDetails();
    EmailVerificationToken existingToken = emailVerificationTokenRepository
        .findOneByEmailAddress(emailDetails.getEmail());

    if (null != existingToken && !existingToken.getUserContactDetails().getReferenceDataUserId()
        .equals(dto.getReferenceDataUserId())) {
      return true;
    }

    UserContactDetails existingContactDetails = repository
        .findOneByEmailAddress(emailDetails.getEmail());

    return null != existingContactDetails
        && !existingContactDetails.getReferenceDataUserId().equals(dto.getReferenceDataUserId());

  }

  private void verifyReferenceDataUserId(UserContactDetailsDto contactDetails, Errors errors) {
    UserDto referenceDataUser = userReferenceDataService
        .findOne(contactDetails.getReferenceDataUserId());

    if (null == referenceDataUser) {
      rejectValue(errors, REFERENCE_DATA_USER_ID, MessageKeys.ERROR_USER_NOT_FOUND);
    }
  }

  private void verifyInvariants(UserContactDetailsDto contactDetails, Errors errors) {
    UserContactDetails existing = repository.findOne(contactDetails.getReferenceDataUserId());

    if (null == existing) {
      return;
    }

    if (null != contactDetails.getEmailDetails()) {
      rejectIfInvariantWasChanged(
          errors, EMAIL_VERIFIED,
          existing.getEmailDetails().getEmailVerified(),
          contactDetails.getEmailDetails().getEmailVerified()
      );
    }
  }

  private void rejectIfInvariantWasChanged(Errors errors, String field, Object oldValue,
      Object newValue) {
    rejectIfNotEqual(errors, oldValue, newValue, field, MessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

}
