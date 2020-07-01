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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
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

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "kit_usage_line_items", schema = "siglusintegration")
public class KitUsageLineItem extends BaseEntity {

  private UUID requisitionId;

  private String collection;

  private String service;

  private Integer value;

  public static List<KitUsageLineItem> from(List<KitUsageLineItemDto> kitUsageLineItemDtos,
      SiglusRequisitionDto requisitionDto) {
    List<KitUsageLineItem> lineItems = new ArrayList<>();
    for (KitUsageLineItemDto lineItemDto : kitUsageLineItemDtos) {
      String collection = lineItemDto.getCollection();
      for (Map.Entry<String, KitUsageServiceLineItemDto> serviceDto :
          lineItemDto.getServices().entrySet()) {
        KitUsageServiceLineItemDto service = serviceDto.getValue();
        KitUsageLineItem kitUsageLineItem = KitUsageLineItem.builder()
            .requisitionId(requisitionDto.getId())
            .collection(collection)
            .service(serviceDto.getKey())
            .value(service.getValue())
            .build();
        kitUsageLineItem.setId(service.getId());
        lineItems.add(kitUsageLineItem);
      }
    }
    return lineItems;
  }
}
