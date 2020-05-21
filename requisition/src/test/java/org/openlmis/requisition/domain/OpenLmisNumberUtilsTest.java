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

package org.openlmis.requisition.domain;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.openlmis.requisition.domain.OpenLmisNumberUtils.zeroIfNull;

import java.math.BigDecimal;
import org.junit.Test;

public class OpenLmisNumberUtilsTest {

  @Test
  public void shouldReturnZeroIfValueIsNull() throws Exception {
    assertThat(zeroIfNull((Integer) null), is(equalTo(0)));
    assertThat(zeroIfNull((Long) null), is(equalTo(0L)));
    assertThat(zeroIfNull((BigDecimal) null), is(equalTo(BigDecimal.ZERO)));
  }

  @Test
  public void shouldReturnValueIfItIsNotNull() throws Exception {
    assertThat(zeroIfNull(10), is(equalTo(10)));
    assertThat(zeroIfNull(12345678901L), is(equalTo(12345678901L)));
    assertThat(zeroIfNull(new BigDecimal("7")), is(equalTo(new BigDecimal("7"))));
  }
}
