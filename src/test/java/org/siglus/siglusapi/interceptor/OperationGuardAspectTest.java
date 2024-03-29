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

import org.junit.Test;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

public class OperationGuardAspectTest {
  public static final String SECRET = "secret";

  @Test(expected = AuthenticationException.class)
  public void shouldThrowWhenAssertAuthorizedGivenSecretNotMatch() {
    new OperationGuardAspect().assertAuthorized(SECRET);
  }

  @Test
  public void shouldNotThrowWhenAssertAuthorizedGivenSecretDoesMatch() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    OperationGuardAspect operationGuardAspect = new OperationGuardAspect();
    ReflectionTestUtils.setField(operationGuardAspect, "encodedSecret", encoder.encode(SECRET));

    operationGuardAspect.assertAuthorized(SECRET);
  }
}
