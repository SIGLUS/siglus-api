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

package org.openlmis.referencedata.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.referencedata.dto.VersionIdentityDto;
import org.openlmis.referencedata.exception.ValidationMessageException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class VersionableResourceSearchParams extends PageableSearchParams {

  private List<VersionIdentityDto> identities;

  public VersionableResourceSearchParams(List<VersionIdentityDto> identities, Integer page,
      Integer size) {
    super(page, size);
    this.identities = identities;
  }

  @JsonIgnore
  abstract String getInvalidVersionIdentityErrorMessage();

  /**
   * Retrieve identifiers as a set of id-versionId pairs.
   */
  @JsonIgnore
  public Set<Pair<UUID, Long>> getIdentityPairs() {
    List<VersionIdentityDto> list = Optional
        .ofNullable(identities)
        .orElse(Collections.emptyList());

    Set<Pair<UUID, Long>> set = Sets.newHashSet();

    for (VersionIdentityDto identity : list) {
      if (null == identity.getId() || null == identity.getVersionNumber()) {
        throw new ValidationMessageException(getInvalidVersionIdentityErrorMessage());
      }

      set.add(ImmutablePair.of(identity.getId(), identity.getVersionNumber()));
    }

    return set;
  }

}
