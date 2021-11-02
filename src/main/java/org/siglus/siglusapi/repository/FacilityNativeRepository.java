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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.android.db.Facility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
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

}
