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
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.springframework.beans.BeanUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductLocationMovementDraftDto {

  private UUID id;

  private UUID facilityId;

  private UUID programId;

  @JsonFormat(shape = STRING)
  private LocalDate createdDate;

  private String signature;

  private UUID userId;

  private List<ProductLocationMovementDraftLineItemDto> lineItems;

  public static List<ProductLocationMovementDraftDto> from(Collection<ProductLocationMovementDraft> drafts) {
    List<ProductLocationMovementDraftDto> draftDtos = new ArrayList<>(drafts.size());
    drafts.forEach(draft -> draftDtos.add(from(draft)));
    return draftDtos;
  }

  public static ProductLocationMovementDraftDto from(ProductLocationMovementDraft draft) {
    ProductLocationMovementDraftDto draftDto = new ProductLocationMovementDraftDto();
    BeanUtils.copyProperties(draft, draftDto);
    if (draft.getLineItems() != null) {
      draftDto.setLineItems(draft.getLineItems().stream().map(
          ProductLocationMovementDraftLineItemDto::from).collect(toList()));
    }
    return draftDto;
  }
}