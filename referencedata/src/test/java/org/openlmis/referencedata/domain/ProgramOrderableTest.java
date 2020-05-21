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

import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.junit.Test;

public class ProgramOrderableTest {

  private static final String IBUPROFEN = "ibuprofen";
  private static final String EACH = "each";

  private static Orderable ibuprofen;
  private static Program em;
  private static OrderableDisplayCategory testCat;

  {
    em = new Program("EM");
    testCat = OrderableDisplayCategory.createNew(Code.code("test"));
    ibuprofen = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 20, 10, false,
        UUID.randomUUID(), 1L);
  }

  @Test
  public void shouldBeEqualByProgramAndProduct() {
    ProgramOrderable ibuprofenInEm =
        ProgramOrderable.createNew(em, testCat, ibuprofen, CurrencyUnit.USD);

    Program emDupe = new Program("EM");
    OrderableDisplayCategory testCatDupe = OrderableDisplayCategory.createNew(Code.code("catdupe"));
    ProgramOrderable ibuprofenInEmDupe =
        ProgramOrderable.createNew(emDupe, testCatDupe, ibuprofen, CurrencyUnit.USD);

    assertEquals(ibuprofenInEm, ibuprofenInEmDupe);
    assertEquals(ibuprofenInEmDupe, ibuprofenInEm);
    assertEquals(ibuprofenInEm.hashCode(), ibuprofenInEmDupe.hashCode());
  }

  @Test
  public void isForProgramShouldBeTrue() {
    ProgramOrderable ibuprofenInEm =
        ProgramOrderable.createNew(em, testCat, ibuprofen, CurrencyUnit.USD);
    assertTrue(ibuprofenInEm.isForProgram(em));
  }

  @Test
  public void isForProgramShouldBeFalse() {
    ProgramOrderable ibuprofenInEm =
        ProgramOrderable.createNew(em, testCat, ibuprofen, CurrencyUnit.USD);

    assertFalse(ibuprofenInEm.isForProgram(null));
    assertFalse(ibuprofenInEm.isForProgram(new Program("fail")));
  }
}
