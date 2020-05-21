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

package org.openlmis.requisition.web;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.SupportedProgramDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.testutils.FacilityDtoDataBuilder;
import org.openlmis.requisition.testutils.SupportedProgramDtoDataBuilder;
import org.openlmis.requisition.utils.DateHelper;

@RunWith(MockitoJUnitRunner.class)
public class FacilitySupportsProgramHelperTest {

  private static final LocalDate SUPPORT_START_DATE = LocalDate.of(2011,12,12);

  @Mock
  FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  DateHelper dateHelper;

  @InjectMocks
  FacilitySupportsProgramHelper facilitySupportsProgramHelper;

  private FacilityDto facilityDto;
  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();

  @Before
  public void setUp() throws Exception {
    facilityDto = new FacilityDtoDataBuilder().buildAsDto();

    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(dateHelper.isDateBeforeNow(any(LocalDate.class))).thenReturn(true);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenNoSupportedPrograms() {
    facilityDto.setSupportedPrograms(Collections.emptyList());

    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenProgramIsNotSupported() {
    SupportedProgramDto supportedProgramDto = new SupportedProgramDtoDataBuilder()
        .withId(UUID.randomUUID())
        .withSupportActive(true)
        .withProgramActive(true)
        .withSupportStartDate(SUPPORT_START_DATE)
        .buildAsDto();

    facilityDto.setSupportedPrograms(Collections.singletonList(supportedProgramDto));

    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);
  }

  @Test
  public void shouldPassWhenProgramIsSupported() {
    SupportedProgramDto supportedProgramDto = new SupportedProgramDtoDataBuilder()
        .withId(programId)
        .withSupportActive(true)
        .withProgramActive(true)
        .withSupportStartDate(SUPPORT_START_DATE)
        .buildAsDto();

    facilityDto.setSupportedPrograms(Collections.singletonList(supportedProgramDto));

    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenSupportIsNotActive() {
    SupportedProgramDto supportedProgramDto = new SupportedProgramDtoDataBuilder()
        .withId(programId)
        .withSupportActive(false)
        .withProgramActive(true)
        .withSupportStartDate(SUPPORT_START_DATE)
        .buildAsDto();

    facilityDto.setSupportedPrograms(Collections.singletonList(supportedProgramDto));

    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenProgramIsNotActive() {
    SupportedProgramDto supportedProgramDto = new SupportedProgramDtoDataBuilder()
        .withId(programId)
        .withSupportActive(true)
        .withProgramActive(false)
        .withSupportStartDate(SUPPORT_START_DATE)
        .buildAsDto();

    facilityDto.setSupportedPrograms(Collections.singletonList(supportedProgramDto));

    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenStartDateIsAfterCurrentDate() {
    SupportedProgramDto supportedProgramDto = new SupportedProgramDtoDataBuilder()
        .withId(programId)
        .withSupportActive(true)
        .withProgramActive(true)
        .buildAsDto();
    when(dateHelper.isDateBeforeNow(any(LocalDate.class))).thenReturn(false);

    facilityDto.setSupportedPrograms(Collections.singletonList(supportedProgramDto));

    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);
  }

  @Test
  public void shouldPassWhenProgramStartDateIsNull() {
    SupportedProgramDto supportedProgramDto = new SupportedProgramDtoDataBuilder()
        .withId(programId)
        .withSupportActive(true)
        .withProgramActive(true)
        .withSupportStartDate(null)
        .buildAsDto();

    facilityDto.setSupportedPrograms(Collections.singletonList(supportedProgramDto));

    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);
  }

}
