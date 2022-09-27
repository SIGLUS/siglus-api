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

package org.siglus.siglusapi.localmachine.auth;

import static org.mockito.Mockito.mock;

import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationArgumentResolverTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();
  @InjectMocks
  private AuthenticationArgumentResolver authenticationArgumentResolver;

  @Test
  public void shouldResolveArgument() {
    exception.expect(IllegalStateException.class);

    // when
    authenticationArgumentResolver.resolveArgument(
        null, null, mock(NativeWebRequest.class), null);
  }

  @SneakyThrows
  @Test
  public void shouldSupportsParameter() {
    // when
    authenticationArgumentResolver.supportsParameter(
        new MethodParameter(AuthenticationArgumentResolver.class.getMethod("supportsParameter",
            MethodParameter.class), 0));
  }
}
