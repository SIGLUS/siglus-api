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

package org.openlmis.requisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class SupplyPartnerAssociationDto extends BaseDto {
  private ObjectReferenceDto program;
  private ObjectReferenceDto supervisoryNode;
  private List<ObjectReferenceDto> facilities;
  private List<ObjectReferenceDto> orderables;

  /**
   * Retrieves UUID of program from object reference. If the object is null, the null value will be
   * returned.
   *
   * @return UUID of program or null.
   */
  @JsonIgnore
  public UUID getProgramId() {
    return Optional
        .ofNullable(program)
        .map(ObjectReferenceDto::getId)
        .orElse(null);
  }

  /**
   * Retrieves UUID of supervisory node from object reference. If the object is null, the null
   * value will be returned.
   *
   * @return UUID of supervisory node or null.
   */
  @JsonIgnore
  public UUID getSupervisoryNodeId() {
    return Optional
        .ofNullable(supervisoryNode)
        .map(ObjectReferenceDto::getId)
        .orElse(null);
  }

  /**
   * Retrieves UUID of facilities from object reference list. If the list is empty or null,
   * an empty list will be returned.
   *
   * @return UUID of facilities or an empty list.
   */
  @JsonIgnore
  public Set<UUID> getFacilityIds() {
    return Optional
        .ofNullable(facilities)
        .orElse(Collections.emptyList())
        .stream()
        .map(ObjectReferenceDto::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Retrieves UUID of orderables from object reference list. If the list is empty or null,
   * an empty list will be returned.
   *
   * @return UUID of orderables or an empty list.
   */
  @JsonIgnore
  public Set<UUID> getOrderableIds() {
    return Optional
        .ofNullable(orderables)
        .orElse(Collections.emptyList())
        .stream()
        .map(ObjectReferenceDto::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
