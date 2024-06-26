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
import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQuery(
    name = "PhysicalInventoryHistory.queryPhysicalInventoryHistory",
    query = "select "
        + "pih.id, "
        + "case when "
        + "  pih.programid = '00000000-0000-0000-0000-000000000000' then 'ALL' "
        + "else p.name "
        + "end as programname, "
        + "pih.completeddate "
        + "from siglusintegration.physical_inventories_histories pih "
        + "left join referencedata.programs p on pih.programid = p.id "
        + "where pih.facilityid = :facilityId "
        + "order by pih.processDate desc ",
    resultSetMapping = "PhysicalInventoryHistory.SiglusPhysicalInventoryHistoryDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "PhysicalInventoryHistory.SiglusPhysicalInventoryHistoryDto",
    classes = @ConstructorResult(
        targetClass = SiglusPhysicalInventoryHistoryDto.class,
        columns = {
            @ColumnResult(name = "id", type = UUID.class),
            @ColumnResult(name = "programName", type = String.class),
            @ColumnResult(name = "completedDate", type = LocalDate.class),
        }
    )
)

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SiglusPhysicalInventoryHistoryDto {

  private UUID id;
  private String programName;
  private LocalDate completedDate;
}
