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
import static org.openlmis.referencedata.validate.GeographicZoneValidator.CODE;
import static org.openlmis.referencedata.validate.GeographicZoneValidator.LATITUDE;
import static org.openlmis.referencedata.validate.GeographicZoneValidator.LEVEL;
import static org.openlmis.referencedata.validate.GeographicZoneValidator.LONGITUDE;
import static org.openlmis.referencedata.validate.GeographicZoneValidator.NAME;
import static org.openlmis.referencedata.validate.GeographicZoneValidator.PARENT;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.dto.GeographicLevelDto;
import org.openlmis.referencedata.dto.GeographicZoneDto;
import org.openlmis.referencedata.dto.GeographicZoneSimpleDto;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;

@SuppressWarnings("PMD.TooManyMethods")
public class GeographicZoneValidatorTest
    extends FhirLocationValidatorTest<GeographicZoneDto, GeographicZone> {

  @Mock
  private GeographicZoneRepository geographicZoneRepository;

  @InjectMocks
  private GeographicZoneValidator validator;

  private GeographicZone geographicZone = new GeographicZoneDataBuilder()
      .withParent(new GeographicZoneDataBuilder().build())
      .withIsManagedExternallyFlag()
      .build();
  private GeographicZoneDto geographicZoneDto = new GeographicZoneDto();

  @Override
  FhirLocationValidator<GeographicZoneDto, GeographicZone> getValidator() {
    return validator;
  }

  @Override
  Class<GeographicZoneDto> getDtoDefinition() {
    return GeographicZoneDto.class;
  }

  @Override
  GeographicZoneDto getTarget() {
    return geographicZoneDto;
  }

  @Override
  GeographicZone getExistingResource() {
    return geographicZone;
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();

    geographicZone.export(geographicZoneDto);

    when(geographicZoneRepository.findOne(geographicZone.getId())).thenReturn(geographicZone);
  }

  @Test
  public void shouldNotFindErrorsWhenGeoZoneIsValid() {
    mockUserRequest();

    validator.validate(geographicZoneDto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldNotFindErrorsWhenGeoZoneIsValidWithoutParent() {
    mockUserRequest();

    geographicZoneDto.setParent((GeographicZoneSimpleDto) null);
    geographicZone.setParent(null);

    validator.validate(geographicZoneDto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectIfCodeIsNull() {
    geographicZoneDto.setCode(null);

    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, CODE, GeographicZoneMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectIfCodeIsEmpty() {
    geographicZoneDto.setCode("");

    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, CODE, GeographicZoneMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectIfCodeIsWhitespace() {
    geographicZoneDto.setCode("    ");

    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, CODE, GeographicZoneMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectIfLevelIsMissing() {
    geographicZoneDto.setLevel((GeographicLevelDto) null);

    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, LEVEL, GeographicZoneMessageKeys.ERROR_LEVEL_REQUIRED);
  }

  @Test
  public void shouldRejectIfCodeWasChangedForFhirResource() {
    geographicZoneDto.setCode(geographicZone.getCode() + "1234");

    mockUserRequest();
    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, CODE, GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfNameWasChangedForFhirResource() {
    geographicZoneDto.setName(geographicZone.getName() + "1234");

    mockUserRequest();
    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, NAME, GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfParentWasChangedForFhirResource() {
    geographicZoneDto.setParent(new GeographicZoneDataBuilder().build());

    mockUserRequest();
    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, PARENT, GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfParentWasSetToNullForFhirResource() {
    geographicZoneDto.setParent((GeographicZoneSimpleDto) null);

    mockUserRequest();
    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, PARENT, GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfParentWasSetAndExistingIsNullForFhirResource() {
    geographicZone.setParent(null);

    mockUserRequest();
    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, PARENT, GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfLatitudeWasChangedForFhirResource() {
    geographicZoneDto.setLatitude(geographicZone.getLatitude() + 10);

    mockUserRequest();
    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, LATITUDE, GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }

  @Test
  public void shouldRejectIfLongitudeWasChangedForFhirResource() {
    geographicZoneDto.setLongitude(geographicZone.getLongitude() - 10);

    mockUserRequest();
    validator.validate(geographicZoneDto, errors);

    assertErrorMessage(errors, LONGITUDE, GeographicZoneMessageKeys.ERROR_FIELD_IS_INVARIANT);
  }
}
