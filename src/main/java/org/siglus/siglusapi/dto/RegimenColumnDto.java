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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "RegimenLineItem.sumValueRequisitionsUnderHighLevelFacility",
        query = "select rli.regimenid as regimenId,rli.columnname as column,sum(rli.value) as value "
            + "from siglusintegration.regimen_line_items rli "
            + "where rli.requisitionid in "
            + QUERY_REQUISITIONS_UNDER_HIGH_LEVEL
            + " group by rli.regimenid,rli.columnname",
        resultSetMapping = "RegimenLineItem.RegimenColumnDto"),
    @NamedNativeQuery(
        name = "RegimenLineItem.maxValueRequisitionsInLastPeriods",
        query = "select regimenid as regimenId,columnname as column,sum(maxvalue) as value from "
            + "(select rli.regimenid,rli.columnname,max(rli.value) as maxvalue "
            + "from siglusintegration.regimen_line_items rli "
            + "left join requisition.requisitions r on r.id = rli.requisitionid "
            + "where rli.requisitionid in "
            + QUERY_MAX_VALUE_IN_LAST_PERIODS
            + "group by rli.regimenid,rli.columnname,r.facilityid) maxtmp "
            + "group by regimenid, columnname",
        resultSetMapping = "RegimenLineItem.RegimenColumnDto")
})

@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "RegimenLineItem.RegimenColumnDto",
        classes = @ConstructorResult(
            targetClass = RegimenColumnDto.class,
            columns = {
                @ColumnResult(name = "regimenId", type = UUID.class),
                @ColumnResult(name = "column", type = String.class),
                @ColumnResult(name = "value", type = Integer.class),
            }
        )
    )
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegimenColumnDto {

  private UUID id;

  private UUID regimenId;

  private String column;

  private Integer value;

  // comment by yyd: This constructor will be used after the above query, do not delete it
  @SuppressWarnings("unused")
  public RegimenColumnDto(UUID regimenId, String column, Integer value) {
    this.regimenId = regimenId;
    this.column = column;
    this.value = value;
  }

  public String getMappingKey() {
    return regimenId + SEPARATOR + column;
  }

}
