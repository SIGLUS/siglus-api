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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.UsageInformationLineItem;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsageInformationServiceDto {

  private String service;

  private Map<String, UsageInformationInformationDto> informations;

  public static List<UsageInformationServiceDto> from(List<UsageInformationLineItem> lineItems) {
    List<UsageInformationServiceDto> serviceDtos = newArrayList();
    Map<String, List<UsageInformationLineItem>> groupByService = lineItems.stream()
        .collect(Collectors.groupingBy(UsageInformationLineItem::getService));
    groupByService.forEach((groupByServiceKey, groupByServiceValue) -> {
      UsageInformationServiceDto serviceDto = new UsageInformationServiceDto();
      serviceDto.setService(groupByServiceKey);
      Map<String, UsageInformationInformationDto> informations = newHashMap();
      Map<String, List<UsageInformationLineItem>> groupByInformation = groupByServiceValue
          .stream()
          .collect(Collectors.groupingBy(UsageInformationLineItem::getInformation));
      groupByInformation.forEach((groupByInformationKey, groupByInformationValue) -> {
        UsageInformationInformationDto informationDto = new UsageInformationInformationDto();
        Map<UUID, UsageInformationOrderableDto> orderables = newHashMap();
        groupByInformationValue.forEach(lineItem -> {
          UsageInformationOrderableDto usageInformationOrderableDto = UsageInformationOrderableDto
              .builder()
              .usageInformationLineItemId(lineItem.getId())
              .value(lineItem.getValue())
              .build();
          orderables.put(lineItem.getOrderableId(), usageInformationOrderableDto);
        });
        informationDto.setOrderables(orderables);
        informations.put(groupByInformationKey, informationDto);
      });
      serviceDto.setInformations(informations);
      serviceDtos.add(serviceDto);
    });
    return serviceDtos;
  }
}
