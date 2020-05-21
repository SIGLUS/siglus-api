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

package org.openlmis.referencedata.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class OrderedDisplayValueTest {
  @Test
  public void equalsShouldTestDisplayNameAndOrder() {
    OrderedDisplayValue dispMac = new OrderedDisplayValue("mac", 0);
    OrderedDisplayValue dispMac1 = new OrderedDisplayValue("mac", 1);
    OrderedDisplayValue dispCheese = new OrderedDisplayValue("cheese", 0);

    assertNotEquals(dispMac, dispMac1);
    assertNotEquals(dispMac, dispCheese);
  }

  @Test
  public void equalsAndHashcodeShouldIgnoreCaseAndWhitespace() {
    OrderedDisplayValue value = new OrderedDisplayValue(" value ", 0);
    OrderedDisplayValue valueDupe = new OrderedDisplayValue("VaLue", 0);
    assertEquals(value, valueDupe);
    assertEquals(value.hashCode(), valueDupe.hashCode());
  }
}
