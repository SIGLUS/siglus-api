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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyReportFacility;
import org.siglus.siglusapi.dto.android.db.Facility;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class FacilityNativeRepository extends BaseNativeRepository {

  private final NamedParameterJdbcTemplate namedJdbc;

  public Page<Facility> findAllForStockMovements(Collection<UUID> facilityTypeIds, LocalDate since,
      Pageable pageable) {
    String query =
        "SELECT DISTINCT f.id, f.code, f.name, f.description, f.active  " + generateFrom(pageable, true, false);
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("typeIds", facilityTypeIds);
    params.addValue("since", since);
    params.addValue("till", LocalDate.now().minusDays(1));
    List<Facility> list = namedJdbc.query(query, params, facilityExtractor());
    if (list.size() < pageable.getPageSize()) {
      return new PageImpl<>(list, pageable, (long) pageable.getOffset() + list.size());
    }
    String countQuery = "SELECT COUNT(DISTINCT f.code) " + generateFrom(pageable, true, true);
    Long total = namedJdbc.query(countQuery, params, (rs -> rs.next() ? rs.getLong(1) : null));
    return new PageImpl<>(list, pageable, total);
  }

  public Page<Facility> findAllForStockOnHand(Collection<UUID> facilityTypeIds, LocalDate at,
      Pageable pageable) {
    String query =
        "SELECT DISTINCT f.id, f.code, f.name, f.description, f.active  " + generateFrom(pageable, false, false);
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("typeIds", facilityTypeIds);
    params.addValue("at", at);
    List<Facility> list = namedJdbc.query(query, params, facilityExtractor());
    if (list.size() < pageable.getPageSize()) {
      return new PageImpl<>(list, pageable, (long) pageable.getOffset() + list.size());
    }
    String countQuery = "SELECT COUNT(DISTINCT f.code) " + generateFrom(pageable, false, true);
    Long total = namedJdbc.query(countQuery, params, (rs -> rs.next() ? rs.getLong(1) : null));
    return new PageImpl<>(list, pageable, total);
  }

  private String generateFrom(Pageable pageable, boolean forMovements, boolean countQuery) {
    String from = "FROM "
        + "referencedata.facilities f, stockmanagement.stock_cards sc, stockmanagement.calculated_stocks_on_hand cal "
        + "WHERE f.id = sc.facilityid "
        + "AND cal.stockcardid = sc.id "
        + "AND f.typeid NOT IN (:typeIds) ";
    if (forMovements) {
      from += "AND cal.occurreddate BETWEEN :since AND :till ";
    } else {
      from += "AND cal.occurreddate <= :at ";
    }
    if (!countQuery) {
      from += "ORDER BY f.code LIMIT " + pageable.getPageSize() + " OFFSET " + pageable.getOffset();
    }
    return from;
  }

  private RowMapper<Facility> facilityExtractor() {
    return (rs, i) ->
        new Facility(readId(rs), readAsString(rs, "code"), readAsString(rs, "name"),
            readAsString(rs, "description"), readAsBoolean(rs, "active"));
  }


  public List<RequisitionMonthlyReportFacility> queryAllFacilityInfo() {
    String query = "SELECT f.id  AS facilityid, "
        + "       f.name AS facilityname, "
        + "       f.code      AS facilitycode, "
        + "       ft.code      AS facilitytype, "
        + "       ftm.category AS facilitymergetype, "
        + "       z1.name district, "
        + "       z2.name province, "
        + "       vfs.districtfacilitycode, "
        + "       vfs.provincefacilitycode "
        + "FROM referencedata.facilities f "
        + "         LEFT JOIN referencedata.geographic_zones z1 ON f.geographiczoneid = z1.id "
        + "         LEFT JOIN referencedata.geographic_zones z2 ON z1.parentid = z2.id "
        + "         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code "
        + "         LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id "
        + "         LEFT JOIN siglusintegration.facility_type_mapping ftm "
        + "ON ftm.facilitytypecode = ft.code";
    return namedJdbc.query(query, requisitionMonthlyReportFacilityExtractor());
  }

  private RowMapper<RequisitionMonthlyReportFacility> requisitionMonthlyReportFacilityExtractor() {
    return (rs, i) ->
        new RequisitionMonthlyReportFacility(readAsString(rs, "district"),
            readAsString(rs, "province"),
            readAsString(rs, "facilityname"),
            readAsString(rs, "facilitycode"),
            readUuid(rs, "facilityid"),
            readAsString(rs, "facilitytype"),
            readAsString(rs, "facilitymergetype"),
            readAsString(rs, "districtfacilitycode"),
            readAsString(rs, "provincefacilitycode"));
  }

  public List<FacillityStockCardDateDto> findFirstStockCardGroupByFacility() {
    String query = getQuery();
    return namedJdbc.query(query, facilityStockCardDateDtoExtractor());
  }

  private String getQuery() {
    return "SELECT DISTINCT ON (sc.facilityid, sc.programid) "
        + "            MIN(scli.occurreddate) OVER (PARTITION BY sc.facilityid, sc.programid) AS occurreddate, "
        + "            sc.facilityid, "
        + "            sc.programid, "
        + "            sf.isandroid "
        + "FROM stockmanagement.stock_card_line_items scli "
        + "         LEFT JOIN stockmanagement.stock_cards sc ON scli.stockcardid = sc.id "
        + "         LEFT JOIN siglusintegration.facility_extension sf ON sc.facilityid = sf.facilityid ";
  }

  public List<FacillityStockCardDateDto> findMalariaFirstStockCardGroupByFacility(
      Set<UUID> malariaAdditionalOrderableIds, UUID viaProgramId) {
    if (CollectionUtils.isEmpty(malariaAdditionalOrderableIds)) {
      return new ArrayList<>();
    }
    String query = getQuery() + " WHERE sc.programid = :programId "
        + " AND sc.orderableid in (:orderableIds)";
    log.info(query);
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("programId", viaProgramId);
    params.addValue("orderableIds", malariaAdditionalOrderableIds);
    return namedJdbc.query(query, params, facilityStockCardDateDtoExtractor());
  }

  private RowMapper<FacillityStockCardDateDto> facilityStockCardDateDtoExtractor() {
    return (rs, i) ->
        new FacillityStockCardDateDto(readAsDate(rs, "occurreddate"),
            readUuid(rs, "facilityid"),
            readUuid(rs, "programid"),
            readAsBoolean(rs, "isandroid"));
  }

  public List<FacilityProgramPeriodScheduleDto> findFacilityProgramPeriodSchedule() {
    String query = "select  gps.processingscheduleid, gm.facilityid, gps.programid "
        + "from referencedata.requisition_group_members gm "
        + "left join referencedata.requisition_group_program_schedules gps "
        + "on gm.requisitiongroupid = gps.requisitiongroupid";
    return namedJdbc.query(query, facilityProgramPeriodScheduleDtoExtractor());
  }

  private RowMapper<FacilityProgramPeriodScheduleDto> facilityProgramPeriodScheduleDtoExtractor() {
    return (rs, i) ->
        new FacilityProgramPeriodScheduleDto(readUuid(rs, "processingscheduleid"),
            readUuid(rs, "facilityid"),
            readUuid(rs, "programid"));
  }

}
