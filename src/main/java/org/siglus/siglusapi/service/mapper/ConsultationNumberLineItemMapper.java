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

package org.siglus.siglusapi.service.mapper;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.stream.Collectors;
import org.siglus.siglusapi.domain.ConsultationNumberLineItem;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.springframework.stereotype.Component;

@Component
public class ConsultationNumberLineItemMapper {

  public List<ConsultationNumberLineItem> from(ConsultationNumberGroupDto group) {
    if (group == null || isEmpty(group.getColumns())) {
      return emptyList();
    }
    return group.getColumns().entrySet().stream()
        .map(entry -> {
          String columnName = entry.getKey();
          ConsultationNumberColumnDto columnDto = entry.getValue();
          ConsultationNumberLineItem lineItem = new ConsultationNumberLineItem();
          lineItem.setId(columnDto.getId());
          lineItem.setGroup(group.getName());
          lineItem.setColumn(columnName);
          lineItem.setValue(columnDto.getValue());
          return lineItem;
        })
        .collect(Collectors.toList());
  }

  public ConsultationNumberGroupDto from(List<ConsultationNumberLineItem> lineItems) {
    if (isEmpty(lineItems)) {
      return null;
    }
    String groupName = lineItems.stream().findAny().map(ConsultationNumberLineItem::getGroup).get();
    ConsultationNumberGroupDto group = new ConsultationNumberGroupDto();
    group.setName(groupName);
    group.setColumns(lineItems.stream().collect(Collectors
        .toMap(ConsultationNumberLineItem::getColumn, this::toColumnDto)));
    return group;
  }

  private ConsultationNumberColumnDto toColumnDto(ConsultationNumberLineItem lineItem) {
    ConsultationNumberColumnDto dto = new ConsultationNumberColumnDto();
    dto.setId(lineItem.getId());
    dto.setValue(lineItem.getValue());
    return dto;
  }

}
