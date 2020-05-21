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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ContainerDispensableTest {

  private Dispensable containerDispensable;

  @Before
  public void setUp() {
    containerDispensable = new ContainerDispensable("5ml");
  }

  @Test
  public void equalsShouldReturnTrueIfSizeCodeMatches() {
    Dispensable other = new ContainerDispensable("5ml");
    assertTrue(containerDispensable.equals(other));
  }

  @Test
  public void equalsShouldReturnTrueIfCaseDoesNotMatch() {
    Dispensable other = new ContainerDispensable("5ML");
    assertTrue(containerDispensable.equals(other));
  }

  @Test
  public void equalsShouldReturnFalseIfNull() {
    assertFalse(containerDispensable.equals(null));
  }

  @Test
  public void equalsShouldReturnFalseIfClassDoesNotMatch() {
    Dispensable other = new DefaultDispensable("each");
    assertFalse(containerDispensable.equals(other));
  }

  @Test
  public void equalsShouldReturnFalseIfSizeCodeDoesNotMatch() {
    Dispensable other = new ContainerDispensable("0.5ml");
    assertFalse(containerDispensable.equals(other));
  }

  @Test
  public void toStringShouldReturnSizeCode() {
    assertEquals("5ml", containerDispensable.toString());
  }
}
