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

package org.siglus.common.repository;

class RepositoryConstants {

  static final String FROM_ORDERABLES_CLAUSE = " FROM Orderable o";
  static final String WHERE_LATEST_ORDERABLE = " WHERE (o.identity.id, o.identity.versionNumber)"
      + " IN (SELECT identity.id, MAX(identity.versionNumber)"
      + " FROM Orderable GROUP BY identity.id)";
  static final String ORDER_BY_PAGEABLE = " ORDER BY ?#{#pageable}";
  static final String SELECT_ORDERABLE = "Select o";

  private RepositoryConstants() {}
}
