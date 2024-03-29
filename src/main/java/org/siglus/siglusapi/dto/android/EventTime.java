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

package org.siglus.siglusapi.dto.android;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;

@Data
public class EventTime implements Comparable<EventTime> {

  public static final Comparator<EventTime> ASCENDING = comparing(EventTime::getOccurredDate)
      .thenComparing(EventTime::getRecordedAt, nullsFirst(naturalOrder()));

  public static final Comparator<EventTime> DESCENDING = ASCENDING.reversed();

  private final LocalDate occurredDate;
  @Nullable
  private final Instant recordedAt;

  private EventTime(LocalDate occurredDate, @Nullable Instant recordedAt) {
    this.occurredDate = occurredDate;
    this.recordedAt = recordedAt;
  }

  public static EventTime fromRequest(LocalDate occurredDate, Instant recordedAt) {
    return new EventTime(occurredDate, recordedAt);
  }

  public static EventTime fromDatabase(Date occurredDate, @Nullable String recordedAtStr, Timestamp processedAtTs) {
    Instant recordedAt;
    if (recordedAtStr != null) {
      recordedAt = Instant.parse(recordedAtStr);
    } else {
      recordedAt = processedAtTs.toInstant();
    }
    return new EventTime(occurredDate.toLocalDate(), recordedAt);
  }

  @Override
  public int compareTo(@Nonnull EventTime o) {
    return ASCENDING.compare(this, o);
  }
}
