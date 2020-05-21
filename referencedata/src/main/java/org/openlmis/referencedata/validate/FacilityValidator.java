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

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.SupportedProgramDto;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class FacilityValidator extends FhirLocationValidator<FacilityDto, Facility> {

  static final String CODE = "code";
  static final String NAME = "name";
  static final String DESCRIPTION = "description";
  static final String GEOGRAPHIC_ZONE = "geographicZone";
  static final String ACTIVE = "active";
  static final String LOCATION = "location";
  static final String SUPPORTED_PROGRAMS = "supportedPrograms";

  @Autowired
  private FacilityRepository facilityRepository;

  public FacilityValidator() {
    super(FacilityDto.class);
  }

  @Override
  public void validate(Object target, Errors errors) {
    verifyArguments(target, errors, FacilityMessageKeys.ERROR_NULL);
    super.validate(target, errors);
  }

  @Override
  void verifyResource(FacilityDto target, Errors errors) {
    rejectIfEmptyOrWhitespace(errors, CODE, FacilityMessageKeys.ERROR_CODE_REQUIRED);

    if (!isEmpty(target.getSupportedPrograms())) {
      validateDuplicateProgramSupported(target.getSupportedPrograms(), errors);
    }
  }

  @Override
  Facility getExistingResource(FacilityDto target) {
    return facilityRepository.findOne(target.getId());
  }

  @Override
  void verifyInvariants(FacilityDto target, Facility existing, Errors errors) {
    rejectIfInvariantWasChanged(errors, CODE, existing.getCode(), target.getCode());
    rejectIfInvariantWasChanged(errors, NAME, existing.getName(), target.getName());
    rejectIfInvariantWasChanged(errors, DESCRIPTION,
        existing.getDescription(), target.getDescription());
    rejectIfInvariantWasChanged(errors, GEOGRAPHIC_ZONE,
        existing.getGeographicZone().getId(), target.getGeographicZone().getId());
    rejectIfInvariantWasChanged(errors, ACTIVE, existing.getActive(), target.getActive());
    rejectIfInvariantWasChanged(errors, LOCATION, existing.getLocation(), target.getLocation());
  }

  @Override
  String getUnallowedKeyErrorMessage() {
    return FacilityMessageKeys.ERROR_EXTRA_DATA_UNALLOWED_KEY;
  }

  @Override
  String getModifiedKeyErrorMessage() {
    return FacilityMessageKeys.ERROR_EXTRA_DATA_MODIFIED_KEY;
  }

  @Override
  String getInvariantFieldErrorMessage() {
    return FacilityMessageKeys.ERROR_FIELD_IS_INVARIANT;
  }

  private void validateDuplicateProgramSupported(Set<SupportedProgramDto> supportedProgramDtos,
      Errors errors) {
    boolean noDuplicateProgramCode = supportedProgramDtos
        .stream()
        .map(SupportedProgramDto::getCode)
        .filter(Objects::nonNull)
        .allMatch(new HashSet<>()::add);

    boolean noDuplicateProgramId = supportedProgramDtos
        .stream()
        .map(SupportedProgramDto::getId)
        .filter(Objects::nonNull)
        .allMatch(new HashSet<>()::add);

    if (!(noDuplicateProgramCode && noDuplicateProgramId)) {
      rejectValue(errors, SUPPORTED_PROGRAMS,
          FacilityMessageKeys.ERROR_DUPLICATE_PROGRAM_SUPPORTED);
    }
  }
}
