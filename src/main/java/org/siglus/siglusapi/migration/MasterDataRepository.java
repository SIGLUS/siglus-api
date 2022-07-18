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

package org.siglus.siglusapi.migration;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.repository.BaseNativeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MasterDataRepository extends BaseNativeRepository {

  private final JdbcTemplate jdbc;

  public List<AdditionalOrderable> findAdditionalOrderableByProgram(UUID programId) {
    return jdbc.query("select * from datamigration.additional_program_orderables where programid=?",
        this.extractAdditionalOrderable(), programId);
  }

  private RowMapper<AdditionalOrderable> extractAdditionalOrderable() {
    return (rs, i) -> AdditionalOrderable.builder()
        .programId(readUuid(rs, "programid"))
        .orderableId(readUuid(rs, "orderableid"))
        .tradeItemId(readUuid(rs, "tradeitemid"))
        .productCode(readAsString(rs, "productcode"))
        .build();
  }
}
