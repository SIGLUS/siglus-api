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

package org.openlmis.notification.testutils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.openlmis.notification.service.referencedata.RightDto;

public class RightDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String name;
  private String type;
  private String description;
  private Set<RightDto> attachments;

  /**
   * Builder for {@link RightDataBuilder}.
   */
  public RightDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    name = "Right " + instanceNumber;
    type = "GENERAL_ADMIN";
    description = "description";
    attachments = Collections.emptySet();
  }

  /**
   * Builds instance of {@link RightDto}.
   */
  public RightDto build() {
    RightDto right = new RightDto(name, type, description, attachments);
    right.setId(id);
    return right;
  }
}
