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

package org.siglus.siglusapi.localmachine.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;

@RunWith(MockitoJUnitRunner.class)
public class EventCommonServiceTest {

  @InjectMocks
  private EventCommonService eventCommonService;

  @Mock
  private RequisitionGroupMembersRepository requisitionGroupMembersRepository;

  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @Test
  public void shouldGetReceiverId() {
    // given
    given(requisitionGroupMembersRepository
        .findParentFacilityByRequisitionGroup(facilityId, Collections.singleton(programId)))
        .willReturn(Lists.newArrayList(mockRequisitionGroupMembersDto()));

    // when
    UUID receiverId = eventCommonService.getReceiverId(facilityId, programId);

    // then
    assertThat(receiverId).isEqualTo(facilityId);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIllegalStateException() {
    // given
    given(requisitionGroupMembersRepository
        .findParentFacilityByRequisitionGroup(facilityId, Collections.singleton(programId)))
        .willReturn(null);

    // when
    eventCommonService.getReceiverId(facilityId, programId);
  }

  private RequisitionGroupMembersDto mockRequisitionGroupMembersDto() {
    RequisitionGroupMembersDto requisitionGroupMembersDto = new RequisitionGroupMembersDto();
    requisitionGroupMembersDto.setFacilityId(facilityId);
    requisitionGroupMembersDto.setProgramId(programId);
    return requisitionGroupMembersDto;
  }
}
