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
    name = "TracerDrug.findTracerDrug",
    query = " select code as productCode,\n"
        + "       fullproductname as productName\n"
        + "       from referencedata.orderables where extradata @> '{\"isTracer\": true}' ",
    resultSetMapping = "TracerDrug.TracerDrugDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "TracerDrug.TracerDrugDto",
    classes = @ConstructorResult(
        targetClass = TracerDrugDto.class,
        columns = {
            @ColumnResult(name = "productCode", type = String.class),
            @ColumnResult(name = "productName", type = String.class),
        }
    )
)

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TracerDrugDto {

  private String productCode;
  private String productName;
}
