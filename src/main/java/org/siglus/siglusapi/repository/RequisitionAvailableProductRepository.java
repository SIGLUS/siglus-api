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
@SuppressWarnings("checkstyle:LineLength")
public class RequisitionAvailableProductRepository {

  // ORDER STATUS IN ('RECEIVED','CLOSED'), ORDER createddate before ONE YEAR
  private static final String SQL_1 = "DELETE FROM requisition.available_products ap\n"
      + "USING siglusintegration.order_external_ids oei, fulfillment.orders o\n"
      + "WHERE o.status IN ('RECEIVED','CLOSED')\n"
      + "AND ((oei.requisitionid = ap.requisitionid AND o.externalid = oei.id) OR (o.externalid = ap.requisitionid))\n"
      + "AND o.createddate < (NOW() - INTERVAL '1 YEAR')";

  // REQUISITION STATUS IS 'RELEASED_WITHOUT_ORDER', REQUISITION createddate before ONE YEAR
  private static final String SQL_2 = "DELETE FROM requisition.available_products ap\n"
      + "USING requisition.requisitions r\n"
      + "WHERE r.status = 'RELEASED_WITHOUT_ORDER'\n"
      + "AND r.id = ap.requisitionid\n"
      + "AND r.createddate < (NOW() - INTERVAL '1 YEAR')";

  private final JdbcTemplate jdbc;

  @Transactional
  public void clearRequisitionAvailableProducts() {
    Lists.newArrayList(SQL_1, SQL_2).forEach(jdbc::update);
  }
}
