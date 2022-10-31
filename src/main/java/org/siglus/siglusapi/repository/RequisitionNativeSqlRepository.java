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

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.SimpleRequisitionDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequisitionNativeSqlRepository extends BaseNativeRepository {

  private final NamedParameterJdbcTemplate namedJdbc;

  public List<SimpleRequisitionDto> findSimpleRequisitionDto(Collection<UUID> requisitionIds) {
    String sql = "select r.id, r.extradata\n"
        + "from requisition.requisitions r \n"
        + "where r.id in (:requisitionIds)";
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("requisitionIds", requisitionIds);
    return executeQuery(sql, parameters,
        ((rs, rowNum) -> SimpleRequisitionDto.builder()
            .id(readUuid(rs, "id"))
            .extraData(readAsString(rs, "extradata"))
            .build()
        )
    );
  }

  private <T> List<T> executeQuery(String sql, SqlParameterSource parameters, RowMapper<T> transformer) {
    return namedJdbc.query(sql, parameters, new RowMapperResultSetExtractor<>(transformer));
  }

}
