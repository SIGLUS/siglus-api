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

package org.siglus.siglusapi.dto.android.request;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.siglus.siglusapi.constant.android.MmtbRequisitionConstants.MmtbPatientSection;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientLineItemsRequest {

  @NotBlank
  private String name;

  @NotNull
  private List<PatientLineItemColumnRequest> columns;
  
  public static PatientLineItemsRequest from(PatientGroupDto patientGroupDto) {
    List<PatientLineItemColumnRequest> columns = newArrayList();
    String tableValue = patientGroupDto.getName();
    String tableKey = MmtbPatientSection.getTableKeyByValue(tableValue);
    Map<String, PatientColumnDto> columnNameToPatientColumn = patientGroupDto.getColumns();
    columnNameToPatientColumn.forEach((columnValue, patientColumn) -> {
      columns.add(PatientLineItemColumnRequest.builder()
          .tableName(tableKey)
          .name(MmtbPatientSection.getColumnKeyByValue(tableValue, columnValue))
          .value(patientColumn.getValue())
          .build());
    });
    return PatientLineItemsRequest.builder()
        .name(tableKey)
        .columns(columns)
        .build();
  }
}
