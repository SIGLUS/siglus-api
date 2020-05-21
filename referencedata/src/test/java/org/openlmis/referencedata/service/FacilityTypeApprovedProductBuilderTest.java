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
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.dto.ApprovedProductDto;
import org.openlmis.referencedata.dto.FacilityTypeDto;
import org.openlmis.referencedata.dto.ProgramDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.testbuilder.FacilityTypeApprovedProductsDataBuilder;
import org.openlmis.referencedata.testbuilder.FacilityTypeDataBuilder;
import org.openlmis.referencedata.testbuilder.OrderableDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;

@RunWith(MockitoJUnitRunner.class)
public class FacilityTypeApprovedProductBuilderTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private FacilityTypeRepository facilityTypeRepository;

  @InjectMocks
  private FacilityTypeApprovedProductBuilder builder;

  private Orderable orderable = new OrderableDataBuilder().build();
  private Program program = new ProgramDataBuilder().build();
  private FacilityType facilityType = new FacilityTypeDataBuilder().build();
  private ApprovedProductDto importer = new ApprovedProductDto();

  @Before
  public void setUp() {
    new FacilityTypeApprovedProductsDataBuilder()
        .withProgram(program)
        .withFacilityType(facilityType)
        .build()
        .export(importer);

    importer.setOrderable(orderable);

    when(programRepository.findOne(program.getId())).thenReturn(program);
    when(facilityTypeRepository.findOne(facilityType.getId())).thenReturn(facilityType);
  }

  @Test
  public void shouldBuildDomainObjectBasedOnDataFromImporter() {
    FacilityTypeApprovedProduct approvedProduct = builder.build(importer);

    assertThat(approvedProduct)
        .hasFieldOrPropertyWithValue("id", importer.getId())
        .hasFieldOrPropertyWithValue("maxPeriodsOfStock", importer.getMaxPeriodsOfStock())
        .hasFieldOrPropertyWithValue("minPeriodsOfStock", importer.getMinPeriodsOfStock())
        .hasFieldOrPropertyWithValue("emergencyOrderPoint", importer.getEmergencyOrderPoint())
        .hasFieldOrPropertyWithValue("orderableId", orderable.getId())
        .hasFieldOrPropertyWithValue("program", program)
        .hasFieldOrPropertyWithValue("facilityType", facilityType);
  }

  @Test
  public void shouldThrowExceptionIfProgramCouldNotBeFound() {
    when(programRepository.findOne(program.getId())).thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ProgramMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityTypeCouldNotBeFound() {
    when(facilityTypeRepository.findOne(facilityType.getId())).thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfProgramIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ProgramMessageKeys.ERROR_NOT_FOUND);

    importer.getProgram().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityTypeIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeMessageKeys.ERROR_NOT_FOUND);

    importer.getFacilityType().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfProgramImporterDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ProgramMessageKeys.ERROR_NOT_FOUND);

    importer.setProgram((ProgramDto) null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityTypeImporterDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeMessageKeys.ERROR_NOT_FOUND);

    importer.setFacilityType((FacilityTypeDto) null);
    builder.build(importer);
  }

}
