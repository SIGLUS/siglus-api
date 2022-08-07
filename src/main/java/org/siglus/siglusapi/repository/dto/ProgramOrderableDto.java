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

package org.siglus.siglusapi.repository.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@NamedNativeQuery(
    name = "ProgramOrderable.findProgramOrderableDto",
    query = "select * \n"
        + "from referencedata.program_orderables\n"
        + "where (orderableid, orderableversionnumber) in\n"
        + "   (select orderableid, MAX(orderableversionnumber)\n"
        + "   from referencedata.program_orderables\n"
        + "   group by orderableid)",
    resultSetMapping = "ProgramOrderable.ProgramOrderableDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "ProgramOrderable.ProgramOrderableDto",
    classes = @ConstructorResult(
        targetClass = ProgramOrderableDto.class,
        columns = {
            @ColumnResult(name = "orderableid", type = UUID.class),
            @ColumnResult(name = "programid", type = UUID.class),
            @ColumnResult(name = "priceperpack", type = BigDecimal.class),
        }
    )
)

@Data
@Builder
@AllArgsConstructor
public class ProgramOrderableDto implements Serializable {

  private UUID orderableId;
  private UUID programId;
  private BigDecimal price;
}
