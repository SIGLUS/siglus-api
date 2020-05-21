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

package org.openlmis.referencedata.testbuilder;

import java.util.UUID;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightType;

public class RightDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String name;
  private RightType type;
  private String description;

  /**
   * Builds instance of {@link RightDataBuilder} with sample data.
   */
  public RightDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    name = "Role" + instanceNumber;
    type = RightType.GENERAL_ADMIN;
    description = "some description";
  }

  /**
   * Builds instance of {@link Right}.
   */
  public Right build() {
    Right right = Right.newRight(name, type);
    right.setId(id);
    right.setDescription(description);
    return right;
  }

  /**
   * Builds instance of {@link Right} without id.
   */
  public Right buildAsNew() {
    return this.withoutId().build();
  }

  public RightDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  public RightDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public RightDataBuilder withType(RightType type) {
    this.type = type;
    return this;
  }
}
