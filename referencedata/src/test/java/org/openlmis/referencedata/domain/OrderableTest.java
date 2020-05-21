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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.junit.Test;

@SuppressWarnings("PMD.TooManyMethods")
public class OrderableTest {
  private static final String IBUPROFEN = "ibuprofen";
  private static final String EACH = "each";

  private static Orderable ibuprofen;

  @Test
  public void shouldGetProgramOrderableForProgram() {
    Program em = new Program("EssMed");
    Program fp = new Program("FamPla");

    OrderableDisplayCategory testCat = OrderableDisplayCategory.createNew(Code.code("testcat"));
    ProgramOrderable ibuprofenInEm =
        ProgramOrderable.createNew(em, testCat, null, CurrencyUnit.USD);
    ProgramOrderable ibuprofenInFp =
        ProgramOrderable.createNew(fp, testCat, null, CurrencyUnit.USD);
    ibuprofen = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 5, false,
        UUID.randomUUID(), 1L);
    ibuprofen.setProgramOrderables(Arrays.asList(ibuprofenInEm, ibuprofenInFp));
    ibuprofenInEm.setProduct(ibuprofen);
    ibuprofenInFp.setProduct(ibuprofen);

    assertEquals(ibuprofenInEm, ibuprofen.getProgramOrderable(em));
    assertEquals(ibuprofenInFp, ibuprofen.getProgramOrderable(fp));
  }

  @Test
  public void shouldCalculatePacksToOrderWhenPackRoundingThresholdIsSmallerThanRemainder() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 4,
        false, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(26);

    assertEquals(3, packsToOrder);
  }

  @Test
  public void shouldCalculatePacksToOrderWhenPackRoundingThresholdIsGreaterThanRemainder() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        false, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(26);

    assertEquals(2, packsToOrder);
  }

  @Test
  public void shouldCalculatePacksToOrderWhenCanRoundToZero() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        true, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(6);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldCalculatePacksToOrderWhenCanNotRoundToZero() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        false, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(6);

    assertEquals(1, packsToOrder);
  }

  @Test
  public void shouldReturnZeroPacksToOrderIfNetContentIsZero() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 0, 7,
        false, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(6);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldReturnZeroPacksToOrderIfOrderQuantityIsZero() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        false, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(0);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldReturnZeroPackToOrderIfOrderQuantityIsOneAndRoundToZeroTrueWithNetContentTen() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        true, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(1);

    assertEquals(0, packsToOrder);
  }

  @Test
  public void shouldNotRoundUpWhenEqualToThreshold() {
    final int netContent = 100;
    final int roundingThreshold = 50;

    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH),
        netContent, roundingThreshold, false, UUID.randomUUID(), 1L);

    long packsToOrder = product.packsToOrder(250);
    assertEquals(2, packsToOrder);

    packsToOrder = product.packsToOrder(251);
    assertEquals(3, packsToOrder);
  }

  @Test
  public void shouldIndicateResourceWasModifiedIfCheckedDateIsNull() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        true, UUID.randomUUID(), 1L);

    assertTrue(product.wasModifiedSince(null));
  }

  @Test
  public void shouldIndicateResourceWasModifiedIfDateIsBeforeModificationDate() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        true, UUID.randomUUID(), 1L);

    ZonedDateTime now = ZonedDateTime.now();
    product.setLastUpdated(now);

    assertTrue(product.wasModifiedSince(now.minusMinutes(1)));
  }

  @Test
  public void shouldIndicateResourceWasNotModifiedIfDatesAreTheSame() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        true, UUID.randomUUID(), 1L);

    ZonedDateTime now = ZonedDateTime.now();
    product.setLastUpdated(now);

    assertFalse(product.wasModifiedSince(now));
  }

  @Test
  public void shouldIndicateResourceWasNotModifiedIfDateIsAfterModificationDate() {
    Orderable product = new Orderable(Code.code(IBUPROFEN), Dispensable.createNew(EACH), 10, 7,
        true, UUID.randomUUID(), 1L);

    ZonedDateTime now = ZonedDateTime.now();
    product.setLastUpdated(now);

    assertFalse(product.wasModifiedSince(now.plusDays(1)));
  }
}
