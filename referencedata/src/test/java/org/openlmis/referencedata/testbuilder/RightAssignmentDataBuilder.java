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
import org.openlmis.referencedata.domain.RightAssignment;
import org.openlmis.referencedata.domain.User;

public class RightAssignmentDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private User user;
  private String rightName;
  private UUID facilityId;
  private UUID programId;

  /**
   * Returns instance of {@link RightAssignmentDataBuilder} with sample data.
   */
  public RightAssignmentDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    user = new UserDataBuilder().build();
    rightName = "Right" + instanceNumber;
    programId = null;
    facilityId = null;
  }

  /**
   * Builds instance of {@link RightAssignment} without id field.
   */
  public RightAssignment build() {
    RightAssignment rightAssignment = buildAsNew();
    rightAssignment.setId(id);

    return rightAssignment;
  }

  public RightAssignment buildAsNew() {
    return new RightAssignment(user, rightName, facilityId, programId);
  }

  public RightAssignmentDataBuilder withUser(User user) {
    this.user = user;
    return this;
  }

  public RightAssignmentDataBuilder withRightName(String name) {
    this.rightName = name;
    return this;
  }

  public RightAssignmentDataBuilder withProgram(UUID programId) {
    this.programId = programId;
    return this;
  }

  public RightAssignmentDataBuilder withFacility(UUID facilityId) {
    this.facilityId = facilityId;
    return this;
  }

}
