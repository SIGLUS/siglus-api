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

package org.siglus.siglusapi.util;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.ProcessingPeriod;

@RunWith(MockitoJUnitRunner.class)
public class PeriodUtilTest {

  @Test
  public void shouldReturnTrueWhenDateInPeriod() {
    // given
    LocalDate now = LocalDate.now();
    ProcessingPeriod period = new ProcessingPeriod();
    period.setStartDate(now);
    period.setEndDate(now.plusDays(1));

    // when
    boolean actualResponse = PeriodUtil.isDateInPeriod(period, now);

    // then
    assertEquals(Boolean.TRUE, actualResponse);
  }

  @Test
  public void shouldReturnFalseWhenDateNotInPeriod() {
    // given
    LocalDate now = LocalDate.now();
    ProcessingPeriod period = new ProcessingPeriod();
    period.setStartDate(now.plusDays(1));
    period.setEndDate(now.plusDays(2));

    // when
    boolean actualResponse = PeriodUtil.isDateInPeriod(period, now);

    // then
    assertEquals(Boolean.FALSE, actualResponse);
  }

}