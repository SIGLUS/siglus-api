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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "usage_information_line_items", schema = "siglusintegration")
public class UsageInformationLineItem extends BaseEntity {

  private UUID requisitionId;

  private String information;

  private String service;

  private UUID orderableId;

  private Integer value;

  public static List<UsageInformationLineItem> from(List<UsageInformationServiceDto> serviceDtos,
      UUID requisitionId) {
    List<UsageInformationLineItem> lineItems = newArrayList();
    serviceDtos.forEach(serviceDto -> {
      String service = serviceDto.getService();
      serviceDto.getInformations()
          .forEach((informationKey, informationValue) -> informationValue.getOrderables()
              .forEach((orderableKey, orderableValue) -> {
                UsageInformationLineItem lineItem = UsageInformationLineItem.builder()
                    .requisitionId(requisitionId)
                    .service(service)
                    .information(informationKey)
                    .orderableId(orderableKey)
                    .value(orderableValue.getValue())
                    .build();
                lineItem.setId(orderableValue.getUsageInformationLineItemId());
                lineItems.add(lineItem);
              }));
    });
    return lineItems;
  }

}
