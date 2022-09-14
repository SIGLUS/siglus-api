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

package org.siglus.siglusapi.repository;

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.TracerDrugPersistentData;
import org.siglus.siglusapi.dto.RequisitionGeographicZonesDto;
import org.siglus.siglusapi.dto.TracerDrugDto;
import org.siglus.siglusapi.dto.TracerDrugExcelDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;


public interface TracerDrugRepository extends
    JpaRepository<TracerDrugPersistentData, UUID> {


  @Modifying
  @Query(value =
      "insert into dashboard.tracer_drug_persistent_data(productcode, facilitycode, stockonhand, computationtime, cmm)"
          + "\n"
          + "select productcode,\n"
          + "       facilitycode,\n"
          + "       totalsohatthattime,\n"
          + "       mondaydate,\n"
          + "       cmm\n"
          + "from (select (select sum(soh.stockonhand) as totalsohatthattime\n"
          + "              from (select sc.facilityid,\n"
          + "                           sc.orderableid,\n"
          + "                           csoh.stockonhand,\n"
          + "                           csoh.occurreddate,\n"
          + "                           row_number() over (partition by sc.id\n"
          + "                               order by\n"
          + "                                   csoh.occurreddate desc)\n"
          + "                    from (select *\n"
          + "                          from stockmanagement.calculated_stocks_on_hand csoh\n"
          + "                          where csoh.occurreddate <= bigunion.day) as csoh\n"
          + "                             left join stockmanagement.stock_cards sc on\n"
          + "                        sc.id = csoh.stockcardid) soh\n"
          + "              where soh.row_number = 1\n"
          + "                and soh.orderableid = bigunion.orderableid\n"
          + "                and soh.facilityid = bigunion.facilityid\n"
          + "              group by soh.facilityid,\n"
          + "                       soh.orderableid)                             as totalsohatthattime,\n"
          + "\n"
          + "             (select distinct first_value(cmm) over (partition by hc.facilitycode,\n"
          + "                 hc.productcode order by hc.periodend DESC) as cmm\n"
          + "              from (select *\n"
          + "                    from siglusintegration.hf_cmms\n"
          + "                    where periodend <= bigunion.day\n"
          + "                      and productcode = bigunion.productcode\n"
          + "                      and facilitycode = bigunion.facilitycode) hc) as cmm,\n"
          + "             bigunion.day                                           as mondaydate,\n"
          + "             bigunion.productcode                                   as productcode,\n"
          + "             bigunion.facilitycode                                  as facilitycode\n"
          + "      from (select *\n"
          + "            from (select f.id   as facilityid,\n"
          + "                         o.id   as orderableid,\n"
          + "                         o.code as productcode,\n"
          + "                         f.code as facilitycode\n"
          + "                  from (select distinct on\n"
          + "                      (id) *,\n"
          + "                           max(versionnumber) over (partition by id)\n"
          + "                        from referencedata.orderables\n"
          + "                        where cast(orderables.extradata as jsonb) @> cast('{\n"
          + "                          \"isTracer\": true\n"
          + "                        }' as jsonb)) o\n"
          + "                           cross join (select distinct on\n"
          + "                      (facilityid) facilityid\n"
          + "                                       from stockmanagement.stock_cards) scf\n"
          + "                           left join referencedata.facilities f on scf.facilityid = f.id) as main\n"
          + "                     cross join\n"
          + "                 (select date(t) as day\n"
          + "                  from generate_series(cast(?1  AS date) , cast(?2  AS date) , '1 days') as t\n"
          + "                  where to_char(t, 'DAY') = to_char(date '1970-01-05', 'DAY')) as mondaydate) as bigunion)"
          + " as bigbigunion\n"
          + "where bigbigunion.totalsohatthattime is not null\n"
          + "on conflict (productcode,facilitycode,computationtime) do update set "
          + "                                                        stockonhand = excluded.stockonhand,\n"
          + "                                                        cmm         = excluded.cmm;\n", nativeQuery = true)
  int insertDataWithinSpecifiedTime(String startDate, String endDate);

  @Modifying
  @Query(value =
      "insert into dashboard.tracer_drug_persistent_data(productcode, facilitycode, stockonhand, computationtime, cmm)"
          + "\n"
          + "select productcode,\n"
          + "       facilitycode,\n"
          + "       totalsohatthattime,\n"
          + "       mondaydate,\n"
          + "       cmm\n"
          + "from (select (select sum(soh.stockonhand) as totalsohatthattime\n"
          + "              from (select sc.facilityid,\n"
          + "                           sc.orderableid,\n"
          + "                           csoh.stockonhand,\n"
          + "                           csoh.occurreddate,\n"
          + "                           row_number() over (partition by sc.id\n"
          + "                               order by\n"
          + "                                   csoh.occurreddate desc)\n"
          + "                    from (select *\n"
          + "                          from stockmanagement.calculated_stocks_on_hand csoh\n"
          + "                          where csoh.occurreddate <= bigunion.day) as csoh\n"
          + "                             left join stockmanagement.stock_cards sc on\n"
          + "                        sc.id = csoh.stockcardid) soh\n"
          + "              where soh.row_number = 1\n"
          + "                and soh.orderableid = bigunion.orderableid\n"
          + "                and soh.facilityid = bigunion.facilityid\n"
          + "              group by soh.facilityid,\n"
          + "                       soh.orderableid)                             as totalsohatthattime,\n"
          + "\n"
          + "             (select distinct first_value(cmm) over (partition by hc.facilitycode,\n"
          + "                 hc.productcode order by hc.periodend DESC) as cmm\n"
          + "              from (select *\n"
          + "                    from siglusintegration.hf_cmms\n"
          + "                    where periodend <= bigunion.day\n"
          + "                      and productcode = bigunion.productcode\n"
          + "                      and facilitycode = bigunion.facilitycode) hc) as cmm,\n"
          + "             bigunion.day                                           as mondaydate,\n"
          + "             bigunion.productcode                                   as productcode,\n"
          + "             bigunion.facilitycode                                  as facilitycode\n"
          + "      from (select *\n"
          + "            from (select f.id   as facilityid,\n"
          + "                         o.id   as orderableid,\n"
          + "                         o.code as productcode,\n"
          + "                         f.code as facilitycode\n"
          + "                  from (select distinct on\n"
          + "                      (id) *,\n"
          + "                           max(versionnumber) over (partition by id)\n"
          + "                        from referencedata.orderables\n"
          + "                        where cast(orderables.extradata as jsonb) @> cast('{\n"
          + "                          \"isTracer\": true\n"
          + "                        }' as jsonb)) o\n"
          + "                           cross join (select distinct on\n"
          + "                                 (id) id,code\n"
          + "                                       from referencedata.facilities where id in (?3)) f) as main\n"
          + "                     cross join\n"
          + "                 (select date(t) as day\n"
          + "                  from generate_series(cast(?1  AS date) , cast(?2  AS date) , '1 days') as t\n"
          + "                  where to_char(t, 'DAY') = to_char(date '1970-01-05', 'DAY')) as mondaydate) as bigunion)"
          + " as bigbigunion\n"
          + "where bigbigunion.totalsohatthattime is not null\n"
          + "on conflict (productcode,facilitycode,computationtime) do update set "
          + "                                                        stockonhand = excluded.stockonhand,\n"
          + "                                                        cmm         = excluded.cmm;\n", nativeQuery = true)
  int insertDataWithinSpecifiedTimeByFacilityIds(String startDate, String endDate, List<UUID> facilityIds);

  @Query(name = "RequisitionGeographicZone.findAllZones", nativeQuery = true)
  List<RequisitionGeographicZonesDto> getAllRequisitionGeographicZones();

  @Query(name = "TracerDrug.findTracerDrug", nativeQuery = true)
  List<TracerDrugDto> getTracerDrugInfo();

  @Query(name = "TracerDrug.findExcelDetail", nativeQuery = true)
  List<TracerDrugExcelDto> getTracerDrugExcelInfo(String startDate,
      String endDate, String productCode, List<String> facilityCodes);


}
