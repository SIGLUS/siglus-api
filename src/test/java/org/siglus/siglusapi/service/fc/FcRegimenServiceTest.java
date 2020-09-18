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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;

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
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenCategory;
import org.siglus.siglusapi.dto.fc.RegimenDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.RegimenCategoryRepository;
import org.siglus.siglusapi.repository.RegimenRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FcRegimenServiceTest {

  @InjectMocks
  private FcRegimenService fcRegimenService;

  @Mock
  private RegimenRepository regimenRepository;

  @Mock
  private ProgramRealProgramRepository programRealProgramRepository;

  @Mock
  private ProgramReferenceDataService programRefDataService;

  @Mock
  private RegimenCategoryRepository regimenCategoryRepository;

  @Captor
  private ArgumentCaptor<Set<Regimen>> regimensArgumentCaptor;

  private final String code1 = "A2F";
  private final String description1 = "AZT+3TC+LPV/r";
  private final String areaCode1 = "T";
  private final String categoryCode1 = "ADULTS";
  private final String categoryDescription1 = "ADULTS";
  private final String code2 = "TBMDRKm-Lfx-Eto-PAS-E-Z";
  private final String description2 = "TB MDR Km-Lfx-Eto-PAS-E-Z";
  private final String areaCode2 = "TB";
  private final String categoryCode2 = "PAEDIATRICS";
  private final String categoryDescription2 = "PAEDIATRICS";
  private final String code3 = "A3A";
  private final String categoryCode3 = null;
  private final String programCode1 = "ARVP";
  private final UUID programId1 = UUID.randomUUID();
  private final UUID categoryId2 = UUID.randomUUID();
  private final UUID programId3 = UUID.randomUUID();

  @Test
  public void shouldReturnFalseGivenEmptyFcResult() {

    // when
    boolean result = fcRegimenService.processRegimens(Collections.emptyList());

    // then
    assertFalse(result);
  }

  @Test
  public void shouldSaveRegimenData() {
    // given
    when(programRealProgramRepository.findAll())
        .thenReturn(newArrayList(mockProgramRealProgram1(), mockProgramRealProgram2()));
    when(programRefDataService.findAll()).thenReturn(newArrayList(
        mockProgramDto(programId1, programCode1)));
    when(regimenRepository.findAll()).thenReturn(newArrayList(mockRegimen1(), mockRegimen3()));
    when(regimenCategoryRepository.findAll())
        .thenReturn(newArrayList(mockCategory(categoryId2, categoryCode2, categoryDescription2)));

    // when
    fcRegimenService.processRegimens(newArrayList(mockRegimenDto1(), mockRegimenDto2(),
        mockRegimenDto3()));

    // then
    verify(regimenRepository).save(regimensArgumentCaptor.capture());
    Set<Regimen> saved = regimensArgumentCaptor.getValue();
    assertEquals(2, saved.size());
    Regimen r1 = saved.stream()
        .filter(r -> code1.equals(r.getCode()))
        .findFirst()
        .get();
    assertEquals(description1, r1.getName());
    assertEquals(categoryCode1, r1.getRegimenCategory().getCode());
    assertEquals(categoryDescription1, r1.getRegimenCategory().getName());
    assertNull(r1.getRegimenCategory().getId());
    Regimen r3 = saved.stream()
        .filter(r -> code3.equals(r.getCode()))
        .findFirst()
        .get();
    assertEquals(description3, r3.getName());
    assertNull(r3.getRegimenCategory());
  }

  private RegimenDto mockRegimenDto1() {
    return RegimenDto
        .builder()
        .code(code1)
        .description(description1)
        .areaCode(areaCode1)
        .categoryCode(categoryCode1)
        .categoryDescription(categoryDescription1)
        .status(STATUS_ACTIVE)
        .build();
  }

  private RegimenDto mockRegimenDto2() {
    return RegimenDto
        .builder()
        .code(code2)
        .description(description2)
        .areaCode(areaCode2)
        .categoryCode(categoryCode2)
        .categoryDescription(categoryDescription2)
        .status(STATUS_ACTIVE)
        .build();
  }

  private RegimenDto mockRegimenDto3() {
    String description3 = "ddI250+3TC+NVP";
    return RegimenDto
        .builder()
        .code(code3)
        .description(description3)
        .areaCode(areaCode1)
        .categoryCode(categoryCode3)
        .status(STATUS_ACTIVE)
        .build();
  }

  private ProgramRealProgram mockProgramRealProgram1() {
    return ProgramRealProgram
        .builder()
        .programCode(programCode1)
        .programName("ARV")
        .realProgramCode(areaCode1)
        .build();
  }

  private ProgramRealProgram mockProgramRealProgram2() {
    String programCode2 = "MP";
    return ProgramRealProgram
        .builder()
        .programCode(programCode2)
        .programName("Multiple")
        .realProgramCode(areaCode2)
        .build();
  }

  private ProgramDto mockProgramDto(UUID id, String code) {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(id);
    programDto.setCode(code);
    return programDto;
  }

  private Regimen mockRegimen1() {
    String dbDescription1 = "AZT+3TC";
    return Regimen.builder()
        .code(code1)
        .name(dbDescription1)
        .programId(programId1)
        .regimenCategory(null)
        .active(true)
        .displayOrder(0)
        .build();
  }

  private Regimen mockRegimen3() {
    String dbDescription3 = "ABC";
    return Regimen.builder()
        .code(code3)
        .name(dbDescription3)
        .regimenCategory(mockCategory(categoryId2, categoryCode2, categoryDescription2))
        .programId(programId3)
        .active(true)
        .displayOrder(1)
        .build();
  }

  private RegimenCategory mockCategory(UUID id, String code, String name) {
    RegimenCategory category = new RegimenCategory();
    category.setId(id);
    category.setCode(code);
    category.setName(name);
    category.setDisplayOrder(0);
    return category;
  }
}
