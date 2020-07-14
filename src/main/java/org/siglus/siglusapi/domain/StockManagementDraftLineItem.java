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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openlmis.stockmanagement.domain.ExtraDataConverter;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "stock_management_draft_line_items", schema = "siglusintegration")
public class StockManagementDraftLineItem extends BaseEntity {
  @ManyToOne
  @JoinColumn(nullable = false)
  private StockManagementDraft stockManagementDraft;

  @Column(nullable = false)
  private UUID orderableId;

  @Column(nullable = false)
  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")
  private LocalDate occurredDate;

  private UUID lotId;
  private LocalDate expirationDate;
  private String lotCode;
  private String documentNumber;
  private Integer quantity;
  private UUID destinationId;
  private String destinationFreeText;
  private UUID sourceId;
  private String sourceFreeText;
  private UUID reasonId;
  private String reasonFreeText;

  @Column(name = "extradata", columnDefinition = "jsonb")
  @Convert(converter = ExtraDataConverter.class)
  private Map<String, String> extraData;

  public static StockManagementDraftLineItem from(StockManagementDraftLineItemDto draftLineItemDto,
                                                  StockManagementDraft draft) {
    StockManagementDraftLineItem lineItem = new StockManagementDraftLineItem();
    BeanUtils.copyProperties(draftLineItemDto, lineItem);
    lineItem.setStockManagementDraft(draft);
    return lineItem;
  }
}
