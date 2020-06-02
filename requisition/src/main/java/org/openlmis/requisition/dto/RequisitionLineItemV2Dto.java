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

import static org.openlmis.requisition.web.ResourceNames.APPROVED_PRODUCTS;
import static org.openlmis.requisition.web.ResourceNames.ORDERABLES;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.money.Money;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public final class RequisitionLineItemV2Dto extends BaseRequisitionLineItemDto {

  @JsonIgnore
  private String serviceUrl;

  private VersionObjectReferenceDto orderable;

  private VersionObjectReferenceDto approvedProduct;

  @Override
  @JsonIgnore
  public void setOrderable(OrderableDto orderableDto) {
    this.orderable = new VersionObjectReferenceDto(
        orderableDto.getId(), serviceUrl, ORDERABLES, orderableDto.getVersionNumber());
  }

  @JsonSetter("orderable")
  public void setOrderable(VersionObjectReferenceDto orderable) {
    this.orderable = orderable;
  }

  @Override
  @JsonIgnore
  public void setApprovedProduct(ApprovedProductDto approvedProduct) {
    this.approvedProduct = new VersionObjectReferenceDto(
        approvedProduct.getId(), serviceUrl, APPROVED_PRODUCTS, approvedProduct.getVersionNumber());
  }

  @JsonSetter("approvedProduct")
  public void setApprovedProduct(VersionObjectReferenceDto approvedProduct) {
    this.approvedProduct = approvedProduct;
  }

  @JsonIgnore
  @Override
  public VersionIdentityDto getOrderableIdentity() {
    return Optional
        .ofNullable(orderable)
        .map(item -> new VersionIdentityDto(item.getId(), item.getVersionNumber()))
        .orElse(null);
  }

  @JsonIgnore
  @Override
  public VersionIdentityDto getApprovedProductIdentity() {
    return Optional
        .ofNullable(approvedProduct)
        .map(item -> new VersionIdentityDto(item.getId(), item.getVersionNumber()))
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public void setPricePerPack(Money pricePerPack) {
    // not supported
  }

  @Override
  @JsonIgnore
  public void setMaxPeriodsOfStock(BigDecimal maxPeriodsOfStock) {
    // not supported
  }

}
