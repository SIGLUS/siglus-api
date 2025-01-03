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

package org.siglus.siglusapi.localmachine.repository;

import java.util.HashMap;
import java.util.Map;

public class EventHistorySql {
  private static final String WHERE_FACILITY_ID =
      "where e.receiverid = '@@' and e.groupid is not null and e.localreplayed = true "
          + "and e.syncedtime >= now() - interval '6 months'";
  public static final String EVENTS = "localmachine.events";
  public static final String EVENTS_QUERY =
      "select * from localmachine.events e "
      + WHERE_FACILITY_ID;

  public static Map<String, String> getEventSql() {
    Map<String, String> eventSql = new HashMap<>();
    eventSql.put(EVENTS, EVENTS_QUERY);

    return eventSql;
  }
}
