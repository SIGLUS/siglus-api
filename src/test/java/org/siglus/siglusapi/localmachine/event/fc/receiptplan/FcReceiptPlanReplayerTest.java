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

package org.siglus.siglusapi.localmachine.event.fc.receiptplan;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.service.fc.FcReceiptPlanService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;

@RunWith(MockitoJUnitRunner.class)
public class FcReceiptPlanReplayerTest {

  @InjectMocks
  private FcReceiptPlanReplayer replayer;

  @Mock
  private FcReceiptPlanService fcReceiptPlanService;

  @Mock
  private SiglusSimulateUserAuthHelper simulateUserAuthHelper;

  @Test
  public void shouldReplayerSuccessfully() {
    // given
    UserDto userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    when(fcReceiptPlanService.getFcUserInfo()).thenReturn(userDto);
    when(simulateUserAuthHelper.simulateNewUserBefore()).thenReturn(null);
    doNothing().when(simulateUserAuthHelper).simulateNewUserAuth(userDto.getId());
    doNothing().when(simulateUserAuthHelper).simulateNewUserAfter(null);
    ReceiptPlanDto receiptPlanDto = new ReceiptPlanDto();
    FcReceiptPlanEvent event = new FcReceiptPlanEvent();
    event.setReceiptPlanDto(receiptPlanDto);

    // when
    replayer.replay(event);

    // then
    verify(fcReceiptPlanService, times(1)).updateRequisition(receiptPlanDto, userDto);
  }
}