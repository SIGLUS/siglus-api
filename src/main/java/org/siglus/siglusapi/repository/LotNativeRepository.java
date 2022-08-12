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

import java.sql.ResultSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LotNativeRepository extends BaseNativeRepository {

  private final NamedParameterJdbcTemplate namedJdbc;

  public ProductLot findById(UUID lotId) {
    String lotQuery = "SELECT l.id, lotcode, tradeitemid, expirationdate, o.code AS productcode "
        + "FROM referencedata.lots l, referencedata.orderable_identifiers oi, referencedata.orderables o "
        + "WHERE l.tradeitemid = oi.value ::uuid "
        + "AND oi.key='tradeItem' "
        + "AND oi.orderableid = o.id "
        + "AND l.id = :lotId";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("lotId", lotId);
    return namedJdbc.query(lotQuery, params, productLotExtractor());
  }

  public ProductLot findOne(String productCode, UUID tradeItemId, String lotCode) {
    ProductLotCode code = ProductLotCode.of(productCode, lotCode);
    String lotQuery = "SELECT id, lotcode, tradeitemid, expirationdate FROM referencedata.lots "
        + "WHERE lotcode = :lotCode AND tradeitemid = :tradeItemId";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("lotCode", lotCode);
    params.addValue("tradeItemId", tradeItemId);
    return namedJdbc.query(lotQuery, params, productLotExtractor(code));
  }

  @SneakyThrows
  private Lot readLot(ResultSet rs) {
    return Lot.fromDatabase(readAsString(rs, "lotcode"), readAsDate(rs, "expirationdate"));
  }

  private ResultSetExtractor<ProductLot> productLotExtractor(ProductLotCode code) {
    return rs -> {
      if (!rs.next()) {
        return null;
      }
      return ProductLot.fromDatabase(readId(rs), code.getProductCode(), readUuid(rs, "tradeitemid"), readLot(rs));
    };
  }

  private ResultSetExtractor<ProductLot> productLotExtractor() {
    return rs -> {
      if (!rs.next()) {
        return null;
      }
      return ProductLot
          .fromDatabase(readId(rs), readAsString(rs, "productcode"), readUuid(rs, "tradeitemid"), readLot(rs));
    };
  }

}
