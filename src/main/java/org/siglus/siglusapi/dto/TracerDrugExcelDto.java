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

import java.util.Date;
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
    name = "TracerDrug.findExcelDetail",
    query = " select maindata.code as productCode,\n"
        + "       maindata.programcode as programCode,\n"
        + "       maindata.fullproductname as productName,\n"
        + "       maindata.provincename as provinceName,\n"
        + "       maindata.districtname as districtName,\n"
        + "       maindata.facilityname as facilityName,\n"
        + "       maindata.facilitycode  as facilityCode,\n"
        + "       maindata.day as computationTime,\n"
        + "       tracertdpd.stockonhand as stockOnHand,\n"
        + "       tracertdpd.closedcmm as closedCmm,\n"
        + "       case\n"
        + "           when tracertdpd.stockonhand is null then 0\n"
        + "           when tracertdpd.stockonhand = 0 then 1 \n"
        + "           when tracertdpd.cmm = cast('-1' as double precision)\n"
        + "               or tracertdpd.cmm is null then 3 \n"
        + "           when cast(tracertdpd.stockonhand as double precision) < "
        + "round(cast(tracertdpd.cmm as numeric), 2) then 2 \n"
        + "           when cast(maindata.programcode as text) = cast('TARV' as text)\n"
        + "               and (round(cast(tracertdpd.cmm as numeric), 2) * cast(3 as double precision)) < "
        + "cast(tracertdpd.stockonhand as double precision)\n"
        + "               then 4 \n"
        + "           when cast(maindata.programcode as text) <> cast('TARV' as text)\n"
        + "               and (round(cast(tracertdpd.cmm as numeric), 2) * cast(2 as double precision)) < "
        + "cast(tracertdpd.stockonhand as double precision)\n"
        + "               then 4 \n"
        + "           else 3 \n"
        + "           end as stockStatusColorCode\n"
        + "           from\n"
        + "    (\n"
        + "    select tracero.code,tracero.programcode,tracero.fullproductname,"
        + "tracerf.facilitycode,tracerf.facilityname,tracerf.districtname,tracerf.provincename,td.day from\n"
        + "(\n"
        + "        select distinct on\n"
        + "                          (o.id) o.id,fullproductname,o.code,prnm.programcode,\n"
        + "                               max(versionnumber) over (partition by o.id)\n"
        + "                            from  referencedata.orderables o\n"
        + "        left join referencedata.program_orderables po on o.id = po.orderableid\n"
        + "        left join siglusintegration.program_requisition_name_mapping prnm on prnm.programid = po.programid\n"
        + "                                                   where o.extradata @> '{\n"
        + "                     \"isTracer\": true\n"
        + "                   }' and o.code = ?3 \n"
        + "           ) as tracero\n"
        + "\n"
        + "CROSS JOIN\n"
        + "           (select distinct on (f.code) f.code as facilitycode, f.name as facilityname, "
        + "gz.name as districtname,gz_prov.name as provincename from referencedata.facilities f\n"
        + "                           left join referencedata.geographic_zones gz on\n"
        + "                      gz.id = f.geographiczoneid\n"
        + "                           left join referencedata.geographic_zones gz_prov on\n"
        + "                      gz_prov.id = gz.parentid\n"
        + "                                                           where f.code in ?4 ) as tracerf\n"
        + "CROSS JOIN (select date(t) as day\n"
        + "                  from generate_series(cast(?1 as date), cast(?2 as date), '1 days') as t\n"
        + "                  where to_char(t, 'DAY') = to_char(date '1970-01-05', 'DAY')) as td\n"
        + "    ) as maindata\n"
        + "\n"
        + "left join (\n"
        + "    select tdpd.stockonhand,\n"
        + "           tdpd.productcode,\n"
        + "           tdpd.facilitycode,\n"
        + "           tdpd.computationtime,\n"
        + "           tdpd.cmm,\n"
        + "           first_value(tdpd.cmm) over (PARTITION BY (tdpd.facilitycode, tdpd.productcode) ORDER BY "
        + "tdpd.computationtime DESC)  as closedcmm\n"
        + "            from dashboard.tracer_drug_persistent_data tdpd) tracertdpd\n"
        + "on tracertdpd.productcode = maindata.code and tracertdpd.facilitycode = maindata.facilitycode and "
        + "tracertdpd.computationtime = maindata.day; ",
    resultSetMapping = "TracerDrug.TracerDrugExcelDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "TracerDrug.TracerDrugExcelDto",
    classes = @ConstructorResult(
        targetClass = TracerDrugExcelDto.class,
        columns = {
            @ColumnResult(name = "productCode", type = String.class),
            @ColumnResult(name = "programCode", type = String.class),
            @ColumnResult(name = "productName", type = String.class),
            @ColumnResult(name = "provinceName", type = String.class),
            @ColumnResult(name = "districtName", type = String.class),
            @ColumnResult(name = "facilityName", type = String.class),
            @ColumnResult(name = "facilityCode", type = String.class),
            @ColumnResult(name = "computationTime", type = Date.class),
            @ColumnResult(name = "stockOnHand", type = Integer.class),
            @ColumnResult(name = "closedCmm", type = Double.class),
            @ColumnResult(name = "stockStatusColorCode", type = Integer.class)
        }
    )
)

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TracerDrugExcelDto {

  private String productCode;
  private String programCode;
  private String productName;
  private String provinceName;
  private String districtName;
  private String facilityName;
  private String facilityCode;
  private Date computationTime;
  private Integer stockOnHand;
  private Double closedCmm;
  private Integer stockStatusColorCode;

}
