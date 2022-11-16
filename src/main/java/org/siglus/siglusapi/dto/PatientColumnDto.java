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

import static org.siglus.siglusapi.constant.FieldConstants.SEPARATOR;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.QUERY_MAX_VALUE_IN_LAST_PERIODS;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.QUERY_REQUISITIONS_UNDER_HIGH_LEVEL;

import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "PatientLineItem.sumValueRequisitionsUnderHighLevelFacility",
        query = "select pli.groupname as group,pli.columnname as column,sum(pli.value) as value "
            + "from siglusintegration.patient_line_items pli "
            + "where pli.requisitionid in "
            + QUERY_REQUISITIONS_UNDER_HIGH_LEVEL
            + " group by pli.groupname,pli.columnname",
        resultSetMapping = "PatientLineItem.PatientColumnDto"),
    @NamedNativeQuery(
        name = "PatientLineItem.maxValueRequisitionsInLastPeriods",
        query = "select groupname as group, columnname as column, sum(maxvalue) as value from "
            + "(select pli.groupname,pli.columnname,max(pli.value) as maxvalue "
            + "from siglusintegration.patient_line_items pli "
            + "left join requisition.requisitions r on r.id = pli.requisitionid "
            + "where pli.requisitionid in "
            + QUERY_MAX_VALUE_IN_LAST_PERIODS
            + " group by pli.groupname,pli.columnname,r.facilityid) maxtmp "
            + "group by groupname,columnname",
        resultSetMapping = "PatientLineItem.PatientColumnDto")
})
@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "PatientLineItem.PatientColumnDto",
        classes = @ConstructorResult(
            targetClass = PatientColumnDto.class,
            columns = {
                @ColumnResult(name = "group", type = String.class),
                @ColumnResult(name = "column", type = String.class),
                @ColumnResult(name = "value", type = Integer.class),
            }
        )
    )
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientColumnDto {

  private UUID id;

  private String group;

  private String column;

  @Min(value = 0)
  private Integer value;

  // comment by yyd: This constructor will be used after the above query, do not delete it
  @SuppressWarnings("unused")
  public PatientColumnDto(String group, String column, Integer value) {
    this.group = group;
    this.column = column;
    this.value = value;
  }

  public String getMappingKey() {
    return group + SEPARATOR + column;
  }


}
