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

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"checkstyle:LineLength", "PMD.AvoidDuplicateLiterals"})
public class LotManagementRepository {

  // set expired lots which are expired and SOH = 0 in all facilities for more than 6 months to inactive
  private static final String UPDATE_SQL = "UPDATE referencedata.lots SET active = FALSE\n"
      + "WHERE id IN (\n"
      + "        SELECT lotid\n"
      + "        FROM (\n"
      + "            SELECT l.id lotid, l.lotcode, l.expirationdate, sum(csoh.stockonhand) totalstockonhand\n"
      + "            FROM referencedata.lots l\n"
      + "            JOIN stockmanagement.stock_cards sc ON l.id = sc.lotid\n"
      + "            JOIN ( SELECT DISTINCT ON (stockcardid)\n"
      + "                    stockcardid, occurreddate, stockonhand\n"
      + "                FROM stockmanagement.calculated_stocks_on_hand\n"
      + "                ORDER BY stockcardid, occurreddate DESC) csoh ON sc.id = csoh.stockcardid\n"
      + "            WHERE csoh.occurreddate < (NOW() - INTERVAL '6 MONTHS')\n"
      + "                AND l.expirationdate < NOW()\n"
      + "            GROUP BY l.id) result\n"
      + "        WHERE result.totalstockonhand = 0)";

  private static final String SQL_FROM = "FROM referencedata.lots\n"
      + "WHERE id NOT IN (\n"
      + "        SELECT lotid\n"
      + "        FROM stockmanagement.stock_cards\n"
      + "        WHERE lotid IS NOT NULL)\n"
      + "    AND id NOT IN (\n"
      + "        SELECT lotid\n"
      + "        FROM stockmanagement.physical_inventory_line_items\n"
      + "        WHERE lotid IS NOT NULL)\n"
      + "    AND id NOT IN (\n"
      + "        SELECT lotid\n"
      + "        FROM fulfillment.shipment_line_items\n"
      + "        WHERE lotid IS NOT NULL)\n"
      + "    AND id NOT IN (\n"
      + "        SELECT lotid\n"
      + "        FROM fulfillment.proof_of_delivery_line_items\n"
      + "        WHERE lotid IS NOT NULL)";

  // back up to be cleared unused lots
  private static final String INSERT_SQL = "INSERT INTO siglusintegration.expired_lots_backup (id, lotcode, expirationdate, manufacturedate, tradeitemid, active) "
      + "(SELECT id, lotcode, expirationdate, manufacturedate, tradeitemid, active " + SQL_FROM + ") ON CONFLICT DO NOTHING";

  // clear lots which are not used by any facility
  private static final String DELETE_SQL = "DELETE " + SQL_FROM;

  private final JdbcTemplate jdbc;

  @Transactional
  public void clearExpiredLots() {
    Lists.newArrayList(UPDATE_SQL, INSERT_SQL, DELETE_SQL).forEach(jdbc::update);
  }
}
