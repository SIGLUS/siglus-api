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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.referencedata.exception.ValidationMessageException;

public class GtinTest {

  private Gtin gtin1 = new Gtin("11111111111111");
  private Gtin gtin2 = new Gtin("22222222");

  @Test
  public void shouldBeEqualByGtin() {
    Gtin gtin1Duplicate = new Gtin(gtin1.getGtin());
    assertTrue(gtin1.equals(gtin1Duplicate));
    assertTrue(gtin1Duplicate.equals(gtin1));
  }

  @Test
  public void shouldEnforceHashGtin() {
    Gtin gtin1Duplicate = new Gtin(gtin1.getGtin());
    assertEquals(gtin1.hashCode(), gtin1Duplicate.hashCode());
    assertNotEquals(gtin1.hashCode(), gtin2.hashCode());
  }

  @Test
  public void shouldNotBeEqual() {
    assertNotEquals(gtin1, gtin2);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(Gtin.class)
        .verify();
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfGtinIsNotNumeric() {
    new Gtin("ab12345678ba");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfGtinIsTooShort() {
    new Gtin("1234567");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfGtinIsTooLong() {
    new Gtin("123456789012345");
  }
}
