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
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.stockmanagement.domain.common.VvmApplicable;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.springframework.beans.BeanUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockManagementDraftLineItemDto implements VvmApplicable {
  private UUID orderableId;
  private UUID lotId;
  private String lotCode;
  @JsonFormat(shape = STRING)
  private LocalDate occurredDate;
  @JsonFormat(shape = STRING)
  private LocalDate expirationDate;
  private String documentNumber;
  private Integer quantity;
  private UUID destinationId;
  private String destinationFreeText;
  private UUID sourceId;
  private String sourceFreeText;
  private UUID reasonId;
  private String reasonFreeText;

  private Map<String, String> extraData;

  public static StockManagementDraftLineItemDto from(StockManagementDraftLineItem lineItem) {
    StockManagementDraftLineItemDto lineItemDto = new StockManagementDraftLineItemDto();
    BeanUtils.copyProperties(lineItem, lineItemDto);
    return lineItemDto;
  }
}
