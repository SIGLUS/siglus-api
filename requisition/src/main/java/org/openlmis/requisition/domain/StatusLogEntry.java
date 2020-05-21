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

package org.openlmis.requisition.domain;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.ToString;

/**
 * StatusLogEntry is used to encapsulate data (author and dateTime) associated with a
 * requisition's status change.
 */
@ToString
public class StatusLogEntry {
  private UUID authorId;
  private ZonedDateTime changeDate;

  public StatusLogEntry(){}

  public StatusLogEntry(UUID authorId, ZonedDateTime changeDate) {
    this.authorId = authorId;
    this.changeDate = changeDate;
  }

  public UUID getAuthorId() {
    return authorId;
  }

  public void setAuthorId(UUID authorId) {
    this.authorId = authorId;
  }

  public ZonedDateTime getChangeDate() {
    return changeDate;
  }

  public void setChangeDate(ZonedDateTime changeDate) {
    this.changeDate = changeDate;
  }
}
