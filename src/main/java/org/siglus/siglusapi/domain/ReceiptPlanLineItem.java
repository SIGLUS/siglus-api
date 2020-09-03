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

package org.siglus.siglusapi.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.fc.ProductDto;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "receipt_plan_line_item", schema = "siglusintegration")
public class ReceiptPlanLineItem extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "receiptPlanId")
  @Getter
  @Setter
  private ReceiptPlan receiptPlan;

  private String productCode;

  private String productName;

  private Integer approvedQuantity;

  public static ReceiptPlanLineItem from(ReceiptPlan receiptPlan, ProductDto productDto) {
    ReceiptPlanLineItem receiptPlanLineItem = new ReceiptPlanLineItem();
    receiptPlanLineItem.setId(null);
    receiptPlanLineItem.setReceiptPlan(receiptPlan);
    receiptPlanLineItem.setProductCode(productDto.getFnmCode());
    receiptPlanLineItem.setProductName(productDto.getProductDescription());
    receiptPlanLineItem.setApprovedQuantity(productDto.getApprovedQuantity());
    return receiptPlanLineItem;
  }
}
