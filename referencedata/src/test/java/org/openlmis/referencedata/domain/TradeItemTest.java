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

import static junit.framework.Assert.assertFalse;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.Test;

public class TradeItemTest {

  private static final String IBUPROFEN_NAME = "Ibuprofen";
  private static final String ADVIL_NAME = "Advil";
  private static final String CLASSIFICATION_SYSTEM = "some-system";
  private static final String CLASSIFICATION_ID = "some-id";
  private static final String CID1 = "CID1";

  private CommodityType ibuprofen = new CommodityType(IBUPROFEN_NAME, CLASSIFICATION_SYSTEM,
      CLASSIFICATION_ID, null, null);

  private TradeItem advil;

  @Test
  public void canFulfillWithClassification() throws Exception {
    advil = new TradeItem(ADVIL_NAME, new ArrayList<>());
    assertFalse(advil.canFulfill(ibuprofen));

    TradeItemClassification classification = new TradeItemClassification();
    classification.setClassificationSystem(CLASSIFICATION_SYSTEM);
    classification.setClassificationId(CLASSIFICATION_ID);
    advil = new TradeItem(ADVIL_NAME, Collections.singletonList(classification));
    assertTrue(advil.canFulfill(ibuprofen));
  }

  @Test
  public void shouldAssignClassifications() {
    advil = new TradeItem(ADVIL_NAME, new ArrayList<>());

    advil.assignCommodityType("csys", CID1);
    advil.assignCommodityType("test sys", "ID2");

    assertThat(advil.getClassifications(), hasSize(2));
    TradeItemClassification classification = advil.findClassificationById(CID1);
    assertNotNull(classification);
    assertThat(classification.getClassificationSystem(), is("csys"));

    advil.assignCommodityType("csys changed", CID1);

    assertThat(advil.getClassifications(), hasSize(2));
    classification = advil.findClassificationById(CID1);
    assertNotNull(classification);
    assertThat(classification.getClassificationSystem(), is("csys changed"));
  }
}
