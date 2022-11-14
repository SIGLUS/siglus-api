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
import static org.siglus.siglusapi.constant.FieldConstants.SEPARATOR;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.QUERY_MAX_VALUE_IN_LAST_PERIODS;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.QUERY_REQUISITIONS_UNDER_HIGH_LEVEL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "RegimenSummaryLineItem.sumValueRequisitionsUnderHighLevelFacility",
        query = "select rsli.name,rsli.columnname as column,sum(rsli.value) as value "
            + "from siglusintegration.regimen_summary_line_items rsli "
            + "where rsli.requisitionid in "
            + QUERY_REQUISITIONS_UNDER_HIGH_LEVEL
            + " group by rsli.name,rsli.columnname",
        resultSetMapping = "RegimenSummaryLineItem.RegimenSummaryLineDto"),
    @NamedNativeQuery(
        name = "RegimenSummaryLineItem.maxValueRequisitionsInLastPeriods",
        query = "select name,columnname as column,sum(maxvalue) as value from "
            + "(select rsli.name,rsli.columnname,max(rsli.value) as maxvalue "
            + "from siglusintegration.regimen_summary_line_items rsli "
            + "left join requisition.requisitions r on r.id = rsli.requisitionid "
            + "where rsli.requisitionid in "
            + QUERY_MAX_VALUE_IN_LAST_PERIODS
            + "group by rsli.name,rsli.columnname,r.facilityid) maxtmp "
            + "group by name, columnname",

        resultSetMapping = "RegimenSummaryLineItem.RegimenSummaryLineDto")
})
@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "RegimenSummaryLineItem.RegimenSummaryLineDto",
        classes = @ConstructorResult(
            targetClass = RegimenSummaryLineDto.class,
            columns = {
                @ColumnResult(name = "name", type = String.class),
                @ColumnResult(name = "column", type = String.class),
                @ColumnResult(name = "value", type = Integer.class),
            }
        )
    )
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegimenSummaryLineDto {

  private String name;

  private String column;

  private Integer value;

  // column: value
  private Map<String, RegimenColumnDto> columns;

  // comment by yyd: This constructor will be used after the above query, do not delete it
  @SuppressWarnings("unused")
  public RegimenSummaryLineDto(String name, String column, Integer value) {
    this.name = name;
    this.column = column;
    this.value = value;
  }

  public static List<RegimenSummaryLineDto> from(List<RegimenSummaryLineItem> lineItems) {
    List<RegimenSummaryLineDto> regimenSummaryLineDtos = newArrayList();

    Map<String, List<RegimenSummaryLineItem>> groupBySummaryRow = lineItems.stream()
        .collect(Collectors.groupingBy(RegimenSummaryLineItem::getName));

    groupBySummaryRow.forEach((rowName, regimenSummaryLineItems) -> {

      Map<String, RegimenColumnDto> columnMap = regimenSummaryLineItems.stream()
          .collect(Collectors.toMap(RegimenSummaryLineItem::getColumn,
              regimenSummaryLineItem -> RegimenColumnDto.builder()
                  .id(regimenSummaryLineItem.getId())
                  .value(regimenSummaryLineItem.getValue())
                  .build()));

      RegimenSummaryLineDto lineDto = new RegimenSummaryLineDto();
      lineDto.setColumns(columnMap);
      lineDto.setName(rowName);

      regimenSummaryLineDtos.add(lineDto);
    });

    return regimenSummaryLineDtos;

  }

  public String getNameColumn() {
    return name + SEPARATOR + column;
  }


}
