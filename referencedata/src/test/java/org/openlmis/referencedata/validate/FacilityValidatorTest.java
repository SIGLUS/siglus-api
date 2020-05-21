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
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.validate.FacilityValidator.ACTIVE;
import static org.openlmis.referencedata.validate.FacilityValidator.CODE;
import static org.openlmis.referencedata.validate.FacilityValidator.DESCRIPTION;
import static org.openlmis.referencedata.validate.FacilityValidator.GEOGRAPHIC_ZONE;
import static org.openlmis.referencedata.validate.FacilityValidator.LOCATION;
import static org.openlmis.referencedata.validate.FacilityValidator.NAME;
import static org.openlmis.referencedata.validate.FacilityValidator.SUPPORTED_PROGRAMS;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.SupportedProgramDto;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("PMD.TooManyMethods")
public class FacilityValidatorTest extends FhirLocationValidatorTest<FacilityDto, Facility> {

  @Mock
  private FacilityRepository facilityRepository;

  @InjectMocks
  private FacilityValidator validator;

  private Facility facility = new FacilityDataBuilder()
      .withSupportedProgram(new ProgramDataBuilder().build())
      .withIsManagedExternallyFlag()
      .build();
  private FacilityDto facilityDto = new FacilityDto();

  @Override
  FhirLocationValidator<FacilityDto, Facility> getValidator() {
    return validator;
  }

  @Override
  Class<FacilityDto> getDtoDefinition() {
    return FacilityDto.class;
  }

  @Override
  FacilityDto getTarget() {
    return facilityDto;
  }

  @Override
  Facility getExistingResource() {
    return facility;
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();

    facility.export(facilityDto);

    when(facilityRepository.findOne(facility.getId())).thenReturn(facility);
  }

  @Test
  public void shouldNotFindErrorsWhenFacilityIsValid() {
    mockUserRequest();

    validator.validate(facilityDto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldNotFindErrorsWhenFacilityIsValidWithoutSupportedPrograms() {
    mockUserRequest();

    facilityDto.setSupportedPrograms(Sets.newHashSet());
    facility.setSupportedPrograms(Sets.newHashSet());

    validator.validate(facilityDto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectWhenFacilityCodeIsNull() {
    facilityDto.setCode(null);

    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, CODE, FacilityMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectWhenFacilityCodeIsEmpty() {
    facilityDto.setCode("");

    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, CODE, FacilityMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectWhenFacilityCodeIsWhitespace() {
    facilityDto.setCode(" ");

    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, CODE, FacilityMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectIfSupportedProgramCodeIsDuplicated() {
    UUID programId = UUID.randomUUID();

    Program program1 = new ProgramDataBuilder().withId(programId).build();
    Program program2 = new ProgramDataBuilder().withId(programId).build();

    new FacilityDataBuilder()
        .withSupportedProgram(program1)
        .withSupportedProgram(program2)
        .build()
        .export(facilityDto);

    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, SUPPORTED_PROGRAMS,
        FacilityMessageKeys.ERROR_DUPLICATE_PROGRAM_SUPPORTED);
  }

  @Test
  public void shouldRejectIfSupportedProgramIdIsDuplicated() {
    String code = RandomStringUtils.randomAlphanumeric(10);

    Program program1 = new ProgramDataBuilder().build();
    Program program2 = new ProgramDataBuilder().build();

    new FacilityDataBuilder()
        .withSupportedProgram(program1)
        .withSupportedProgram(program2)
        .build()
        .export(facilityDto);

    for (SupportedProgramDto supportedProgram : facilityDto.getSupportedPrograms()) {
      ReflectionTestUtils.setField(supportedProgram, "code", code);
    }

    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, SUPPORTED_PROGRAMS,
        FacilityMessageKeys.ERROR_DUPLICATE_PROGRAM_SUPPORTED);
  }

  @Test
  public void shouldRejectIfCodeWasChangedForFhirResource() {
    facilityDto.setCode(facility.getCode() + "1234");

    mockUserRequest();
    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, CODE, FacilityMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfNameWasChangedForFhirResource() {
    facilityDto.setName(facility.getName() + "1234");

    mockUserRequest();
    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, NAME, FacilityMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfDescriptionWasChangedForFhirResource() {
    facilityDto.setDescription(facility.getDescription() + "1234");

    mockUserRequest();
    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, DESCRIPTION, FacilityMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfGeographicZoneWasChangedForFhirResource() {
    facilityDto.setGeographicZone(new GeographicZoneDataBuilder().build());

    mockUserRequest();
    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, GEOGRAPHIC_ZONE, FacilityMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfActiveWasChangedForFhirResource() {
    facilityDto.setActive(!facility.getActive());

    mockUserRequest();
    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, ACTIVE, FacilityMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfLocationWasChangedForFhirResource() {
    facilityDto.setLocation(new GeometryFactory()
        .createPoint(new Coordinate(facility.getLocation().getX() + 10,
            facility.getLocation().getY() - 10)));

    mockUserRequest();
    validator.validate(facilityDto, errors);

    assertErrorMessage(errors, LOCATION, FacilityMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }
}
