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

package org.openlmis.requisition.service.referencedata;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;

public final class ApproveProductsAggregator {
  private Map<VersionIdentityDto, ApprovedProductDto> allProducts;
  private Set<ApprovedProductReference> allProductReferences;
  private Set<VersionIdentityDto> allOrderableIdentities;

  private List<ApprovedProductDto> fullSupplyProducts;
  private Set<UUID> fullSupplyOrderableIds;

  private Set<ApprovedProductReference> nonFullSupplyProductReferences;

  /**
   * Create a new instance of the {@link ApproveProductsAggregator}.
   */
  public ApproveProductsAggregator(List<ApprovedProductDto> products, UUID programId) {
    allProducts = Maps.newHashMap();
    allProductReferences = Sets.newHashSet();
    allOrderableIdentities = Sets.newHashSet();

    fullSupplyProducts = Lists.newArrayList();
    fullSupplyOrderableIds = Sets.newHashSet();

    nonFullSupplyProductReferences = Sets.newHashSet();

    groupByOrderableId(products, programId);
  }

  private void groupByOrderableId(List<ApprovedProductDto> products, UUID programId) {
    for (ApprovedProductDto approvedProduct : products) {
      OrderableDto orderable = approvedProduct.getOrderable();

      allProducts.put(approvedProduct.getIdentity(), approvedProduct);
      allProductReferences.add(new ApprovedProductReference(approvedProduct.getId(),
          approvedProduct.getVersionNumber(), orderable.getId(), orderable.getVersionNumber()));
      allOrderableIdentities.add(orderable.getIdentity());

      ProgramOrderableDto po = orderable.getProgramOrderable(programId);

      if (Objects.equals(true, po.getFullSupply())) {
        fullSupplyProducts.add(approvedProduct);
        fullSupplyOrderableIds.add(orderable.getId());
      }

      if (Objects.equals(false, po.getFullSupply())) {
        nonFullSupplyProductReferences.add(new ApprovedProductReference(approvedProduct.getId(),
            approvedProduct.getVersionNumber(), orderable.getId(), orderable.getVersionNumber()));
      }
    }
  }

  public Set<ApprovedProductReference> getApprovedProductReferences() {
    return allProductReferences;
  }

  public Set<VersionIdentityDto> getOrderableIdentities() {
    return allOrderableIdentities;
  }

  public Set<ApprovedProductReference> getNonFullSupplyApprovedProductReferences() {
    return nonFullSupplyProductReferences;
  }

  public List<ApprovedProductDto> getFullSupplyProducts() {
    return fullSupplyProducts;
  }

  /**
   * Retrieve full supply orderable ids.
   */
  public Set<UUID> getFullSupplyOrderableIds() {
    return fullSupplyOrderableIds;
  }

  public Map<VersionIdentityDto, ApprovedProductDto> getAllGroupByIdentity() {
    return allProducts;
  }
}
