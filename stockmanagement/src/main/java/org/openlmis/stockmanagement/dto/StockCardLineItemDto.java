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

package org.openlmis.stockmanagement.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class StockCardLineItemDto extends BaseDto {

  // [SIGLUS change start]
  // [change reason]: support get data from get stock card interface
  @JsonFormat(shape = STRING)
  private LocalDate occurredDate;
  private String destinationFreeText;
  private String documentNumber;
  private Map<String, String> extraData;
  private Boolean physicalInventory;
  private Integer quantity;
  private Integer quantityWithSign;
  private StockCardLineItemReasonDto reason;
  private String reasonFreeText;
  private String signature;
  private String sourceFreeText;
  private List<PhysicalInventoryLineItemAdjustmentDto> stockAdjustments;
  private Integer stockOnHand;
  @JsonIgnore
  private ZonedDateTime processedDate;

  @JsonIgnore
  private StockCard stockCard;

  //@JsonUnwrapped
  @JsonIgnore
  private StockCardLineItem lineItem;
  // [SIGLUS change end]

  private FacilityDto source;
  private FacilityDto destination;

  /**
   * Create stock card line item dto from stock card line item.
   *
   * @param stockCardLineItem stock card line item.
   * @return the created stock card line item dto.
   */
  public static StockCardLineItemDto createFrom(StockCardLineItem stockCardLineItem) {
    return StockCardLineItemDto.builder()
        .lineItem(stockCardLineItem)
        .build();
  }
}
