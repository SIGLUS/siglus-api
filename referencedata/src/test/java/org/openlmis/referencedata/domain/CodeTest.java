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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CodeTest {

  private Code codeA;
  private Code codeB;

  {
    codeA = Code.code("codeA");
    codeB = Code.code("codeB");
  }

  @Test
  public void shouldUseBlankProductCodeIfNull() {
    Code code = Code.code(null);
    assertEquals(Code.code(""), code);
  }

  @Test
  public void shouldBeEqualByCode() {
    Code codeADupe = Code.code("codeA");
    assertTrue(codeA.equals(codeADupe));
    assertTrue(codeADupe.equals(codeA));
  }

  @Test
  public void shouldBeEqualIgnoreCase() {
    Code codeADupe = Code.code("codea");
    assertEquals(codeA, codeADupe);
  }

  @Test
  public void shouldBeEqualIgnoreSpace() {
    Code codeADupe = Code.code("code A");
    assertEquals(codeA, codeADupe);
    assertEquals(codeADupe, codeA);
  }

  @Test
  public void shouldEnforceHashCode() {
    Code codeADupe = Code.code("code a");
    assertEquals(codeA.hashCode(), codeADupe.hashCode());

    assertNotEquals(codeA.hashCode(), codeB.hashCode());
  }

  @Test
  public void shouldNotBeEqual() {
    assertNotEquals(codeA, codeB);
  }
}
