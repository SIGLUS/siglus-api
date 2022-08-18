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

package org.siglus.siglusapi.localmachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.eventstore.EventRecord;
import org.springframework.beans.factory.annotation.Autowired;

public class LocalMachineTest extends LocalMachineBaseTest {
  @Autowired private LocalMachine localMachine;

  @Test
  public void canEmitEventSuccessfully() {
    UserDto mockedCurrentUser = new UserDto();
    mockedCurrentUser.setId(UUID.randomUUID());
    given(authenticationHelper.getCurrentUser()).willReturn(mockedCurrentUser);
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    String peeringId = "peeringId";
    String receiverId = "receiverId";

    localMachine.emitGroupEvent(peeringId, receiverId, payload);

    verify(eventRecordRepository).save(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getSenderId()).isEqualTo(mockedCurrentUser.getId().toString());
    assertThat(eventRecord.getGroupId()).isEqualTo(peeringId);
    assertThat(eventRecord.getReceiverId()).isEqualTo(receiverId);
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestEventPayload {
    private String id;
  }
}
