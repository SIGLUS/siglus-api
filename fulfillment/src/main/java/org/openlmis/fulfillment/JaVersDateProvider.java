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

package org.openlmis.fulfillment;

import org.javers.common.date.DateProvider;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

/**
 * This class may be used by JaVers to retrieve the LocalDateTime that it associates with commits.
 * It is intended to be used, rather than JaVers' default DateProvider, so as to be explicit and
 * consistent with the use of UTC within JaVers' domain. (Otherwise, JaVers uses the default
 * system timezone, which may change, when constructing a LocalDateTime.)
 */
public class JaVersDateProvider implements DateProvider {
  private static final DateTimeZone DATE_TIME_ZONE = DateTimeZone.UTC;

  public LocalDateTime now() {
    return LocalDateTime.now(DATE_TIME_ZONE);
  }
}
