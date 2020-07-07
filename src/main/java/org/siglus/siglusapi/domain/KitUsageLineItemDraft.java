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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.siglus.siglusapi.dto.KitUsageLineItemDto;
import org.siglus.siglusapi.dto.KitUsageServiceLineItemDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.springframework.beans.BeanUtils;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "kit_usage_line_items_draft", schema = "siglusintegration")
public class KitUsageLineItemDraft extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "requisitionDraftId")
  @Getter
  @Setter
  private RequisitionDraft requisitionDraft;

  private UUID kitUsageLineItemId;

  private UUID requisitionId;

  private String collection;

  private String service;

  private Integer value;

  public static List<KitUsageLineItemDraft> from(RequisitionDraft draft,
      SiglusRequisitionDto requisitionDto) {
    List<KitUsageLineItemDto> kitUsageLineItemDtos = requisitionDto.getKitUsageLineItems();
    List<KitUsageLineItem> lineItems = KitUsageLineItem.from(kitUsageLineItemDtos, requisitionDto);
    return lineItems.stream().map(lineItem -> {
      KitUsageLineItemDraft lineItemDraft = new KitUsageLineItemDraft();
      BeanUtils.copyProperties(lineItem, lineItemDraft);
      lineItemDraft.setKitUsageLineItemId(lineItem.getId());
      lineItemDraft.setId(null);
      lineItemDraft.setRequisitionDraft(draft);
      return lineItemDraft;
    }).collect(Collectors.toList());
  }

  public static List<KitUsageLineItemDto> from(List<KitUsageLineItemDraft> draftList) {
    List<KitUsageLineItemDto> kitDtos = new ArrayList<>();
    Map<String, List<KitUsageLineItemDraft>> groupKitUsages = draftList.stream()
        .collect(Collectors.groupingBy(KitUsageLineItemDraft::getCollection));
    for (Entry<String, List<KitUsageLineItemDraft>> groupKitUsage : groupKitUsages.entrySet()) {
      KitUsageLineItemDto kitUsageLineItemDto = new KitUsageLineItemDto();
      kitUsageLineItemDto.setCollection(groupKitUsage.getKey());
      Map<String, KitUsageServiceLineItemDto> services = new HashMap<>();
      groupKitUsage.getValue().forEach(lineItem -> {
        KitUsageServiceLineItemDto dto = KitUsageServiceLineItemDto.builder()
            .id(lineItem.getKitUsageLineItemId())
            .value(lineItem.getValue())
            .build();
        services.put(lineItem.getService(), dto);
      });
      kitUsageLineItemDto.setServices(services);
      kitDtos.add(kitUsageLineItemDto);
    }
    return kitDtos;
  }
}
