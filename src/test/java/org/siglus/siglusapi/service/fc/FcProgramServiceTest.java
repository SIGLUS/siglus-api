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

package org.siglus.siglusapi.service.fc;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.fc.ProgramDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;

@RunWith(MockitoJUnitRunner.class)
public class FcProgramServiceTest {

  @InjectMocks
  private FcProgramService fcProgramService;

  @Captor
  private ArgumentCaptor<Set<ProgramRealProgram>> programsArgumentCaptor;

  @Mock
  private ProgramRealProgramRepository programRealProgramRepository;

  private final String programCode1 = "programCode1";
  private final String programName1 = "programName1";
  private final String programCode2 = "programCode2";
  private final String programCode3 = "programCode3";

  private final UUID programId1 = UUID.randomUUID();
  private final UUID programId2 = UUID.randomUUID();

  @Test
  public void shouldReturnFalseGivenEmptyFcResult() {

    // when
    boolean result = fcProgramService.processPrograms(Collections.emptyList());

    // then
    assertFalse(result);
  }

  @Test
  public void shouldSaveProgramData() {
    // given
    when(programRealProgramRepository.findAll())
        .thenReturn(newArrayList(
            mockProgram(programId1, programCode1, programName1, true),
            mockProgram(programId2, programCode2, "toUpdate", false)));

    // when
    String programStatus3 = "Activo";
    String programName3 = "programName3";
    String programStatus2 = "Inactivo";
    String programStatus1 = "Activo";
    String programName2 = "programName2";
    fcProgramService.processPrograms(newArrayList(
        mockProgramDto(programCode1, programName1, programStatus1),
        mockProgramDto(programCode2, programName2, programStatus2),
        mockProgramDto(programCode3, programName3, programStatus3)
    ));

    // then
    verify(programRealProgramRepository).save(programsArgumentCaptor.capture());
    Set<ProgramRealProgram> saved = programsArgumentCaptor.getValue();
    assertEquals(2, saved.size());
    ProgramRealProgram p2 = saved.stream()
        .filter(p -> programCode2.equals(p.getRealProgramCode()))
        .findFirst()
        .get();
    assertEquals(programId2, p2.getId());
    assertEquals(programName2, p2.getRealProgramName());
    assertFalse(p2.getActive());
    ProgramRealProgram p3 = saved.stream()
        .filter(p -> programCode3.equals(p.getRealProgramCode()))
        .findFirst()
        .get();
    assertNull(p3.getId());
    assertEquals(programName3, p3.getRealProgramName());
    assertTrue(p3.getActive());
  }

  @Test
  public void shouldReturnFalseIfCatchExceptionWhenDealCpData() {
    // given
    when(programRealProgramRepository.findAll()).thenThrow(new RuntimeException());

    // when
    boolean result = fcProgramService.processPrograms(newArrayList(
        mockProgramDto(programCode1, programName1, "Active")
    ));

    // then
    assertFalse(result);
  }

  private ProgramDto mockProgramDto(String code, String name, String status) {
    return ProgramDto
        .builder()
        .code(code)
        .description(name)
        .status(status)
        .build();
  }

  private ProgramRealProgram mockProgram(UUID id, String code, String name, boolean active) {
    ProgramRealProgram program = new ProgramRealProgram();
    program.setId(id);
    program.setRealProgramCode(code);
    program.setRealProgramName(name);
    program.setActive(active);
    return program;
  }
}
