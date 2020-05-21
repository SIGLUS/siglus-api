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

package org.openlmis.referencedata.util;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.repository.UserSearchParams;

public class UserSearchParamsDataBuilder {

  private Set<String> id = emptySet();
  private String username = "someName";
  private String firstName = "firstName";
  private String lastName = "lastName";
  private String homeFacilityId = UUID.randomUUID().toString();
  private Boolean active = true;
  private Map<String, String> extraData = emptyMap();

  /**
   * Builds new instance of {@link UserSearchParams} with test data.
   */
  public UserSearchParams build() {
    return new UserSearchParams(id, username, firstName, lastName, homeFacilityId,
        active, extraData);
  }

  /**
   * Sets the {@code null} value for all fields.
   */
  public UserSearchParamsDataBuilder asEmpty() {
    id = null;
    username = null;
    firstName = null;
    lastName = null;
    homeFacilityId = null;
    active = null;
    extraData = null;

    return this;
  }

  public UserSearchParamsDataBuilder withId(Set<String> id) {
    this.id = id;
    return this;
  }

  public UserSearchParamsDataBuilder withUsername(String username) {
    this.username = username;
    return this;
  }

  public UserSearchParamsDataBuilder withFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public UserSearchParamsDataBuilder withLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public UserSearchParamsDataBuilder withHomeFacilityId(UUID homeFacilityId) {
    this.homeFacilityId = homeFacilityId.toString();
    return this;
  }

  public UserSearchParamsDataBuilder withActive(Boolean active) {
    this.active = active;
    return this;
  }

  public UserSearchParamsDataBuilder withExtraData(Map<String, String> extraData) {
    this.extraData = extraData;
    return this;
  }
}
