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

package org.openlmis.requisition.utils;

import java.util.Map;
import org.openlmis.requisition.domain.StatusLogEntry;
import org.openlmis.requisition.dto.StatusChangeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StatusChangeHelper {

  private static final Logger logger = LoggerFactory.getLogger(StatusChangeHelper.class);

  private StatusChangeHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds or updates the given status change to the given maps status log entries. It will only
   * update the entry if the existing one has later date.
   *
   * @param statusLogEntries  the map of status go entries.
   * @param statusChange  the status change
   */
  public static void addOrUpdate(Map<String, StatusLogEntry> statusLogEntries,
                                 StatusChangeDto statusChange) {
    logger.debug("addOrUpdate method");
    logger.debug("statusLogEntries {}", statusLogEntries);
    logger.debug("statusChange {}", statusChange);

    if (null == statusChange
        || null == statusChange.getStatus()
        || null == statusChange.getCreatedDate()) {
      return;
    }

    StatusLogEntry existing = statusLogEntries.get(statusChange.getStatus().toString());

    if (null == existing || existing.getChangeDate().isBefore(statusChange.getCreatedDate())) {
      StatusLogEntry entry = new StatusLogEntry(statusChange.getAuthorId(),
          statusChange.getCreatedDate());
      statusLogEntries.put(statusChange.getStatus().toString(), entry);
    }
  }

}
