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

package org.siglus.siglusapi.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_NAME;

import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProgramServiceTest {

  private static final int NOT_TOO_LONG = 16;

  @InjectMocks
  private SiglusProgramService service;

  @Mock
  private ProgramReferenceDataService programRefDataService;

  @Test
  public void shouldReturnAllProgramListWhenGetProgramsGivenAllProductsProgramCode() {
    // when
    List<ProgramDto> programs = service.getPrograms(ALL_PRODUCTS_PROGRAM_CODE);

    // then
    assertEquals(allProgramList(), programs);
  }

  private List<ProgramDto> allProgramList() {
    return singletonList(getAllProgramDto());
  }

  @Test
  public void shouldReturnListWithSpecifiedProgramWhenGetProgramsGivenNotAllProductsProgramCode() {
    // given
    String code = mockRandomCode();
    ProgramDto mockProgram1 = mockProgram(UUID.randomUUID(), code);
    ProgramDto mockProgram2 = mockProgram(UUID.randomUUID(), random(NOT_TOO_LONG));
    when(programRefDataService.findAll()).thenReturn(asList(mockProgram1, mockProgram2));
    // when
    List<ProgramDto> programs = service.getPrograms(code);

    // then
    int expectedSize = 1;
    assertEquals(expectedSize, programs.size());
    assertThat(programs, hasItem(mockProgram1));
  }

  @Test
  public void shouldReturnProgramListWhenGetProgramsGivenNoMatchedProgramExt() {
    // given
    String code = mockRandomCode();
    UUID programId = UUID.randomUUID();
    ProgramDto mockProgram = mockProgram(programId, code);
    when(programRefDataService.findAll()).thenReturn(singletonList(mockProgram));
    // when
    List<ProgramDto> programs = service.getPrograms(code);

    // then
    int expectedSize = 1;
    assertEquals(expectedSize, programs.size());
    assertThat(programs, hasItem(mockProgram));
  }

  @Test
  public void shouldReturnAllProgramWhenGetProgramGivenAllProductsProgramId() {
    // when
    ProgramDto program = service.getProgram(ALL_PRODUCTS_PROGRAM_ID);

    // then
    assertEquals(getAllProgramDto(), program);
  }

  private ProgramDto getAllProgramDto() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(ALL_PRODUCTS_PROGRAM_ID);
    programDto.setCode(ALL_PRODUCTS_PROGRAM_CODE);
    programDto.setName(ALL_PRODUCTS_PROGRAM_NAME);
    return programDto;
  }

  @Test
  public void shouldProgramWhenGetProgramGivenNotAllProductsProgramId() {
    // given
    UUID programId = UUID.randomUUID();
    ProgramDto mockProgram = mockProgram(programId, random(NOT_TOO_LONG));
    when(programRefDataService.findOne(programId)).thenReturn(mockProgram);

    // when
    ProgramDto program = service.getProgram(programId);

    // then
    assertEquals(mockProgram, program);
  }

  @Test
  public void shouldReturnWhenGetProgramByCode() {
    // given
    UUID programId = UUID.randomUUID();
    String code = mockRandomCode();
    ProgramDto mockProgram = mockProgram(programId, code);
    when(programRefDataService.findAll()).thenReturn(Lists.newArrayList(mockProgram));

    // when
    ProgramDto program = service.getProgramByCode(code).orElse(null);

    // then
    assertEquals(mockProgram, program);
  }

  @Test
  public void shouldReturnAllPrograms() {
    UUID programId = UUID.randomUUID();
    String code = mockRandomCode();
    ProgramDto mockProgram = mockProgram(programId, code);
    when(programRefDataService.findAll()).thenReturn(Lists.newArrayList(mockProgram));

    List<ProgramDto> allPrograms = service.getAllPrograms();

    assertEquals(1, allPrograms.size());
    assertEquals(mockProgram, allPrograms.get(0));
  }

  private ProgramDto mockProgram(UUID programId, String code) {
    ProgramDto mockProgram = new ProgramDto();
    mockProgram.setId(programId);
    mockProgram.setCode(code);
    return mockProgram;
  }

  private String mockRandomCode() {
    String code = random(NOT_TOO_LONG);
    while (code.equals(ALL_PRODUCTS_PROGRAM_CODE)) {
      code = random(NOT_TOO_LONG);
    }
    return code;
  }

}
