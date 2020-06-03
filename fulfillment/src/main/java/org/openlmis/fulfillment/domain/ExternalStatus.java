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

package org.openlmis.fulfillment.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum ExternalStatus {
  INITIATED(1),
  REJECTED(1),
  SUBMITTED(2),
  AUTHORIZED(3),
  IN_APPROVAL(4),
  APPROVED(5),
  RELEASED(6),
  SKIPPED(-1);

  private int value;

  ExternalStatus(int value) {
    this.value = value;
  }

  @JsonIgnore
  public boolean isPreAuthorize() {
    return value == 1 || value == 2;
  }

  @JsonIgnore
  public boolean isPostSubmitted() {
    return value >= 2;
  }

}