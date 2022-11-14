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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "UsageInformationLineItem.sumValueRequisitionsUnderHighLevelFacility",
        query =
            "select uili.service,uili.information,uili.orderableid as orderableId,sum(uili.value) as value "
                + "from siglusintegration.usage_information_line_items uili "
                + "where uili.requisitionid in "
                + QUERY_REQUISITIONS_UNDER_HIGH_LEVEL
                + " group by uili.service,uili.information,uili.orderableid",
        resultSetMapping = "UsageInformationLineItem.UsageInformationOrderableDto"),
    @NamedNativeQuery(
        name = "UsageInformationLineItem.maxValueRequisitionsInLastPeriods",
        query = "select service, information, orderableid as orderableId,sum(maxvalue) as value from "
            + "(select uili.service,uili.information,uili.orderableid,"
            + "max(uili.value) as maxvalue "
            + "from siglusintegration.usage_information_line_items uili "
            + "left join requisition.requisitions r on r.id = uili.requisitionid "
            + "where uili.requisitionid in "
            + QUERY_MAX_VALUE_IN_LAST_PERIODS
            + "group by uili.service,uili.information,uili.orderableid,r.facilityid) maxtmp "
            + "group by service, information, orderableid",
        resultSetMapping = "UsageInformationLineItem.UsageInformationOrderableDto")
})

@MappedSuperclass
@SqlResultSetMapping(
    name = "UsageInformationLineItem.UsageInformationOrderableDto",
    classes = @ConstructorResult(
        targetClass = UsageInformationOrderableDto.class,
        columns = {
            @ColumnResult(name = "service", type = String.class),
            @ColumnResult(name = "information", type = String.class),
            @ColumnResult(name = "orderableId", type = UUID.class),
            @ColumnResult(name = "value", type = Integer.class),
        }
    )
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsageInformationOrderableDto {

  private UUID usageInformationLineItemId;

  private String service;

  private String information;

  private UUID orderableId;

  private Integer value;

  // comment by yyd: This constructor will be used after the above query, do not delete it
  @SuppressWarnings("unused")
  public UsageInformationOrderableDto(String service, String information, UUID orderableId, Integer value) {
    this.service = service;
    this.information = information;
    this.orderableId = orderableId;
    this.value = value;
  }

  public String getServiceInformationOrderableId() {
    return service + SEPARATOR + information + SEPARATOR + orderableId;
  }

}
