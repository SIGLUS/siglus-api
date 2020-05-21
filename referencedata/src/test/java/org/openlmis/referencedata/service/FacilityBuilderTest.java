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

package org.openlmis.referencedata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupportedProgram;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.FacilityOperatorDto;
import org.openlmis.referencedata.dto.FacilityTypeDto;
import org.openlmis.referencedata.dto.GeographicZoneSimpleDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityOperatorRepository;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.FacilityOperatorDataBuilder;
import org.openlmis.referencedata.testbuilder.FacilityTypeDataBuilder;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.util.messagekeys.FacilityOperatorMessageKeys;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FacilityBuilderTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private GeographicZoneRepository geographicZoneRepository;

  @Mock
  private FacilityTypeRepository facilityTypeRepository;

  @Mock
  private FacilityOperatorRepository facilityOperatorRepository;

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private FacilityRepository facilityRepository;

  @InjectMocks
  private FacilityBuilder builder;

  private GeographicZone geographicZone = new GeographicZoneDataBuilder().build();
  private FacilityType facilityType = new FacilityTypeDataBuilder().build();
  private FacilityOperator facilityOperator = new FacilityOperatorDataBuilder().build();
  private Program program = new ProgramDataBuilder().build();
  private Facility facility = new FacilityDataBuilder()
      .withGeographicZone(geographicZone)
      .withType(facilityType)
      .withOperator(facilityOperator)
      .withSupportedProgram(program)
      .build();

  private FacilityDto importer = new FacilityDto();

  @Before
  public void setUp() {
    facility.export(importer);
    importer.setId(null);

    when(geographicZoneRepository.findOne(geographicZone.getId()))
        .thenReturn(geographicZone);
    when(facilityOperatorRepository.findOne(facilityOperator.getId()))
        .thenReturn(facilityOperator);
    when(facilityTypeRepository.findOne(facilityType.getId()))
        .thenReturn(facilityType);
    when(programRepository.findByCode(any(Code.class)))
        .thenReturn(program);
    when(programRepository.findOne(program.getId()))
        .thenReturn(program);
  }

  @Test
  public void shouldBuildDomainObjectBasedOnDataFromImporter() {
    Facility built = builder.build(importer);

    assertThat(built)
        .isEqualToIgnoringGivenFields(importer,
            "geographicZone", "type", "operator", "supportedPrograms")
        .hasFieldOrPropertyWithValue("geographicZone", geographicZone)
        .hasFieldOrPropertyWithValue("type", facilityType)
        .hasFieldOrPropertyWithValue("operator", facilityOperator);

    assertThat(built.getSupportedPrograms())
        .hasSize(1);

    SupportedProgram supportedProgram = built.getSupportedPrograms().iterator().next();

    assertThat(supportedProgram.getFacilityProgram())
        .hasFieldOrPropertyWithValue("program", program)
        .hasFieldOrPropertyWithValue("facility", built);
  }

  @Test
  public void shouldThrowExceptionIfGeographicZoneCouldNotBeFound() {
    when(geographicZoneRepository.findOne(geographicZone.getId()))
        .thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicZoneMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityTypeCouldNotBeFound() {
    when(facilityTypeRepository.findOne(facilityType.getId()))
        .thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityOperatorCouldNotBeFound() {
    when(facilityOperatorRepository.findOne(facilityOperator.getId())).thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityOperatorMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfGeographicZoneIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicZoneMessageKeys.ERROR_NOT_FOUND);

    importer.getGeographicZone().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityTypeIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeMessageKeys.ERROR_NOT_FOUND);

    importer.getType().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityOperatorIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityOperatorMessageKeys.ERROR_NOT_FOUND);

    importer.getOperator().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfGeographicZoneImporterDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicZoneMessageKeys.ERROR_NOT_FOUND);

    importer.setGeographicZone((GeographicZoneSimpleDto) null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityTypeImporterDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeMessageKeys.ERROR_NOT_FOUND);

    importer.setType((FacilityTypeDto) null);
    builder.build(importer);
  }

  @Test
  public void shouldNotThrowExceptionIfFacilityOperatorImporterWasNotSet() {
    importer.setOperator((FacilityOperatorDto) null);

    Facility built = builder.build(importer);

    assertThat(built.getOperator()).isNull();
  }

  @Test
  public void shouldFindProgramByIdIfProgramCodeItNotSet() {
    importer.getSupportedPrograms().forEach(elem -> {
      ReflectionTestUtils.setField(elem, "code", null);
    });

    Facility built = builder.build(importer);
    assertThat(built.getSupportedPrograms().iterator().next().getFacilityProgram().getProgram())
        .isEqualTo(program);
  }

  @Test
  public void shouldThrowExceptionIfProgramCodeAndProgramIdAreNotSet() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ProgramMessageKeys.ERROR_CODE_OR_ID_REQUIRED);

    importer.getSupportedPrograms().forEach(elem -> {
      elem.setId(null);
      ReflectionTestUtils.setField(elem, "code", null);
    });

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfProgramNotFound() {
    when(programRepository.findByCode(any(Code.class))).thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ProgramMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldHandleEmptySupportedProgramSet() {
    ReflectionTestUtils.setField(importer, "supportedPrograms", null);
    Facility built = builder.build(importer);

    assertThat(built.getSupportedPrograms()).hasSize(0);
  }

  @Test
  public void shouldHandleNullValueAsSupportedProgramSet() {
    ReflectionTestUtils.setField(importer, "supportedPrograms", Sets.newHashSet());
    Facility built = builder.build(importer);

    assertThat(built.getSupportedPrograms()).hasSize(0);
  }

  @Test
  public void shouldUseInstanceFromDatabaseIfImporterHasIdSet() {
    importer.setId(UUID.randomUUID());
    when(facilityRepository.findOne(importer.getId()))
        .thenReturn(facility);

    Facility built = builder.build(importer);
    assertThat(built.getId()).isEqualTo(facility.getId());
  }
}
