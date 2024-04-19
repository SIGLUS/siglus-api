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
        + "pih.id as physicalinventoryhistoryid, "
        + "pih.groupid, "
        + "pih.physicalinventoryextensionid, "
        + "pie.physicalinventoryid, "
        + "p.name as programname, "
        + "pi.occurreddate as completeddate "
        + "from siglusintegration.physical_inventories_histories pih "
        + "left join siglusintegration.physical_inventories_extension pie on pih.physicalinventoryextensionid = pie.id "
        + "left join stockmanagement.physical_inventories pi on pie.physicalinventoryid = pi.id "
        + "left join referencedata.programs p on pi.programid = p.id "
        + "where pih.facilityid = :facilityId and pi.isdraft = 'false' "
        + "order by pi.occurreddate desc, p.name ",
    resultSetMapping = "PhysicalInventoryHistory.SiglusPhysicalInventoryHistoryDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "PhysicalInventoryHistory.SiglusPhysicalInventoryHistoryDto",
    classes = @ConstructorResult(
        targetClass = SiglusPhysicalInventoryHistoryDto.class,
        columns = {
            @ColumnResult(name = "physicalInventoryHistoryId", type = UUID.class),
            @ColumnResult(name = "groupId", type = UUID.class),
            @ColumnResult(name = "physicalInventoryExtensionId", type = UUID.class),
            @ColumnResult(name = "physicalInventoryId", type = UUID.class),
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

  private UUID physicalInventoryHistoryId;
  private UUID groupId;
  private UUID physicalInventoryExtensionId;
  private UUID physicalInventoryId;
  private String programName;
  private LocalDate completedDate;
}
