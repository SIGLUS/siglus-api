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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SiglusDateHelper {

  public static final String DATE_TYPE_YEAR_MONTH_DATE = "yyyy-MM-dd";

  public static final String YEAR_MONTH_DATE = "yyyyMMdd";

  @Autowired
  private Clock clock;

  public LocalDate getCurrentDate() {
    return LocalDate.now(clock);
  }

  public static String formatDateTime(ZonedDateTime date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TYPE_YEAR_MONTH_DATE);
    return date.format(formatter);
  }

  public static String formatDate(Date date) {
    DateFormat dateFormat = new SimpleDateFormat(DATE_TYPE_YEAR_MONTH_DATE);
    return dateFormat.format(date);
  }

  public String getTodayDateStr() {
    LocalDate now = LocalDate.now(clock);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YEAR_MONTH_DATE);
    return now.format(formatter);
  }

  public String getCurrentMonthStr() {
    LocalDate now = LocalDate.now(clock);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
    return now.format(formatter);
  }
}
