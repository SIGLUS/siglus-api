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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.RegimenDispatchLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "requisitionDraft")
@Table(name = "regimen_summary_line_item_draft", schema = "siglusintegration")
public class RegimenSummaryLineItemDraft extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "requisitionDraftId")
  private RequisitionDraft requisitionDraft;

  private UUID regimenSummaryLineItemId;

  private UUID requisitionId;

  private UUID regimenDispatchLineId;

  @Column(name = "columnname")
  private String column;

  private Integer value;

  public static List<RegimenSummaryLineItemDraft> from(RequisitionDraft draft,
      SiglusRequisitionDto requisitionDto) {
    List<RegimenSummaryLineDto> lineDtos = requisitionDto.getRegimenDispatchLineItems();
    List<RegimenSummaryLineItem> lineItems =
        RegimenSummaryLineItem.from(lineDtos, requisitionDto.getId());

    return lineItems.stream().map(lineItem -> {
      RegimenSummaryLineItemDraft lineItemDraft = new RegimenSummaryLineItemDraft();
      BeanUtils.copyProperties(lineItem, lineItemDraft);
      lineItemDraft.setRegimenSummaryLineItemId(lineItem.getId());
      lineItemDraft.setId(null);
      lineItemDraft.setRequisitionDraft(draft);
      return lineItemDraft;
    }).collect(Collectors.toList());
  }

  public static List<RegimenSummaryLineDto> getRegimenSummaryLineDtos(
      List<RegimenSummaryLineItemDraft> drafts,
      Map<UUID, RegimenDispatchLineDto> regimenDispatchLineDtoMap) {
    List<RegimenSummaryLineItem> lineItems = drafts.stream().map(draft -> {
      RegimenSummaryLineItem summaryLineItem = new RegimenSummaryLineItem();
      BeanUtils.copyProperties(draft, summaryLineItem);
      summaryLineItem.setId(draft.getRegimenSummaryLineItemId());
      return summaryLineItem;
    }).collect(Collectors.toList());
    return RegimenSummaryLineDto.from(lineItems, regimenDispatchLineDtoMap);
  }
}
