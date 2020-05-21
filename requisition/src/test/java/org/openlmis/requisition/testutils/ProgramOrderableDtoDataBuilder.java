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
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ProgramOrderableDtoDataBuilder implements DtoDataBuilder<ProgramOrderableDto> {

  private UUID programId;
  private UUID orderableDisplayCategoryId;
  private String orderableCategoryDisplayName;
  private Integer orderableCategoryDisplayOrder;
  private Boolean fullSupply;
  private Integer displayOrder;
  private Money pricePerPack;

  /**
   * Builder for {@link ProgramOrderableDto}.
   */
  public ProgramOrderableDtoDataBuilder() {
    programId = UUID.randomUUID();
    orderableDisplayCategoryId = UUID.randomUUID();
    orderableCategoryDisplayName = "orderable category";
    orderableCategoryDisplayOrder = 0;
    fullSupply = true;
    displayOrder = 0;
    pricePerPack = Money.of(CurrencyUnit.EUR, 15.6);
  }

  @Override
  public ProgramOrderableDto buildAsDto() {
    return new ProgramOrderableDto(
        programId,
        orderableDisplayCategoryId,
        orderableCategoryDisplayName,
        orderableCategoryDisplayOrder,
        fullSupply,
        displayOrder,
        pricePerPack
    );
  }

  public ProgramOrderableDtoDataBuilder withProgramId(UUID programId) {
    this.programId = programId;
    return this;
  }

  public ProgramOrderableDtoDataBuilder withPricePerPack(Money pricePerPack) {
    this.pricePerPack = pricePerPack;
    return this;
  }
}
