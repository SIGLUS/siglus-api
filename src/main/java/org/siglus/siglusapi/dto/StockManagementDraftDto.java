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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.springframework.beans.BeanUtils;

@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StockManagementDraftDto extends StockManagementDraftBaseDto {

  private UUID destinationId;
  private String documentNumber;

  private List<StockManagementDraftLineItemDto> lineItems;

  public static List<StockManagementDraftDto> from(Collection<StockManagementDraft> drafts) {
    List<StockManagementDraftDto> draftDtos = new ArrayList<>(drafts.size());
    drafts.forEach(i -> draftDtos.add(from(i)));
    return draftDtos;
  }

  public static StockManagementDraftDto from(StockManagementDraft draft) {
    StockManagementDraftDto draftDto = new StockManagementDraftDto();
    BeanUtils.copyProperties(draft, draftDto);
    if (draft.getLineItems() != null) {
      draftDto.setLineItems(draft.getLineItems().stream().map(
          StockManagementDraftLineItemDto::from).collect(toList()));
    }
    return draftDto;
  }

  public static StockManagementDraftDto from(StockManagementDraftWithLocationDto draft) {
    StockManagementDraftDto draftDto = new StockManagementDraftDto();
    BeanUtils.copyProperties(draft, draftDto);
    if (draft.getLineItems() != null) {
      draftDto.setLineItems(draft.getLineItems().stream().map(
          StockManagementDraftLineItemDto::from).collect(toList()));
    }
    return draftDto;
  }
}
