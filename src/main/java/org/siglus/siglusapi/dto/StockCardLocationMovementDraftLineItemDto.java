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

package org.siglus.siglusapi.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraftLineItem;
import org.springframework.beans.BeanUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCardLocationMovementDraftLineItemDto {

  private UUID orderableId;
  private String productCode;
  private String productName;
  private UUID lotId;
  private String lotCode;
  private Boolean isKit;
  private String srcArea;
  private String srcLocationCode;
  private String destArea;
  private String destLocationCode;
  @JsonFormat(shape = STRING)
  private LocalDate expirationDate;
  private Integer quantity;
  private Integer stockOnHand;

  public static StockCardLocationMovementDraftLineItemDto from(StockCardLocationMovementDraftLineItem lineItem) {
    StockCardLocationMovementDraftLineItemDto lineItemDto = new StockCardLocationMovementDraftLineItemDto();
    BeanUtils.copyProperties(lineItem, lineItemDto);
    return lineItemDto;
  }
}
