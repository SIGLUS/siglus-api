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

package org.siglus.siglusapi.domain;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum NotificationStatus {
  REJECTED,
  SUBMITTED,
  AUTHORIZED,
  IN_APPROVAL,
  APPROVED,
  RELEASED_WITHOUT_ORDER,
  ORDERED,
  SHIPPED,
  RECEIVED;

  public static Collection<NotificationStatus> requisitionStatuses() {
    return Stream.of(NotificationStatus.values())
        .filter(NotificationStatus::isRequisitionPeriod)
        .collect(Collectors.toList());
  }

  public boolean isRequisitionPeriod() {
    return this == REJECTED || this == SUBMITTED || this == AUTHORIZED || this == IN_APPROVAL
        || this == APPROVED;
  }

}
