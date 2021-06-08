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

package org.siglus.siglusapi.service.android;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenCategory;
import org.siglus.siglusapi.dto.android.response.RegimenCategoryResponse;
import org.siglus.siglusapi.dto.android.response.RegimenResponse;
import org.siglus.siglusapi.repository.RegimenRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusRegimenServiceTest {

  @InjectMocks
  private SiglusRegimenService service;
  @Mock
  private RegimenRepository repo;
  @Mock
  private ProgramReferenceDataService programDataService;

  @Before
  public void prepare() {
    UUID programId1 = UUID.randomUUID();
    Regimen regimen1 = mock(Regimen.class);
    when(regimen1.getCode()).thenReturn("regimen code 1");
    when(regimen1.getName()).thenReturn("regimen name 1");
    when(regimen1.getActive()).thenReturn(false);
    when(regimen1.getProgramId()).thenReturn(programId1);
    when(regimen1.getIsCustom()).thenReturn(false);
    when(regimen1.getDisplayOrder()).thenReturn(1);
    RegimenCategory category1 = mock(RegimenCategory.class);
    when(category1.getCode()).thenReturn("category code 1");
    when(category1.getName()).thenReturn("category name 1");
    when(category1.getDisplayOrder()).thenReturn(1);
    when(regimen1.getRegimenCategory()).thenReturn(category1);
    UUID programId2 = UUID.randomUUID();
    Regimen regimen2 = mock(Regimen.class);
    when(regimen2.getCode()).thenReturn("regimen code 2");
    when(regimen2.getName()).thenReturn("regimen name 2");
    when(regimen2.getActive()).thenReturn(true);
    when(regimen2.getProgramId()).thenReturn(programId2);
    when(regimen2.getIsCustom()).thenReturn(true);
    when(regimen2.getDisplayOrder()).thenReturn(2);
    RegimenCategory category2 = mock(RegimenCategory.class);
    when(category2.getCode()).thenReturn("category code 2");
    when(category2.getName()).thenReturn("category name 2");
    when(category2.getDisplayOrder()).thenReturn(2);
    when(regimen2.getRegimenCategory()).thenReturn(category2);
    when(repo.findAll()).thenReturn(asList(regimen1, regimen2));
    ProgramDto program1 = mock(ProgramDto.class);
    when(program1.getId()).thenReturn(programId1);
    when(program1.getCode()).thenReturn("program code 1");
    when(program1.getName()).thenReturn("program name 1");
    ProgramDto program2 = mock(ProgramDto.class);
    when(program2.getId()).thenReturn(programId2);
    when(program2.getCode()).thenReturn("program code 2");
    when(program2.getName()).thenReturn("program name 2");
    when(programDataService.findAll()).thenReturn(asList(program1, program2));
  }

  @Test
  public void shouldReturnResponseWhenGetRegimens() {
    // when
    List<RegimenResponse> regimens = service.getRegimens();

    // then
    assertEquals(2, regimens.size());
    RegimenResponse regimen1 = regimens.get(0);
    assertEquals("regimen code 1", regimen1.getCode());
    assertEquals("regimen name 1", regimen1.getName());
    assertEquals(1, (int) regimen1.getDisplayOrder());
    assertFalse(regimen1.getActive());
    assertFalse(regimen1.getIsCustom());
    assertEquals("program code 1", regimen1.getProgramCode());
    assertEquals("program name 1", regimen1.getProgramName());
    RegimenCategoryResponse category1 = regimen1.getCategory();
    assertNotNull(category1);
    assertEquals("category code 1", category1.getCode());
    assertEquals("category name 1", category1.getName());
    assertEquals(1, (int) category1.getDisplayOrder());

    RegimenResponse regimen2 = regimens.get(1);
    assertEquals("regimen code 2", regimen2.getCode());
    assertEquals("regimen name 2", regimen2.getName());
    assertEquals(2, (int) regimen2.getDisplayOrder());
    assertTrue(regimen2.getActive());
    assertTrue(regimen2.getIsCustom());
    assertEquals("program code 2", regimen2.getProgramCode());
    assertEquals("program name 2", regimen2.getProgramName());
    RegimenCategoryResponse category2 = regimen2.getCategory();
    assertNotNull(category2);
    assertEquals("category code 2", category2.getCode());
    assertEquals("category name 2", category2.getName());
    assertEquals(2, (int) category2.getDisplayOrder());
  }


}
