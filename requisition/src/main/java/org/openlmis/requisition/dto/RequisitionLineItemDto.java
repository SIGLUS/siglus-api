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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.money.Money;
import org.openlmis.requisition.utils.MoneyDeserializer;
import org.openlmis.requisition.utils.MoneySerializer;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionLineItemDto extends BaseRequisitionLineItemDto {

  private OrderableDto orderable;

  @JsonSerialize(using = MoneySerializer.class)
  @JsonDeserialize(using = MoneyDeserializer.class)
  private Money pricePerPack;

  private BigDecimal maxPeriodsOfStock;

  @JsonIgnore
  @Override
  public VersionIdentityDto getOrderableIdentity() {
    return Optional
        .ofNullable(orderable)
        .map(item -> new VersionIdentityDto(item.getId(), item.getVersionNumber()))
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public VersionIdentityDto getApprovedProductIdentity() {
    return null;
  }

  @Override
  @JsonIgnore
  public void setApprovedProduct(ApprovedProductDto approvedProduct) {
    // nothing to do here
  }
}
