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

import java.sql.Date;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.siglus.siglusapi.domain.TracerDrugPersistentData;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class TracerDrugRepositoryImpl implements TracerDrugRepositoryCustom {
  private final JdbcTemplate jdbcTemplate;

  @Override
  public void batchInsertOrUpdate(List<TracerDrugPersistentData> tracerDrugPersistentData) {
    if (CollectionUtils.isEmpty(tracerDrugPersistentData)) {
      return;
    }
    jdbcTemplate.batchUpdate(
        "insert into dashboard.tracer_drug_persistent_data("
            + " facilitycode,productcode,computationtime,stockonhand,cmm) "
            + " values (?,?,?,?,?) "
            + " on conflict (productcode,facilitycode,computationtime) "
            + " do update set "
            + " stockonhand = excluded.stockonhand,cmm=excluded.cmm ",
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            TracerDrugPersistentData record = tracerDrugPersistentData.get(i);
            ps.setString(1, record.getFacilityCode());
            ps.setString(2, record.getProductCode());
            ps.setDate(3, Date.valueOf(record.getComputationTime()));
            ps.setInt(4, record.getStockOnHand());
            if (Objects.isNull(record.getCmm())) {
              ps.setNull(5, JDBCType.DOUBLE.getVendorTypeNumber());
            } else {
              ps.setDouble(5, record.getCmm());
            }
          }

          @Override
          public int getBatchSize() {
            return tracerDrugPersistentData.size();
          }
        });
  }
}
