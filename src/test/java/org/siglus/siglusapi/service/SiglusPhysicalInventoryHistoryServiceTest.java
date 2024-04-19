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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryListDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.PhysicalInventoryHistoryRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class SiglusPhysicalInventoryHistoryServiceTest {

  @InjectMocks
  private SiglusPhysicalInventoryHistoryService siglusPhysicalInventoryHistoryService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;
  @Mock
  private PhysicalInventoryHistoryRepository physicalInventoryHistoryRepository;


  @Before
  public void prepare() {
  }

  @Test
  public void shouldReturnSiglusPhysicalInventoryHistoryListDtos() {
    // given
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(UUID.randomUUID());
    SiglusPhysicalInventoryHistoryDto dto = SiglusPhysicalInventoryHistoryDto.builder()
        .physicalInventoryHistoryId(UUID.randomUUID())
        .physicalInventoryExtensionId(UUID.randomUUID())
        .physicalInventoryId(UUID.randomUUID())
        .groupId(UUID.randomUUID())
        .programName("programName")
        .completedDate(LocalDate.now())
        .build();
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoryHistoryRepository.queryPhysicalInventoryHistories(userDto.getHomeFacilityId())).thenReturn(
        Collections.singletonList(dto));

    // when
    List<SiglusPhysicalInventoryHistoryListDto> listDtos = siglusPhysicalInventoryHistoryService
        .searchPhysicalInventoryHistories();

    // then
    SiglusPhysicalInventoryHistoryListDto listDto = SiglusPhysicalInventoryHistoryListDto.builder()
        .groupId(dto.getGroupId())
        .programName(dto.getProgramName())
        .completedDate(dto.getCompletedDate())
        .build();
    assertThat(listDtos.size()).isEqualTo(1);
    assertThat(listDtos.get(0).getGroupId()).isEqualTo(listDto.getGroupId());
    assertThat(listDtos.get(0).getProgramName()).isEqualTo(listDto.getProgramName());
    assertThat(listDtos.get(0).getCompletedDate()).isEqualTo(listDto.getCompletedDate());
  }

}
