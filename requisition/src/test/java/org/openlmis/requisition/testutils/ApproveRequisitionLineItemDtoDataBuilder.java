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

package org.openlmis.requisition.testutils;

import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.dto.ApproveRequisitionLineItemDto;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ApproveRequisitionLineItemDtoDataBuilder implements
    DtoDataBuilder<ApproveRequisitionLineItemDto> {

  private UUID id;
  private BasicOrderableDto orderable;
  private Integer approvedQuantity;
  private Money pricePerPack;
  private Money totalCost;
  private Boolean skipped;

  /**
   * Builder for {@link ApproveRequisitionLineItemDto}.
   */
  public ApproveRequisitionLineItemDtoDataBuilder() {
    id = UUID.randomUUID();
    orderable = new BasicOrderableDto();
    approvedQuantity = 1;
    pricePerPack = Money.of(CurrencyUnit.EUR, 1);
    totalCost = Money.of(CurrencyUnit.EUR, 2);
    skipped = false;
  }

  @Override
  public ApproveRequisitionLineItemDto buildAsDto() {
    return new ApproveRequisitionLineItemDto(id,
        orderable,
        approvedQuantity,
        pricePerPack,
        totalCost,
        skipped
    );
  }

  public ApproveRequisitionLineItemDtoDataBuilder withOrderable(OrderableDto orderable) {
    this.orderable = orderable;
    return this;
  }

  public ApproveRequisitionLineItemDtoDataBuilder withSkippedFlag(Boolean skipped) {
    this.skipped = skipped;
    return this;
  }
}
