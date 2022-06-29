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

import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.springframework.beans.BeanUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "stock_management_drafts", schema = "siglusintegration")
public class StockManagementDraft extends BaseEntity {

  @Column(nullable = false)
  private UUID facilityId;
  @Column(nullable = false)
  private Boolean isDraft;
  @Column(nullable = false)
  private UUID programId;

  private LocalDate occurredDate;
  private String signature;
  private UUID userId;
  private String draftType;
  private UUID destinationId;
  private PhysicalInventorySubDraftEnum status;
  private String documentationNumber;

  @LazyCollection(FALSE)
  @OneToMany(cascade = ALL, mappedBy = "stockManagementDraft", orphanRemoval = true)
  private List<StockManagementDraftLineItem> lineItems;


  public static StockManagementDraft createEmptyDraft(StockManagementDraftDto draftDto) {
    StockManagementDraft draft = new StockManagementDraft();
    BeanUtils.copyProperties(draftDto, draft);
    draft.setIsDraft(true);
    return draft;
  }

  public static StockManagementDraft createStockManagementDraft(StockManagementDraftDto draftDto, boolean isDraft) {
    StockManagementDraft draft = new StockManagementDraft();
    BeanUtils.copyProperties(draftDto, draft);
    draft.setIsDraft(isDraft);
    List<StockManagementDraftLineItemDto> lineItemDtos = draftDto.getLineItems();
    if (lineItemDtos != null) {
      draft.setLineItems(lineItemDtos.stream()
          .map(lineItemDto -> StockManagementDraftLineItem.from(lineItemDto, draft))
          .collect(toList()));
    }
    return draft;
  }
}
