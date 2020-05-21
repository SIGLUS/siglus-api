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
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class IdealStockAmountDtoDataBuilder implements DtoDataBuilder<IdealStockAmountDto> {
  private UUID id = UUID.randomUUID();
  private ObjectReferenceDto facility = new ObjectReferenceDto(UUID.randomUUID());
  private ObjectReferenceDto commodityType = new ObjectReferenceDto(UUID.randomUUID());
  private ObjectReferenceDto processingPeriod = new ObjectReferenceDto(UUID.randomUUID());
  private Integer amount = 1000;

  public IdealStockAmountDtoDataBuilder withCommodityTypeId(UUID id) {
    commodityType = new ObjectReferenceDto(id);
    return this;
  }

  /**
   * Create new instance of {@link IdealStockAmountDto}.
   */
  public IdealStockAmountDto buildAsDto() {
    IdealStockAmountDto dto = new IdealStockAmountDto(
        facility, commodityType, processingPeriod, amount
    );
    dto.setId(id);

    return dto;
  }
}
