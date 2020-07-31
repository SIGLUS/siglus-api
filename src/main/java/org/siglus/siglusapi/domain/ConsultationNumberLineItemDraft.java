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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "consultation_number_line_item_drafts", schema = "siglusintegration")
public class ConsultationNumberLineItemDraft extends AbstractUsageLineItemDraft {

  public static List<ConsultationNumberLineItemDraft> from(RequisitionDraft draft,
      SiglusRequisitionDto requisitionDto) {
    ConsultationNumberGroupDto group = requisitionDto.getConsultationNumberLineItems().stream()
        .findFirst().orElse(null);
    List<ConsultationNumberLineItemDraft> list = fromGroup(group);
    for (ConsultationNumberLineItemDraft lineItemDraft : list) {
      lineItemDraft.setRequisitionDraft(draft);
      lineItemDraft.setRequisitionId(requisitionDto.getId());
    }

    return list;
  }

  private static List<ConsultationNumberLineItemDraft> fromGroup(ConsultationNumberGroupDto group) {
    if (group == null || isEmpty(group.getColumns())) {
      return emptyList();
    }
    return group.getColumns().entrySet().stream()
        .map(entry -> {
          String columnName = entry.getKey();
          ConsultationNumberColumnDto columnDto = entry.getValue();
          ConsultationNumberLineItemDraft draft = new ConsultationNumberLineItemDraft();
          draft.setPatientLineItemId(columnDto.getId());
          draft.setId(null);
          draft.setGroup(group.getName());
          draft.setColumn(columnName);
          draft.setValue(columnDto.getValue());
          return draft;
        })
        .collect(Collectors.toList());
  }

  public static List<ConsultationNumberGroupDto> getLineItemDto(
      List<ConsultationNumberLineItemDraft> drafts) {
    if (isEmpty(drafts)) {
      return emptyList();
    }
    String groupName = drafts.stream().findAny().map(ConsultationNumberLineItemDraft::getGroup)
        .orElseThrow(NullPointerException::new);
    ConsultationNumberGroupDto group = new ConsultationNumberGroupDto();
    group.setName(groupName);
    group.setColumns(drafts.stream().collect(Collectors
        .toMap(ConsultationNumberLineItemDraft::getColumn,
            ConsultationNumberLineItemDraft::draftToDto)));
    return singletonList(group);
  }

  private static ConsultationNumberColumnDto draftToDto(ConsultationNumberLineItemDraft draft) {
    if (draft == null) {
      return null;
    }
    ConsultationNumberColumnDto dto = new ConsultationNumberColumnDto();
    dto.setId(draft.getPatientLineItemId());
    dto.setValue(draft.getValue());
    return dto;
  }

}
