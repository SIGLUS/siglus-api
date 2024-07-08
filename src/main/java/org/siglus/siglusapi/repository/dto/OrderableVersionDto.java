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

import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQuery(
    name = "Orderable.findOrderablesByIds",
    query = "select  o.id as id, o.versionnumber as versionNumber, "
        + " o.code as productCode, o.fullproductname as fullProductName "
        + "from referencedata.orderables o "
        + "where o.id in (:ids)",
    resultSetMapping = "Orderable.OrderableVersionDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "Orderable.OrderableVersionDto",
    classes = @ConstructorResult(
        targetClass = OrderableVersionDto.class,
        columns = {
            @ColumnResult(name = "id", type = UUID.class),
            @ColumnResult(name = "versionNumber", type = Long.class),
            @ColumnResult(name = "productCode", type = String.class),
            @ColumnResult(name = "fullProductName", type = String.class),
        }
    )
)


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderableVersionDto {

  private UUID id;

  private Long versionNumber;

  private String productCode;

  private String fullProductName;

}
