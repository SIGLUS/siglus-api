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
        name = "TestConsumptionLineItem.sumValueRequisitionsUnderHighLevelFacility",
        query =
            "select rtcli.service,rtcli.project,rtcli.outcome,sum(rtcli.value) as value "
                + "from siglusintegration.rapid_test_consumption_line_items rtcli "
                + "where rtcli.requisitionid in "
                + QUERY_REQUISITIONS_UNDER_HIGH_LEVEL
                + " group by rtcli.service,rtcli.project,rtcli.outcome",
        resultSetMapping = "TestConsumptionLineItem.TestConsumptionOutcomeDto"),
    @NamedNativeQuery(
        name = "TestConsumptionLineItem.maxValueRequisitionsInLastPeriods",
        query =
            "select service, project, outcome, sum(maxvalue) as value from "
                + "(select rtcli.service, rtcli.project, rtcli.outcome, max(rtcli.value) as maxvalue "
                + "from siglusintegration.rapid_test_consumption_line_items rtcli "
                + "left join requisition.requisitions r on r.id = rtcli.requisitionid "
                + "where rtcli.requisitionid in "
                + QUERY_MAX_VALUE_IN_LAST_PERIODS
                + "group by rtcli.service, rtcli.project, rtcli.outcome,r.facilityid) maxtmp "
                + "group by service, project, outcome",
        resultSetMapping = "TestConsumptionLineItem.TestConsumptionOutcomeDto")
})
@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "TestConsumptionLineItem.TestConsumptionOutcomeDto",
        classes = @ConstructorResult(
            targetClass = TestConsumptionOutcomeDto.class,
            columns = {
                @ColumnResult(name = "service", type = String.class),
                @ColumnResult(name = "project", type = String.class),
                @ColumnResult(name = "outcome", type = String.class),
                @ColumnResult(name = "value", type = Integer.class),
            }
        )
    )
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestConsumptionOutcomeDto {

  private UUID testConsumptionLineItemId;

  private String service;

  private String project;

  private String outcome;

  private Integer value;

  // comment by yyd: This constructor will be used after the above query, do not delete it
  @SuppressWarnings("unused")
  public TestConsumptionOutcomeDto(String service, String project, String outcome, Integer value) {
    this.service = service;
    this.project = project;
    this.outcome = outcome;
    this.value = value;
  }

  public String getServiceProjectOutcome() {
    return service + SEPARATOR + project + SEPARATOR + outcome;
  }

}
