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

import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.dto.GeographicZoneDto;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class GeographicZoneValidator
    extends FhirLocationValidator<GeographicZoneDto, GeographicZone> {

  static final String CODE = "code";
  static final String NAME = "name";
  static final String LEVEL = "level";
  static final String PARENT = "parent";
  static final String LATITUDE = "latitude";
  static final String LONGITUDE = "longitude";

  @Autowired
  private GeographicZoneRepository geographicZoneRepository;

  public GeographicZoneValidator() {
    super(GeographicZoneDto.class);
  }

  @Override
  void verifyResource(GeographicZoneDto target, Errors errors) {
    rejectIfEmptyOrWhitespace(errors, CODE, GeographicZoneMessageKeys.ERROR_CODE_REQUIRED);

    if (null == target.getLevel()) {
      rejectValue(errors, LEVEL, GeographicZoneMessageKeys.ERROR_LEVEL_REQUIRED);
    }
  }

  @Override
  GeographicZone getExistingResource(GeographicZoneDto target) {
    return geographicZoneRepository.findOne(target.getId());
  }

  @Override
  void verifyInvariants(GeographicZoneDto target, GeographicZone existing, Errors errors) {
    rejectIfInvariantWasChanged(errors, CODE, existing.getCode(), target.getCode());
    rejectIfInvariantWasChanged(errors, NAME, existing.getName(), target.getName());

    if (null != target.getParent() && null != existing.getParent()) {
      rejectIfInvariantWasChanged(errors, PARENT,
          existing.getParent().getId(), target.getParent().getId());
    } else if ((null == target.getParent() && null != existing.getParent())
        || (null != target.getParent() && null == existing.getParent())) {
      rejectValue(errors, PARENT, getInvariantFieldErrorMessage());
    }

    rejectIfInvariantWasChanged(errors, LATITUDE, existing.getLatitude(), target.getLatitude());
    rejectIfInvariantWasChanged(errors, LONGITUDE, existing.getLongitude(), target.getLongitude());
  }

  @Override
  String getUnallowedKeyErrorMessage() {
    return GeographicZoneMessageKeys.ERROR_EXTRA_DATA_UNALLOWED_KEY;
  }

  @Override
  String getModifiedKeyErrorMessage() {
    return GeographicZoneMessageKeys.ERROR_EXTRA_DATA_MODIFIED_KEY;
  }

  @Override
  String getInvariantFieldErrorMessage() {
    return GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT;
  }
}
