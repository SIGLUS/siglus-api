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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RequisitionDto extends BaseRequisitionDto {

  @Getter
  @Setter
  private FacilityDto facility;

  @Getter
  @Setter
  private ProgramDto program;

  @Getter
  @Setter
  private ProcessingPeriodDto processingPeriod;

  @Setter
  private List<RequisitionLineItemDto> requisitionLineItems;

  @Getter
  @Setter
  private Set<OrderableDto> availableFullSupplyProducts;

  @Setter
  private Set<OrderableDto> availableNonFullSupplyProducts;

  /**
   * Retrieves a list of available non full supply products or an empty set,
   * if the property is null.
   * @return set of available non full supply products
   */
  public Set<OrderableDto> getAvailableNonFullSupplyProducts() {
    return Sets.newHashSet(Optional
        .ofNullable(availableNonFullSupplyProducts)
        .orElse(Collections.emptySet()));
  }

  @Override
  @JsonIgnore
  public Set<VersionIdentityDto> getAvailableNonFullSupplyProductsIdentities() {
    return Optional
        .ofNullable(getAvailableNonFullSupplyProducts())
        .orElse(Collections.emptySet())
        .stream()
        .map(item -> new VersionIdentityDto(item.getId(), item.getVersionNumber()))
        .collect(Collectors.toSet());
  }

  @Override
  List<BaseRequisitionLineItemDto> getLineItems() {
    return Lists.newArrayList(Optional
        .ofNullable(requisitionLineItems)
        .orElse(Collections.emptyList()));
  }
}
