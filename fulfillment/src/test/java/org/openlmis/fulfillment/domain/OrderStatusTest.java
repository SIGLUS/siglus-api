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

package org.openlmis.fulfillment.domain;

import static java.util.Locale.ENGLISH;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

public class OrderStatusTest {

  @Test
  public void shouldFindStatus() throws Exception {
    Stream.of(OrderStatus.values())
        .forEach(val -> assertThat(OrderStatus.fromString(val.toString()), is(equalTo(val))));
    Stream.of(OrderStatus.values())
        .forEach(val -> assertThat(
            OrderStatus.fromString(val.toString().toLowerCase(ENGLISH)), is(equalTo(val))
        ));
    Stream.of(OrderStatus.values())
        .forEach(val -> assertThat(
            OrderStatus.fromString(val.toString().toUpperCase(ENGLISH)), is(equalTo(val))
        ));
  }

  @Test
  public void shouldNotFindStatus() throws Exception {
    assertThat(OrderStatus.fromString(null), is(nullValue()));
    assertThat(OrderStatus.fromString(""), is(nullValue()));
    assertThat(OrderStatus.fromString("     "), is(nullValue()));
    assertThat(OrderStatus.fromString(RandomStringUtils.random(10)), is(nullValue()));
  }
}
