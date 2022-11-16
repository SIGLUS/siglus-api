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
        name = "AgeGroupLineItem.sumValueRequisitionsUnderHighLevelFacility",
        query =
            "select agli.groupname as service,agli.columnname as group,sum(agli.value) as value "
                + "from siglusintegration.age_group_line_items agli "
                + "where agli.requisitionid in "
                + QUERY_REQUISITIONS_UNDER_HIGH_LEVEL
                + " group by agli.groupname,agli.columnname",
        resultSetMapping = "AgeGroupLineItem.AgeGroupLineItemDto"),
    @NamedNativeQuery(
        name = "AgeGroupLineItem.maxValueRequisitionsInLastPeriods",
        query = "select groupname as service,columnname as group,sum(maxvalue) as value from "
            + "(select agli.groupname,agli.columnname,max(agli.value) as maxvalue "
            + "from siglusintegration.age_group_line_items agli "
            + "left join requisition.requisitions r on r.id = agli.requisitionid "
            + "where agli.requisitionid in "
            + QUERY_MAX_VALUE_IN_LAST_PERIODS
            + "group by agli.groupname,agli.columnname,r.facilityid) maxtmp "
            + "group by groupname, columnname",
        resultSetMapping = "AgeGroupLineItem.AgeGroupLineItemDto")
})
@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "AgeGroupLineItem.AgeGroupLineItemDto",
        classes = @ConstructorResult(
            targetClass = AgeGroupLineItemDto.class,
            columns = {
                @ColumnResult(name = "service", type = String.class),
                @ColumnResult(name = "group", type = String.class),
                @ColumnResult(name = "value", type = Integer.class),
            }
        )
    )
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgeGroupLineItemDto {

  private UUID ageGroupLineItemId;

  private String service;

  private String group;

  private Integer value;

  // comment by yyd: This constructor will be used after the above query, do not delete it
  @SuppressWarnings("unused")
  public AgeGroupLineItemDto(String service, String group, Integer value) {
    this.service = service;
    this.group = group;
    this.value = value;
  }

  public String getMappingKey() {
    return service + SEPARATOR + group;
  }

}
