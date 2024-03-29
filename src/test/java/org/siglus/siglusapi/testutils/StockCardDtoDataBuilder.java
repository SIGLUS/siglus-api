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

package org.siglus.siglusapi.testutils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardDto;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.ReasonCategory;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.siglus.siglusapi.testutils.api.DtoDataBuilder;

public class StockCardDtoDataBuilder implements DtoDataBuilder<StockCardDto> {

  private BaseDto lot;
  private OrderableDto orderable;
  private Integer stockOnHand;

  public StockCardDtoDataBuilder() {
    lot = new BaseDto();
    orderable = new org.siglus.siglusapi.testutils.OrderableDtoDataBuilder().buildAsDto();
    stockOnHand = 2;
  }

  @Override
  public StockCardDto buildAsDto() {
    return new StockCardDto(lot, orderable, stockOnHand);
  }

  public static org.openlmis.stockmanagement.dto.StockCardDto createStockCardDto() {
    StockCardLineItemReason reason = StockCardLineItemReason
        .builder()
        .name("Transfer In")
        .reasonCategory(ReasonCategory.ADJUSTMENT)
        .reasonType(ReasonType.CREDIT).build();


    StockCardLineItem lineItem = StockCardLineItem
        .builder()
        .quantity(1)
        .stockOnHand(1)
        .occurredDate(LocalDate.of(2017, 2, 13))
        .reason(reason).build();

    StockCardLineItemDto lineItemDto = StockCardLineItemDto
        .builder()
        .lineItem(lineItem)
        .source(FacilityDto.builder().name("HF1").build())
        .build();

    return org.openlmis.stockmanagement.dto.StockCardDto.builder()
        .stockOnHand(1)
        .facility(FacilityDto.builder().name("HC01").id(UUID.randomUUID()).build())
        .program(ProgramDto.builder().name("HIV").id(UUID.randomUUID()).build())
        .orderable(org.openlmis.stockmanagement.dto.referencedata.OrderableDto.builder().productCode("ABC01").build())
        .lineItems(Arrays.asList(lineItemDto))
        .build();
  }
}
