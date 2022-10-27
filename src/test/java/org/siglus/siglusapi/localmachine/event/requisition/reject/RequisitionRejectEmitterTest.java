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

package org.siglus.siglusapi.localmachine.event.requisition.reject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.EventPayloadCheckUtils;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.localmachine.event.requisition.web.reject.RequisitionRejectEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.reject.RequisitionRejectEvent;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.android.FileBasedTest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class RequisitionRejectEmitterTest extends FileBasedTest {

  @InjectMocks
  private RequisitionRejectEmitter requisitionRejectEmitter;
  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private EventPublisher eventPublisher;
  @Mock
  private EventCommonService baseEventCommonService;
  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();

  @Test
  public void shouldSuccessWhenEmitWithViaJson() throws IOException {
    // given
    String json = readFromFile("event.json");
    ObjectMapper objectMapper = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER;
    RequisitionRejectEvent req = objectMapper.readValue(json, RequisitionRejectEvent.class);
    // when
    int count = EventPayloadCheckUtils.checkEventSerializeChanges(req,
        RequisitionRejectEvent.class);
    // then
    assertThat(count).isZero();
  }

  @Test
  public void shouldSuccessWhenEmit() {
    // given
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    user.setId(userId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    // when
    requisitionRejectEmitter.emit(requisitionId, facilityId);
  }
}
