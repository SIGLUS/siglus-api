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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import org.junit.Test;
import org.openlmis.referencedata.exception.ValidationMessageException;

public class CommodityTypeTest {
  private static final String CLASSIFICATION_SYS = "cSys";
  private static final String CLASSIFICATION_SYS_ID = "cSysId";

  private static CommodityType ibuprofen;

  private CommodityType child1;
  private CommodityType child2;
  private CommodityType grandChild1;
  private CommodityType grandChild2;
  private CommodityType grandChild3;

  static {
    ibuprofen =
        new CommodityType("ibuprofen", CLASSIFICATION_SYS, CLASSIFICATION_SYS_ID, null, null);
    ibuprofen.setChildren(new ArrayList<>());
  }

  @Test
  public void testEqualsAndHashCode() {
    assertTrue(ibuprofen.equals(ibuprofen));

    CommodityType ibuprofenDupe =
        new CommodityType("ibuprofen", CLASSIFICATION_SYS, CLASSIFICATION_SYS_ID, null, null);
    assertEquals(ibuprofen.hashCode(), ibuprofenDupe.hashCode());
  }

  @Test
  public void shouldAssignParent() {
    setUpHierarchy();

    assertEquals(ibuprofen, child1.getParent());
    assertEquals(ibuprofen, child2.getParent());
    assertEquals(child1, grandChild1.getParent());
    assertEquals(child1, grandChild2.getParent());
    assertEquals(child2, grandChild3.getParent());
    assertEquals(asList(child1, child2), ibuprofen.getChildren());
    assertEquals(asList(grandChild1, grandChild2), child1.getChildren());
    assertEquals(singletonList(grandChild3), child2.getChildren());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotAcceptDirectChildrenAsParent() {
    setUpHierarchy();
    ibuprofen.assignParent(child1);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotAcceptNonDirectDescendantsAsParent() {
    setUpHierarchy();
    ibuprofen.assignParent(grandChild1);
  }

  private void setUpHierarchy() {
    child1 = generateCommodityType("child1");
    child2 = generateCommodityType("child2");
    grandChild1 = generateCommodityType("grand child1");
    grandChild2 = generateCommodityType("grand child1");
    grandChild3 = generateCommodityType("grand child3");

    child1.assignParent(ibuprofen);
    child2.assignParent(ibuprofen);

    grandChild1.assignParent(child1);
    grandChild2.assignParent(child1);
    grandChild3.assignParent(child2);
  }

  private CommodityType generateCommodityType(String productName) {
    CommodityType commodityType = new CommodityType(productName, "CS", "CID", null, null);
    commodityType.setChildren(new ArrayList<>());
    return commodityType;
  }
}