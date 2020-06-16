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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
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
  public void around() throws Throwable {
    HttpServletRequest request = new MockHttpServletRequest("GET",
        "/api/siglusapi/requisitions/initiate");
    ServletRequestAttributes attributes = new ServletWebRequest(request);
    RequestContextHolder.setRequestAttributes(attributes);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);

    when(joinPoint.getTarget()).thenReturn(controller);
    when(joinPoint.getSignature()).thenReturn(signature);

    webLogAspect.around(joinPoint);

    verify(joinPoint, times(1)).proceed();
  }
}