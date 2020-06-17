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

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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

  public ProgramExtension findByProgramId(UUID programId) {
    return repo.findByProgramId(programId);
  }

  @Test
  public void shouldCallRepoWhenFindByProgramId() {
    //given
    UUID programId = UUID.randomUUID();

    //when
    service.findByProgramId(programId);

    //then
    verify(repo).findByProgramId(programId);
  }

  @Test
  public void shouldReturnAllProgramListWhenGetProgramsGivenAllProductsProgramCode() {
    //given & when
    List<SiglusProgramDto> programs = service.getPrograms(ALL_PRODUCTS_PROGRAM_CODE);

    //then
    assertEquals(allProgramList(), programs);
  }

  private List<SiglusProgramDto> allProgramList() {
    return singletonList(allProgram());
  }

  @Test
  public void shouldReturnAllProgramListWhenGetProgramsGivenNotAllProductsProgramCode() {
    //given
    when(programRefDataService.findAll()).thenReturn(singletonList(new ProgramDto()));
    when(repo.findAll()).thenReturn(singletonList(new ProgramExtension()));
    String code = random(NOT_TOO_LONG);
    while (code.equals(ALL_PRODUCTS_PROGRAM_CODE)) {
      code = random(NOT_TOO_LONG);
    }
    //when
    service.getPrograms(code);

    //then
    verify(programRefDataService).findAll();
    verify(repo).findAll();
  }

  @Test
  public void shouldReturnProgramListWhenGetProgramsGivenNotAllProductsProgramCodeAndNoPrgExt() {
    //given
    when(programRefDataService.findAll()).thenReturn(singletonList(new ProgramDto()));
    String code = random(NOT_TOO_LONG);
    while (code.equals(ALL_PRODUCTS_PROGRAM_CODE)) {
      code = random(NOT_TOO_LONG);
    }
    //when
    service.getPrograms(code);

    //then
    verify(programRefDataService).findAll();
    verify(repo).findAll();
  }

  @Test
  public void shouldReturnAllProgramWhenGetProgramGivenAllProductsProgramId() {
    //given & when
    SiglusProgramDto program = service.getProgram(ALL_PRODUCTS_PROGRAM_ID);

    //then
    assertEquals(allProgram(), program);
  }

  private SiglusProgramDto allProgram() {
    SiglusProgramDto siglusProgramDto = new SiglusProgramDto();
    siglusProgramDto.setId(ALL_PRODUCTS_PROGRAM_ID);
    siglusProgramDto.setCode(ALL_PRODUCTS_PROGRAM_CODE);
    siglusProgramDto.setName(ALL_PRODUCTS_PROGRAM_NAME);
    return siglusProgramDto;
  }

  @Test
  public void shouldProgramWhenGetProgramGivenNotAllProductsProgramId() {
    //given
    when(programRefDataService.findOne(any())).thenReturn(new ProgramDto());
    when(repo.findByProgramId(any())).thenReturn(new ProgramExtension());
    UUID programId = UUID.randomUUID();

    //when
    service.getProgram(programId);

    //then
    verify(programRefDataService).findOne(programId);
    verify(repo).findByProgramId(programId);
  }

}
