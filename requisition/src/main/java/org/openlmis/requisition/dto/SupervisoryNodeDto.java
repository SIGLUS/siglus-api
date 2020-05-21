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
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.Map;
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
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SupervisoryNodeDto extends BaseDto {
  private String code;
  private ObjectReferenceDto facility;
  private String name;
  private String description;
  private Map<String, Object> extraData;
  private ObjectReferenceDto parentNode;
  private ObjectReferenceDto requisitionGroup;
  private Set<ObjectReferenceDto> childNodes;
  private ObjectReferenceDto partnerNodeOf;
  private Set<ObjectReferenceDto> partnerNodes;

  /**
   * Retrieves UUID of parent node from object reference. If the object is null, the null value
   * will be returned.
   *
   * @return UUID of parent node or null.
   */
  @JsonIgnore
  public UUID getParentNodeId() {
    return Optional
        .ofNullable(parentNode)
        .map(ObjectReferenceDto::getId)
        .orElse(null);
  }

  /**
   * Retrieves UUID of partner nodes from object reference list. If the list is empty or null,
   * an empty list will be returned.
   *
   * @return UUID of partner nodes or an empty list.
   */
  @JsonIgnore
  public Set<UUID> getPartnerNodeIds() {
    return Optional
        .ofNullable(partnerNodes)
        .orElse(Collections.emptySet())
        .stream()
        .map(ObjectReferenceDto::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Retrieves UUID of requisition group from object reference. If the object is null,
   * the null value will be returned.
   *
   * @return UUID of requisition group or null.
   */
  @JsonIgnore
  public UUID getRequisitionGroupId() {
    return Optional
        .ofNullable(requisitionGroup)
        .map(ObjectReferenceDto::getId)
        .orElse(null);
  }
}
