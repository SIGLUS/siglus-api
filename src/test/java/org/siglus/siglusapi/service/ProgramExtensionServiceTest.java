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
import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_NAME;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.siglusapi.dto.SiglusProgramDto;

@RunWith(MockitoJUnitRunner.class)
public class ProgramExtensionServiceTest {

  private static final int NOT_TOO_LONG = 16;

  @InjectMocks
  private ProgramExtensionService service;

  @Mock
  private ProgramExtensionRepository repo;

  @Mock
  private ProgramReferenceDataService programRefDataService;

  @Test
  public void shouldCallRepoWhenFindByProgramId() {
    // given
    UUID programId = UUID.randomUUID();

    // when
    service.findByProgramId(programId);

    // then
    verify(repo).findByProgramId(programId);
  }

  @Test
  public void shouldReturnAllProgramListWhenGetProgramsGivenAllProductsProgramCode() {
    // when
    List<SiglusProgramDto> programs = service.getPrograms(ALL_PRODUCTS_PROGRAM_CODE);

    // then
    assertEquals(allProgramList(), programs);
  }

  private List<SiglusProgramDto> allProgramList() {
    return singletonList(getAllProgramDto());
  }

  @Test
  public void shouldReturnListWithSpecifiedProgramWhenGetProgramsGivenNotAllProductsProgramCode() {
    // given
    String code = random(NOT_TOO_LONG);
    while (code.equals(ALL_PRODUCTS_PROGRAM_CODE)) {
      code = random(NOT_TOO_LONG);
    }
    ProgramDto mockProgram1 = mockProgram(UUID.randomUUID(), code);
    ProgramDto mockProgram2 = mockProgram(UUID.randomUUID(), random(NOT_TOO_LONG));
    when(programRefDataService.findAll()).thenReturn(asList(mockProgram1, mockProgram2));
    ProgramExtension mockProgramExtension1 = mockProgramExtension(mockProgram1);
    ProgramExtension mockProgramExtension2 = mockProgramExtension(mockProgram2);
    when(repo.findAll()).thenReturn(asList(mockProgramExtension1, mockProgramExtension2));
    // when
    List<SiglusProgramDto> programs = service.getPrograms(code);

    // then
    SiglusProgramDto expected = expectedSiglusProgram(mockProgram1, mockProgramExtension1);
    int expectedSize = 1;
    assertEquals(expectedSize, programs.size());
    assertThat(programs, hasItem(expected));
  }

  @Test
  public void shouldReturnProgramListWhenGetProgramsGivenNoMatchedProgramExt() {
    // given
    String code = random(NOT_TOO_LONG);
    while (code.equals(ALL_PRODUCTS_PROGRAM_CODE)) {
      code = random(NOT_TOO_LONG);
    }
    UUID programId = UUID.randomUUID();
    ProgramDto mockProgram = mockProgram(programId, code);
    when(programRefDataService.findAll()).thenReturn(singletonList(mockProgram));
    // when
    List<SiglusProgramDto> programs = service.getPrograms(code);

    // then
    SiglusProgramDto expected = expectedSiglusProgram(mockProgram, null);
    int expectedSize = 1;
    assertEquals(expectedSize, programs.size());
    assertThat(programs, hasItem(expected));
  }

  @Test
  public void shouldReturnAllProgramWhenGetProgramGivenAllProductsProgramId() {
    // when
    SiglusProgramDto program = service.getProgram(ALL_PRODUCTS_PROGRAM_ID);

    // then
    assertEquals(getAllProgramDto(), program);
  }

  private SiglusProgramDto getAllProgramDto() {
    SiglusProgramDto siglusProgramDto = new SiglusProgramDto();
    siglusProgramDto.setId(ALL_PRODUCTS_PROGRAM_ID);
    siglusProgramDto.setCode(ALL_PRODUCTS_PROGRAM_CODE);
    siglusProgramDto.setName(ALL_PRODUCTS_PROGRAM_NAME);
    return siglusProgramDto;
  }

  @Test
  public void shouldProgramWhenGetProgramGivenNotAllProductsProgramId() {
    // given
    UUID programId = UUID.randomUUID();
    ProgramDto mockProgram = mockProgram(programId, random(NOT_TOO_LONG));
    when(programRefDataService.findOne(programId)).thenReturn(mockProgram);
    ProgramExtension mockProgramExtension = mockProgramExtension(mockProgram);
    when(repo.findByProgramId(programId)).thenReturn(mockProgramExtension);

    // when
    SiglusProgramDto program = service.getProgram(programId);

    // then
    SiglusProgramDto expected = expectedSiglusProgram(mockProgram, mockProgramExtension);
    assertEquals(expected, program);
  }

  private ProgramDto mockProgram(UUID programId, String code) {
    ProgramDto mockProgram = new ProgramDto();
    mockProgram.setId(programId);
    mockProgram.setCode(code);
    return mockProgram;
  }

  private ProgramExtension mockProgramExtension(ProgramDto program) {
    ProgramExtension mockProgramExtension = new ProgramExtension();
    mockProgramExtension.setId(UUID.randomUUID());
    mockProgramExtension.setCode(program.getCode());
    mockProgramExtension.setProgramId(program.getId());
    mockProgramExtension.setIsVirtual(nextBoolean());
    mockProgramExtension.setParentId(UUID.randomUUID());
    mockProgramExtension.setIsSupportEmergency(nextBoolean());
    return mockProgramExtension;
  }

  private SiglusProgramDto expectedSiglusProgram(ProgramDto programDto,
      ProgramExtension extension) {
    SiglusProgramDto siglusProgramDto = new SiglusProgramDto();
    siglusProgramDto.setId(programDto.getId());
    siglusProgramDto.setCode(programDto.getCode());
    if (extension != null) {
      siglusProgramDto.setIsVirtual(extension.getIsVirtual());
      siglusProgramDto.setParentId(extension.getParentId());
      siglusProgramDto.setIsSupportEmergency(extension.getIsSupportEmergency());
    }
    return siglusProgramDto;
  }

}
