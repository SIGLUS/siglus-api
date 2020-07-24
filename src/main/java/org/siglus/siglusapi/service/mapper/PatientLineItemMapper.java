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
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.springframework.stereotype.Component;

@Component
public class PatientLineItemMapper {

  public List<PatientLineItem> from(PatientGroupDto patientGroup) {
    if (patientGroup == null) {
      return null;
    }
    if (isEmpty(patientGroup.getColumns())) {
      return emptyList();
    }
    return patientGroup.getColumns().entrySet().stream()
        .map(entry -> {
          String columnName = entry.getKey();
          PatientColumnDto columnDto = entry.getValue();
          PatientLineItem lineItem = new PatientLineItem();
          lineItem.setId(columnDto.getId());
          lineItem.setGroup(patientGroup.getName());
          lineItem.setColumn(columnName);
          lineItem.setValue(columnDto.getValue());
          return lineItem;
        })
        .collect(Collectors.toList());
  }

  public List<PatientGroupDto> from(List<PatientLineItem> lineItems) {
    if (isEmpty(lineItems)) {
      return emptyList();
    }
    return lineItems.stream()
        .collect(Collectors.groupingBy(PatientLineItem::getGroup))
        .entrySet().stream()
        .map(this::from)
        .collect(Collectors.toList());
  }

  private PatientGroupDto from(Entry<String, List<PatientLineItem>> entry) {
    List<PatientLineItem> lineItems = entry.getValue();
    PatientGroupDto dto = new PatientGroupDto();
    dto.setName(entry.getKey());
    dto.setColumns(
        lineItems.stream().collect(Collectors.toMap(PatientLineItem::getColumn, this::from)));
    return dto;
  }

  private PatientColumnDto from(PatientLineItem lineItem) {
    PatientColumnDto dto = new PatientColumnDto();
    dto.setId(lineItem.getId());
    dto.setValue(lineItem.getValue());
    return dto;
  }

}
