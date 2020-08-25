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

package org.siglus.common.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SiglusDateHelper {

  @Autowired
  private Clock clock;

  public LocalDate getCurrentDate() {
    return LocalDate.now(clock);
  }

  public static String formatDateTime(ZonedDateTime date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return date.format(formatter);
  }

  public String getYesterdayDateStr() {
    LocalDate now = LocalDate.now(clock);
    LocalDate yesterday = now.minusDays(1);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    return yesterday.format(formatter);
  }

  public String getCurrentMonthStr() {
    LocalDate now = LocalDate.now(clock);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
    return now.format(formatter);
  }
}
