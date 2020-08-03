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
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "regimen_line_item_draft", schema = "siglusintegration")
public class RegimenLineItemDraft extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "requisitionDraftId")
  private RequisitionDraft requisitionDraft;

  private UUID regimenLineItemId;

  private UUID requisitionId;

  private UUID regimenId;

  @Column(name = "columnname")
  private String column;

  private Integer value;

  public static List<RegimenLineItemDraft> from(RequisitionDraft draft,
      SiglusRequisitionDto requisitionDto) {
    List<RegimenLineDto> lineDtos = requisitionDto.getRegimenLineItems();
    List<RegimenLineItem> lineItems = RegimenLineItem.from(lineDtos, requisitionDto.getId());

    return lineItems.stream().map(lineItem -> {
      RegimenLineItemDraft lineItemDraft = new RegimenLineItemDraft();
      BeanUtils.copyProperties(lineItem, lineItemDraft);
      lineItemDraft.setRegimenLineItemId(lineItem.getId());
      lineItemDraft.setId(null);
      lineItemDraft.setRequisitionDraft(draft);
      return lineItemDraft;
    }).collect(Collectors.toList());
  }

  public static List<RegimenLineDto> getRegimenLineDtos(List<RegimenLineItemDraft> drafts,
      Map<UUID, RegimenDto> regimenDtoMap) {
    List<RegimenLineItem> lineItems = drafts.stream().map(draft -> {
      RegimenLineItem regimenLineItem = new RegimenLineItem();
      BeanUtils.copyProperties(draft, regimenLineItem);
      regimenLineItem.setId(draft.getRegimenLineItemId());
      return regimenLineItem;
    }).collect(Collectors.toList());
    return RegimenLineDto.from(lineItems, regimenDtoMap);
  }
}
