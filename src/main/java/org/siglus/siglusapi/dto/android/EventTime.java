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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class EventTime implements Comparable<EventTime> {

  public static final Comparator<EventTime> ASCENDING = Comparator
      .comparing(EventTime::getOccurredDate)
      .thenComparing(EventTime::getRecordedAt);

  public static final Comparator<EventTime> DESCENDING = ASCENDING.reversed();

  private final LocalDate occurredDate;
  private final Instant recordedAt;

  @Override
  public int compareTo(@Nonnull EventTime o) {
    return ASCENDING.compare(this, o);
  }
}
