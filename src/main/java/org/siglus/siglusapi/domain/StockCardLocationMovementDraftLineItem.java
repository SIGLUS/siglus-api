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

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftLineItemDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "stock_card_location_movement_draft_line_items", schema = "siglusintegration")
public class StockCardLocationMovementDraftLineItem extends BaseEntity {

  @ManyToOne
  @JoinColumn(nullable = false)
  private StockCardLocationMovementDraft stockCardLocationMovementDraft;

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

  private LocalDate expirationDate;
  private Integer quantity;
  private Integer stockOnHand;

  public static StockCardLocationMovementDraftLineItem from(StockCardLocationMovementDraftLineItemDto lineItemDto,
      StockCardLocationMovementDraft draft) {
    StockCardLocationMovementDraftLineItem lineItem = new StockCardLocationMovementDraftLineItem();
    BeanUtils.copyProperties(lineItemDto, lineItem);
    lineItem.setStockCardLocationMovementDraft(draft);
    return lineItem;
  }
}
