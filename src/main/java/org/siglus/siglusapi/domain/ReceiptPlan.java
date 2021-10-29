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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "receipt_plans", schema = "siglusintegration")
public class ReceiptPlan extends BaseEntity {

  private String receiptPlanNumber;

  private String facilityCode;

  private String facilityName;

  private ZonedDateTime approveRequisitionDate;

  private String requisitionNumber;

  private ZonedDateTime lastUpdatedDate;

  @OneToMany(
      mappedBy = "receiptPlan",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  @Default
  private List<ReceiptPlanLineItem> lineItems = Collections.emptyList();

  public static ReceiptPlan from(ReceiptPlanDto receiptPlanDto) {
    ReceiptPlan receiptPlan = new ReceiptPlan();
    receiptPlan.setReceiptPlanNumber(receiptPlanDto.getReceiptPlanNumber());
    receiptPlan.setFacilityCode(receiptPlanDto.getClientCode());
    receiptPlan.setFacilityName(receiptPlanDto.getClientName());
    receiptPlan.setApproveRequisitionDate(receiptPlanDto.getDate());
    receiptPlan.setRequisitionNumber(receiptPlanDto.getRequisitionNumber());
    receiptPlan.setLastUpdatedDate(receiptPlanDto.getLastUpdatedAt());
    receiptPlan.setLineItems(receiptPlanDto.getProducts().stream().map(lineItem ->
        ReceiptPlanLineItem.from(receiptPlan, lineItem)).collect(Collectors.toList()));
    return receiptPlan;
  }

}
