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

import java.util.Objects;
import lombok.AllArgsConstructor;
import org.openlmis.referencedata.domain.FhirLocation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.validation.Errors;

@AllArgsConstructor
public abstract class FhirLocationValidator<D extends FhirLocation, E extends FhirLocation>
    implements BaseValidator {
  public static final String IS_MANAGED_EXTERNALLY = "isManagedExternally";
  static final String EXTRA_DATA = "extraData";

  private Class<D> dtoDefinition;

  @Override
  public boolean supports(Class<?> clazz) {
    return Objects.equals(dtoDefinition, clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    D newResource = dtoDefinition.cast(target);
    verifyResource(newResource, errors);

    if (errors.hasErrors()) {
      return;
    }

    if (null == newResource.getId()) {
      handleCreateEvent(newResource, errors);
    } else {
      E existingResource = getExistingResource(newResource);

      if (null == existingResource) {
        handleCreateEvent(newResource, errors);
      } else {
        handleUpdateEvent(newResource, existingResource, errors);
      }
    }
  }

  private void handleCreateEvent(D newResource, Errors errors) {
    if (newResource.isManagedExternally() && isUserRequest()) {
      rejectValue(errors, EXTRA_DATA, getUnallowedKeyErrorMessage(), IS_MANAGED_EXTERNALLY);
    }
  }

  private void handleUpdateEvent(D newResource, E existingResource, Errors errors) {
    boolean isFlagSetInNewResource = newResource.isManagedExternally();
    boolean isFlagSetInExistingResource = existingResource.isManagedExternally();

    if (!isFlagSetInNewResource && isFlagSetInExistingResource && isUserRequest()) {
      rejectValue(errors, EXTRA_DATA, getModifiedKeyErrorMessage(),
          IS_MANAGED_EXTERNALLY, String.valueOf(isFlagSetInNewResource));
    }

    if (!errors.hasErrors() && isFlagSetInNewResource && isUserRequest()) {
      verifyInvariants(newResource, existingResource, errors);
    }
  }

  abstract void verifyResource(D target, Errors errors);

  abstract E getExistingResource(D target);

  abstract void verifyInvariants(D target, E existing, Errors errors);

  abstract String getUnallowedKeyErrorMessage();

  abstract String getModifiedKeyErrorMessage();

  abstract String getInvariantFieldErrorMessage();

  void rejectIfInvariantWasChanged(Errors errors, String field, Object oldValue, Object newValue) {
    rejectIfNotEqual(errors, oldValue, newValue, field, getInvariantFieldErrorMessage());
  }

  private boolean isUserRequest() {
    OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();

    return !authentication.isClientOnly();
  }
}
