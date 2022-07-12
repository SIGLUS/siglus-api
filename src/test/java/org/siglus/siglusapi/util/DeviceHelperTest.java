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

package org.siglus.siglusapi.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RunWith(MockitoJUnitRunner.class)
public class DeviceHelperTest {

  @InjectMocks
  private DeviceHelper deviceHelper;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private AppInfoRepository appInfoRepository;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  private final UUID facilityId = UUID.randomUUID();

  @Before
  public void prepare() {
    UserDto user = mock(UserDto.class);
    when(authHelper.getCurrentUser()).thenReturn(user);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    FacilityDto facility = mock(FacilityDto.class);
    when(facilityReferenceDataService.findOne((UUID) any())).thenReturn(facility);

    MockHttpServletRequest request = new MockHttpServletRequest("GET",
        "/api/siglusapi/android/me/facility");
    request.addHeader("UniqueId", "123456abc");
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);
  }

  @Test
  public void shouldReturnFalseWhenUniqueIdNotEqual() {
    //given
    AppInfo appInfo = mock(AppInfo.class);
    appInfo.setUniqueId("test");
    when(appInfoRepository.findByFacilityCode(anyString())).thenReturn(appInfo);

    //when
    boolean isSameDevice = deviceHelper.isSameDevice();

    //then
    assertFalse(isSameDevice);
  }

  @Test
  public void shouldReturnTrueWhenUniqueIdEqual() {
    //given
    AppInfo appInfo = new AppInfo();
    appInfo.setUniqueId("123456abc");
    when(appInfoRepository.findByFacilityCode(anyString())).thenReturn(appInfo);

    //when
    boolean isSameDevice = deviceHelper.isSameDevice();

    //then
    assertTrue(isSameDevice);
  }

  @Test
  public void shouldReturnTrueWhenAppInfoIsNull() {
    //given
    when(appInfoRepository.findByFacilityCode(anyString())).thenReturn(null);

    //when
    boolean isSameDevice = deviceHelper.isSameDevice();

    //then
    assertTrue(isSameDevice);
  }

}
















