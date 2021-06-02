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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.web.SiglusRequisitionController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

@RunWith(MockitoJUnitRunner.class)
public class WebLogAspectTest {

  @Mock
  private SiglusRequisitionController controller;

  private WebLogAspect webLogAspect = new WebLogAspect();

  @Before
  public void setUp() {
    webLogAspect = new WebLogAspect();
  }

  @Test
  public void should_record_web_log_when_request_from_website() throws Throwable {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET",
        "/api/siglusapi/requisitions/initiate");
    ServletRequestAttributes attributes = new ServletWebRequest(request);
    RequestContextHolder.setRequestAttributes(attributes);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);

    // when
    when(joinPoint.getTarget()).thenReturn(controller);
    when(joinPoint.getSignature()).thenReturn(signature);
    webLogAspect.around(joinPoint);

    // then
    verify(joinPoint).proceed();
  }

  @Test
  public void should_record_android_log_when_request_from_android() throws Throwable {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("GET",
        "/api/siglusapi/android/requisitions");
    request.addHeader("UserName", "CS_Moine_Role1");
    request.addHeader("FacilityCode", "01080904");
    request.addHeader("FacilityName", "Centro de Saude de Chiunze");
    request.addHeader("UniqueId", "ac36c07a09f2fdca");
    request.addHeader("DeviceInfo",
        "OS: 9 Model: google Android SDK built for x86versionCode:87.1");
    request.addHeader("VersionCode", "87");
    request.addHeader("AndroidSDKVersion", "28");
    ServletRequestAttributes attributes = new ServletWebRequest(request);
    RequestContextHolder.setRequestAttributes(attributes);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getTarget()).thenReturn(controller);
    when(joinPoint.getSignature()).thenReturn(signature);

    // when
    webLogAspect.around(joinPoint);

    // then
    verify(joinPoint).proceed();
  }
}
