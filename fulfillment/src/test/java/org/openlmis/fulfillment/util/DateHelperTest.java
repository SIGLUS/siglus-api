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

package org.openlmis.fulfillment.util;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DateHelperTest {

  private static final ZoneId ZONE_ID = ZoneId.of("UTC");

  @Mock
  Clock clock;

  @InjectMocks
  DateHelper dateHelper;

  @Before
  public void setUp() {
    Mockito.when(clock.getZone()).thenReturn(ZONE_ID);
    Mockito.when(clock.instant()).thenReturn(Instant.now());
  }

  @Test
  public void shouldGetCurrentDate() {
    assertEquals(LocalDate.now(clock), dateHelper.getCurrentDate());
  }

  @Test
  public void shouldGetCurrentDateWithSystemTimeZone() {
    assertEquals(ZonedDateTime.now(clock), dateHelper.getCurrentDateTimeWithSystemZone());
  }
}
