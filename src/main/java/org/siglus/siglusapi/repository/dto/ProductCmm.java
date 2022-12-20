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

import java.time.LocalDate;
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

@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
      name = "TracerDrugReport.cmm",
      classes =
          @ConstructorResult(
              targetClass = ProductCmm.class,
              columns = {
                @ColumnResult(name = "facilitycode", type = String.class),
                @ColumnResult(name = "productcode", type = String.class),
                @ColumnResult(name = "periodbegin", type = LocalDate.class),
                @ColumnResult(name = "cmm", type = Double.class),
              }))
})
@NamedNativeQueries({
    @NamedNativeQuery(
      name = "TracerDrug.getLastTracerDrugCmmTillDate",
      query =
          "select * from (select cmm.*,\n"
              + " row_number() over (partition by cmm.facilitycode, cmm.productcode order by cmm.periodend desc)"
              + "    from siglusintegration.hf_cmms cmm\n"
              + "    join referencedata.orderables od on od.code=cmm.productcode\n"
              + "    where cmm.facilitycode in :facilityCodes\n"
              + "        and cast(od.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb)\n"
              + "        and cmm.periodbegin  < :date) sub where sub.row_number=1",
      resultSetMapping = "TracerDrugReport.cmm"),
    @NamedNativeQuery(
      name = "TracerDrug.getTracerDrugCmm",
      query =
          "select cmm.* from siglusintegration.hf_cmms cmm "
              + "    where cmm.facilitycode in :facilityCodes "
              + "        and cmm.productcode in "
              + "            (select code from referencedata.orderables "
              + "                where cast(orderables.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb)) "
              + "        and cmm.periodbegin between :beginDate and :endDate",
      resultSetMapping = "TracerDrugReport.cmm")
})
@Data
@Builder
@AllArgsConstructor
public class ProductCmm {
  private String facilityCode;
  private String productCode;
  private LocalDate periodBegin;
  private Double cmm;
}
