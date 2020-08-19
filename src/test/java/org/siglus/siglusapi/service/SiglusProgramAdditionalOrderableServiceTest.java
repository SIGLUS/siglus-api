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

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.ProgramAdditionalOrderable;
import org.siglus.siglusapi.dto.ProgramAdditionalOrderableDto;
import org.siglus.siglusapi.repository.ProgramAdditionalOrderableRepository;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProgramAdditionalOrderableServiceTest {

  @Captor
  private ArgumentCaptor<List<ProgramAdditionalOrderable>> argumentCaptor;

  @InjectMocks
  private SiglusProgramAdditionalOrderableService siglusProgramAdditionalOrderableService;

  @Mock
  private ProgramAdditionalOrderableRepository programAdditionalOrderableRepository;

  private UUID id = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private UUID additionalOrderableId = UUID.randomUUID();

  private String code = "code";

  private String name = "name";

  private UUID orderableOriginProgramId = UUID.randomUUID();

  @Mock
  private Pageable pageable;

  @Test
  public void shouldCallSearchWithOrderableOriginProgramIdWhenOrderableOriginProgramIdNotNull() {
    // when
    siglusProgramAdditionalOrderableService.searchAdditionalOrderables(programId, code, name,
        orderableOriginProgramId, pageable);

    // then
    verify(programAdditionalOrderableRepository).search(programId, "%CODE%", "%NAME%",
        orderableOriginProgramId, pageable);
  }

  @Test
  public void shouldCallSearchWithoutOrderableOriginProgramIdWhenOrderableOriginProgramIdIsNull() {
    // when
    siglusProgramAdditionalOrderableService.searchAdditionalOrderables(programId, code, name,
        null, pageable);

    // then
    verify(programAdditionalOrderableRepository).search(programId, "%CODE%", "%NAME%",
        pageable);
  }

  @Test
  public void shouldCallDelete() {
    // given
    ProgramAdditionalOrderable additionalOrderable = new ProgramAdditionalOrderable();
    when(programAdditionalOrderableRepository.findOne(id))
        .thenReturn(additionalOrderable);

    // when
    siglusProgramAdditionalOrderableService.deleteAdditionalOrderable(id);

    // then
    verify(programAdditionalOrderableRepository).delete(additionalOrderable);
  }

  @Test
  public void shouldCallSaveWhenCreateAdditionalOrderables() {
    // given
    ProgramAdditionalOrderableDto dto = ProgramAdditionalOrderableDto.builder()
        .programId(programId)
        .additionalOrderableId(additionalOrderableId)
        .orderableOriginProgramId(orderableOriginProgramId)
        .build();

    // when
    siglusProgramAdditionalOrderableService.createAdditionalOrderables(newArrayList(dto));

    // then
    verify(programAdditionalOrderableRepository).save(argumentCaptor.capture());
    List<ProgramAdditionalOrderable> additionalOrderables = argumentCaptor.getValue();
    assertEquals(1, additionalOrderables.size());
    assertEquals(programId, additionalOrderables.get(0).getProgramId());
    assertEquals(additionalOrderableId, additionalOrderables.get(0).getAdditionalOrderableId());
    assertEquals(orderableOriginProgramId, additionalOrderables.get(0)
        .getOrderableOriginProgramId());
  }
}
