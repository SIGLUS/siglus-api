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

package org.openlmis.referencedata.repository;

import static org.openlmis.referencedata.util.messagekeys.UserMessageKeys.ERROR_HOME_FACILITY_ID_INVALID;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.UuidUtil;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class UserSearchParams {
  private Set<String> id;
  private String username;
  private String firstName;
  private String lastName;
  private String homeFacilityId;
  private Boolean active;
  private Map<String, String> extraData;

  /**
   * Constructor with id.
   */
  public UserSearchParams(Set<String> id) {
    this.id = id;
  }

  /**
   * Constructor with firstName.
   */
  public UserSearchParams(String firstName) {
    this.firstName = firstName;
  }

  /**
   * Check if all params are empty.
   */
  public boolean isEmpty() {
    if (!CollectionUtils.isEmpty(id)) {
      return false;
    }

    if (!MapUtils.isEmpty(extraData)) {
      return false;
    }

    return Stream
        .of(username, firstName, lastName, homeFacilityId, active, extraData)
        .allMatch(Objects::isNull);
  }

  /**
   * Gets a set of string id list parsed to UUIDs.
   */
  public Set<UUID> getIds() {
    return Optional
        .ofNullable(id)
        .orElse(Collections.emptySet())
        .stream()
        .map(UUID::fromString)
        .collect(Collectors.toSet());
  }

  /**
   * Gets home facility id parsed to UUID.
   *
   * @return home facility id parsed to UUID
   * @throws ValidationMessageException when home facility id is not valid UUID.
   */
  public UUID getHomeFacilityUuid() {
    if (this.homeFacilityId == null) {
      return null;
    }

    return UuidUtil
        .fromString(homeFacilityId)
        .orElseThrow(() -> new ValidationMessageException(ERROR_HOME_FACILITY_ID_INVALID));
  }

}
