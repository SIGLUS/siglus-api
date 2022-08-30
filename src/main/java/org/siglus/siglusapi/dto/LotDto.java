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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.dto.BaseDto;
import org.siglus.siglusapi.dto.android.db.ProductLot;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
public class LotDto extends BaseDto {

  private String lotCode;
  private boolean active;
  private UUID tradeItemId;
  private LocalDate expirationDate;
  private LocalDate manufactureDate;

  public static LotDto convert(ProductLot productLot) {
    LotDto lotDto = LotDto.builder()
        .lotCode(productLot.getLot().getCode())
        .expirationDate(productLot.getLot().getExpirationDate())
        .tradeItemId(productLot.getTradeItemId())
        .manufactureDate(productLot.getLot().getExpirationDate())
        .active(true)
        .build();
    lotDto.setId(productLot.getId());
    return lotDto;
  }

  public static List<LotDto> convertList(Collection<ProductLot> productLots) {
    if (CollectionUtils.isEmpty(productLots)) {
      return new ArrayList<>();
    }
    return productLots.stream().map(LotDto::convert).collect(Collectors.toList());
  }
}
