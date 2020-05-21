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

package org.openlmis.referencedata.repository;

class RepositoryConstants {

  static final String FROM_ORDERABLES_CLAUSE = " FROM Orderable o";
  static final String FROM_REFERENCEDATA_ORDERABLES_CLAUSE = " FROM referencedata.orderables o";
  static final String WHERE_LATEST_ORDERABLE = " WHERE (o.identity.id, o.identity.versionNumber)"
      + " IN (SELECT identity.id, MAX(identity.versionNumber)"
      + " FROM Orderable GROUP BY identity.id)";
  static final String JOIN_WITH_LATEST_ORDERABLE = " JOIN (SELECT id, MAX(versionNumber)"
          + " AS versionNumber FROM referencedata.orderables GROUP BY id) AS latest"
          + " ON o.id = latest.id AND o.versionNumber = latest.versionNumber";
  static final String ORDER_BY_PAGEABLE = " ORDER BY ?#{#pageable}";
  static final String SELECT_ORDERABLE = "Select o";
  static final String SELECT_LAST_UPDATED = "SELECT o.lastupdated";
  static final String ORDER_BY_LAST_UPDATED_DESC_LIMIT_1 = " ORDER BY o.lastupdated DESC LIMIT 1";

  private RepositoryConstants() {}
}
