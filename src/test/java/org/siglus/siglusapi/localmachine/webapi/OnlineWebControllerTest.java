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

import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
  private EventStore eventStore;

  private static final UUID facilityId = UUID.randomUUID();

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
  public void confirmReceivedFromOnlineWeb() {
    // given
    AckRequest request = new AckRequest();
    Set<UUID> eventIds = new HashSet<>();
    eventIds.add(UUID.randomUUID());
    request.setEventIds(eventIds);
    // when
    onlineWebController.confirmReceived(request);
    // then
    verify(eventStore).confirmReceivedToOnlineWeb(request.getEventIds());
  }

  @Test
  public void reSycnDataFromOnlineWeb() {
    // given
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    // when
    onlineWebController.reSync(buildMachineToken(), httpServletResponse);
    // then
    verify(onlineWebService).reSyncData(facilityId, httpServletResponse);
  }

  private MachineToken buildMachineToken() {
    MachineToken machineToken = new MachineToken();
    machineToken.setFacilityId(facilityId);
    return machineToken;
  }
}
