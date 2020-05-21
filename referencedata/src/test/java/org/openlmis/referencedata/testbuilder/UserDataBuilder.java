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

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.ExtraDataEntity;
import org.openlmis.referencedata.domain.RightAssignment;
import org.openlmis.referencedata.domain.RoleAssignment;
import org.openlmis.referencedata.domain.User;

@SuppressWarnings("PMD.TooManyMethods")
public class UserDataBuilder {
  private static int instanceNumber = 0;

  private UUID id = UUID.randomUUID();
  private String username;
  private String firstName = "Admin";
  private String lastName = "User";
  private String jobTitle = "Junior Tester";
  private String timezone = "UTC";
  private UUID homeFacilityId = null;
  private boolean active = true;
  private Set<RoleAssignment> roleAssignments = new HashSet<>();
  private Map<String, Object> extraData = new HashMap<>();
  private Set<RightAssignment> rightAssignments = new HashSet<>();


  /**
   * Builds instance of {@link UserDataBuilder} with sample data.
   */
  public UserDataBuilder() {
    instanceNumber++;

    username = "admin" + instanceNumber;
  }

  public UserDataBuilder withHomeFacilityId(UUID homeFacilityId) {
    this.homeFacilityId = homeFacilityId;
    return this;
  }

  /**
   * Add single extra data to the collection.
   */
  public UserDataBuilder withExtraData(String key, String value) {
    this.extraData = Optional.ofNullable(extraData).orElse(Maps.newHashMap());
    this.extraData.put(key, value);
    return this;
  }

  public UserDataBuilder withUsername(String username) {
    this.username = username;
    return this;
  }

  public UserDataBuilder withFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public UserDataBuilder withLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public UserDataBuilder withActive(boolean active) {
    this.active = active;
    return this;
  }

  public UserDataBuilder withTimeZone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Builds instance of {@link User} without id.
   */
  public User buildAsNew() {
    return new User(
        username, firstName, lastName, jobTitle, timezone, homeFacilityId,
        active, roleAssignments, new ExtraDataEntity(extraData),
        rightAssignments
    );
  }

  /**
   * Builds instance of {@link User}.
   */
  public User build() {
    User user = buildAsNew();
    user.setId(id);

    return user;
  }

}
