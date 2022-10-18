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

package org.siglus.siglusapi.interceptor;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CURRENT_IS_NOT_LOCATION_MANAGEMENT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CURRENT_IS_NOT_STOCK_MANAGEMENT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_WEB_USER;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.siglus.siglusapi.util.LocalMachineHelper;

@RunWith(MockitoJUnitRunner.class)
public class WebApiCheckAspectTest {

  @InjectMocks
  private WebApiCheckAspect webApiCheckAspect;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private AndroidHelper androidHelper;

  @Mock
  private LocalMachineHelper localMachineHelper;

  @Mock
  private FacilityConfigHelper facilityConfigHelper;

  @Mock
  private Machine machine;

  @Test
  public void shouldThrowPermissionMessageExceptionWhenFacilityIsLocalMachine() {
    // then
    exception.expect(PermissionMessageException.class);
    exception.expectMessage(containsString(ERROR_NOT_WEB_USER));

    // given
    when(localMachineHelper.isLocalMachine()).thenReturn(true);
    when(machine.isOnlineWeb()).thenReturn(true);

    // when
    webApiCheckAspect.checkWebWriteApi();
  }

  @Test
  public void shouldThrowPermissionMessageExceptionWhenFacilityIsAndroid() {
    // then
    exception.expect(PermissionMessageException.class);
    exception.expectMessage(containsString(ERROR_NOT_WEB_USER));

    // given
    when(localMachineHelper.isLocalMachine()).thenReturn(false);
    when(androidHelper.isAndroid()).thenReturn(true);
    when(machine.isOnlineWeb()).thenReturn(true);

    // when
    webApiCheckAspect.checkWebWriteApi();
  }

  @Test
  public void shouldThrowBusinessDataExceptionWhenCheckWithLocation() {
    // then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_CURRENT_IS_NOT_LOCATION_MANAGEMENT));

    // given
    when(localMachineHelper.isLocalMachine()).thenReturn(false);
    when(machine.isOnlineWeb()).thenReturn(true);
    when(facilityConfigHelper.isStockManagement()).thenReturn(true);

    // when
    webApiCheckAspect.checkWithLocationApi();
  }

  @Test
  public void shouldThrowBusinessDataExceptionWhenCheckWithoutLocation() {
    // then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_CURRENT_IS_NOT_STOCK_MANAGEMENT));

    // given
    when(localMachineHelper.isLocalMachine()).thenReturn(false);
    when(machine.isOnlineWeb()).thenReturn(true);
    when(facilityConfigHelper.isLocationManagement()).thenReturn(true);

    // when
    webApiCheckAspect.checkWithoutLocationApi();
  }

}