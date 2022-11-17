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

import java.time.LocalDate;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "HfCmm.findHfCmmCountDto",
        query = "select facilitycode, periodbegin, count(*)\n"
            + "from siglusintegration.hf_cmms\n"
            + "where periodbegin in :periodStartDates\n"
            + "group by facilitycode, periodbegin;",
        resultSetMapping = "HfCmm.HfCmmCountDto"),
    @NamedNativeQuery(
        name = "HfCmm.findOneHfCmmCountDto",
        query = "select facilitycode, periodbegin, count(*)\n"
            + "from siglusintegration.hf_cmms\n"
            + "where periodbegin in :periodStartDates\n"
            + "and facilitycode = :facilityCode\n"
            + "group by facilitycode, periodbegin;",
        resultSetMapping = "HfCmm.HfCmmCountDto")}
)
@MappedSuperclass
@SqlResultSetMapping(
    name = "HfCmm.HfCmmCountDto",
    classes = @ConstructorResult(
        targetClass = HfCmmCountDto.class,
        columns = {
            @ColumnResult(name = "facilitycode", type = String.class),
            @ColumnResult(name = "periodbegin", type = LocalDate.class),
            @ColumnResult(name = "count", type = Integer.class),
        }
    )
)
@Data
@Builder
@AllArgsConstructor
public class HfCmmCountDto {

  private String facilityCode;
  private LocalDate periodBegin;
  private Integer count;
}
