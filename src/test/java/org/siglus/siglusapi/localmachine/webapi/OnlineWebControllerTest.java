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


package org.siglus.siglusapi.localmachine.webapi;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.util.Uuid5Generator;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.MasterDataEvent;
import org.siglus.siglusapi.localmachine.ShedLockFactory;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.auth.MachineToken;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.server.ActivationService;
import org.siglus.siglusapi.localmachine.server.OnlineWebService;
import org.springframework.mock.web.MockHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class OnlineWebControllerTest {

  @InjectMocks
  private OnlineWebController onlineWebController;

  @Mock
  private OnlineWebService onlineWebService;

  @Mock
  private ActivationService activationService;

  @Mock
  private ShedLockFactory lockFactory;

  @Mock
  private EventStore eventStore;

  @Mock
  private EventImporter eventImporter;

  @Mock
  private Machine machine;

  @Mock
  private ExternalEventDtoMapper externalEventDtoMapper;

  private static final UUID facilityId = UUID.randomUUID();

  @Test
  public void syncEventsToOnlineWeb() {
    // given
    SyncRequest request = new SyncRequest();
    // when
    onlineWebController.syncEvents(request);
    // then
    verify(eventImporter).importEvents(any());
  }

  @Test
  public void activateAgentFromOnlineWeb() {
    // given
    RemoteActivationRequest request = new RemoteActivationRequest();
    // when
    onlineWebController.activateAgent(request);
    // then
    verify(activationService).activate(request);
  }

  @Test
  public void exportMasterDataEventsFromOnlineWeb() {
    // given
    Long offsetId = 5L;
    MasterDataEvent masterDataEvent = MasterDataEvent.builder()
        .id(6L)
        .facilityId(facilityId)
        .build();
    when(eventStore.getMasterDataEvents(offsetId, facilityId)).thenReturn(Collections.singletonList(masterDataEvent));
    when(machine.getMachineId()).thenReturn(UUID.randomUUID());
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

    // when
    onlineWebController.exportMasterDataEvents(buildMachineToken(), offsetId);

    // then
    verify(eventStore).getMasterDataEvents(offsetId, facilityId);
    verify(externalEventDtoMapper).map(eventArgumentCaptor.capture());
    assertEquals(facilityId, eventArgumentCaptor.getValue().getReceiverId());
    assertEquals(6L, eventArgumentCaptor.getValue().getLocalSequenceNumber());
    assertEquals(Uuid5Generator.fromUtf8(String.valueOf(6)), eventArgumentCaptor.getValue().getId());
  }


  @Test
  public void exportPeeringEventsFromOnlineWeb() {
    // given
    when(eventStore.getEventsForReceiver(facilityId)).thenReturn(Collections.emptyList());

    // when
    onlineWebController.exportPeeringEvents(buildMachineToken());
    // then
    verify(eventStore).getEventsForReceiver(facilityId);
  }

  @Test
  public void reSyncDataFromOnlineWeb() {
    // given
    mockLock();
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    // when
    onlineWebController.reSync(buildMachineToken(), httpServletResponse);
    // then
    verify(onlineWebService).reSyncData(facilityId, httpServletResponse);
  }

  @Test
  public void exchangeAcksToOnlineWeb() {
    // given
    AckExchange request = new AckExchange();
    // when
    onlineWebController.exchangeAcks(request, buildMachineToken());
    // then
    verify(eventStore).routeAcks(any());
    verify(eventStore).getAcksForEventSender(buildMachineToken().getFacilityId());
  }

  @Test
  public void confirmAcksToOnlineWeb() {
    // given
    AckExchange request = new AckExchange();
    // when
    onlineWebController.confirmAcks(request);
    // then
    verify(eventStore).confirmAckShipped(any());
  }

  @Test
  public void reSyncMasterDataFromOnlineWeb() {
    // when
    onlineWebController.reSyncMasterData(buildMachineToken());
    // then
    verify(onlineWebService).reSyncMasterData(facilityId);
  }

  private MachineToken buildMachineToken() {
    MachineToken machineToken = new MachineToken();
    machineToken.setFacilityId(facilityId);
    return machineToken;
  }

  private void mockLock() {
    AutoClosableLock lock = new AutoClosableLock(Optional.ofNullable(mock(SimpleLock.class)));
    given(lockFactory.lock(anyString())).willReturn(lock);
  }
}
