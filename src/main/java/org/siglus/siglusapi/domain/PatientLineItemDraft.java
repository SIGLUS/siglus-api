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
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "patient_line_item_drafts", schema = "siglusintegration")
public class PatientLineItemDraft extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "requisitionDraftId")
  private RequisitionDraft requisitionDraft;

  private UUID patientLineItemId;

  private UUID requisitionId;

  @Column(name = "groupname")
  private String group;

  @Column(name = "columnname")
  private String column;

  private Integer value;

  public static List<PatientLineItemDraft> from(RequisitionDraft draft,
      SiglusRequisitionDto requisitionDto) {
    List<PatientGroupDto> patientGroups = requisitionDto.getPatientLineItems();
    return patientGroups.stream().map(PatientLineItemDraft::from)
        .flatMap(Collection::stream)
        .peek(lineItemDraft -> lineItemDraft.setRequisitionDraft(draft))
        .peek(lineItemDraft -> lineItemDraft.setRequisitionId(requisitionDto.getId()))
        .collect(Collectors.toList());
  }

  private static List<PatientLineItemDraft> from(PatientGroupDto patientGroup) {
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
          PatientLineItemDraft draft = new PatientLineItemDraft();
          draft.setPatientLineItemId(columnDto.getId());
          draft.setId(null);
          draft.setGroup(patientGroup.getName());
          draft.setColumn(columnName);
          draft.setValue(columnDto.getValue());
          return draft;
        })
        .collect(Collectors.toList());
  }

  public static List<PatientGroupDto> getLineItemDto(List<PatientLineItemDraft> lineItemDrafts) {
    if (isEmpty(lineItemDrafts)) {
      return emptyList();
    }
    return lineItemDrafts.stream()
        .collect(Collectors.groupingBy(PatientLineItemDraft::getGroup))
        .entrySet().stream()
        .map(PatientLineItemDraft::entryToDto)
        .collect(Collectors.toList());
  }

  private static PatientGroupDto entryToDto(Entry<String, List<PatientLineItemDraft>> entry) {
    if (entry == null) {
      return null;
    }
    List<PatientLineItemDraft> lineItemDrafts = entry.getValue();
    if (isEmpty(lineItemDrafts)) {
      return null;
    }
    PatientGroupDto dto = new PatientGroupDto();
    dto.setName(entry.getKey());
    dto.setColumns(
        lineItemDrafts.stream()
            .collect(
                Collectors
                    .toMap(PatientLineItemDraft::getColumn, PatientLineItemDraft::draftToDto)));
    return dto;
  }

  private static PatientColumnDto draftToDto(PatientLineItemDraft lineItemDraft) {
    if (lineItemDraft == null) {
      return null;
    }
    PatientColumnDto dto = new PatientColumnDto();
    dto.setId(lineItemDraft.getPatientLineItemId());
    dto.setValue(lineItemDraft.getValue());
    return dto;
  }
}
