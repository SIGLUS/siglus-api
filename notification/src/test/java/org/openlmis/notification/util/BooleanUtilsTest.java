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

package org.openlmis.notification.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import org.junit.Test;

public class BooleanUtilsTest {

  @Test
  public void shouldConvertBooleanValuesToBoolean() {
    isTrue(true);
    isFalse(false);
  }

  @Test
  public void shouldConvertStringValuesToBoolean() {
    isTrue("true", "TRUE", "TrUe");
    isFalse("false", "FALSE", "FaLsE");
    isFalse("", "   ", "abcde");
  }

  @Test
  public void shouldConvertNumberValuesToBoolean() {
    isTrue((byte) 1, (short) 10, 100, 1000L, 1.0, 10.13F);
    isFalse((byte) 0, (short) -10, -100, -1000L, -1.0, -10.13F);
  }

  @Test
  public void shouldConvertIncorrectValuesToBoolean() {
    isFalse(null, new Object());
  }

  private void isFalse(Object... values) {
    Arrays.stream(values).forEach(value -> check(false, value));
  }

  private void isTrue(Object... values) {
    Arrays.stream(values).forEach(value -> check(true, value));
  }

  private void check(boolean expected, Object value) {
    assertThat(BooleanUtils.toBoolean(value), is(expected));
  }

}
