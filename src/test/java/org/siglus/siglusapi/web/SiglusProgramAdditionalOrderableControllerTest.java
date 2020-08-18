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

package org.siglus.siglusapi.web;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.dto.ProgramAdditionalOrderableDto;
import org.siglus.siglusapi.service.SiglusProgramAdditionalOrderableService;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProgramAdditionalOrderableControllerTest {

  @InjectMocks
  private SiglusProgramAdditionalOrderableController siglusProgramAdditionalOrderableController;

  @Mock
  private SiglusProgramAdditionalOrderableService siglusProgramAdditionalOrderableService;

  private UUID id = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private UUID additionalOrderableId = UUID.randomUUID();

  private String code = "code";

  private String name = "name";

  private UUID orderableOriginProgramId = UUID.randomUUID();

  @Mock
  private Pageable pageable;

  @Test
  public void shouldCallSearchAdditionalOrderables() {
    // when
    siglusProgramAdditionalOrderableController.searchAdditionalOrderables(programId, code, name,
        orderableOriginProgramId, pageable);

    // then
    verify(siglusProgramAdditionalOrderableService).searchAdditionalOrderables(programId, code,
        name, orderableOriginProgramId, pageable);
  }

  @Test
  public void shouldCallDeleteAdditionalOrderable() {
    // when
    siglusProgramAdditionalOrderableController.deleteAdditionalOrderable(id);

    // then
    verify(siglusProgramAdditionalOrderableService).deleteAdditionalOrderable(id);
  }

  @Test
  public void shouldCallCreateAdditionalOrderables() {
    // given
    ProgramAdditionalOrderableDto dto = ProgramAdditionalOrderableDto.builder()
        .programId(programId)
        .additionalOrderableId(additionalOrderableId)
        .orderableOriginProgramId(orderableOriginProgramId)
        .build();
    List<ProgramAdditionalOrderableDto> dtos = newArrayList(dto);

    // when
    siglusProgramAdditionalOrderableController.createAdditionalOrderables(dtos);

    // then
    verify(siglusProgramAdditionalOrderableService).createAdditionalOrderables(dtos);
  }
}
