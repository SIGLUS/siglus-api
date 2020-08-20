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

public class RepositoryConstants {

  static final String SELECT_PROGRAM_ORDERABLE = "Select * "
      + "FROM referencedata.program_orderables po ";
  static final String WHERE_LATEST_PROGRAM_ORDERABLE = "WHERE "
      + "(po.orderableid, po.orderableversionnumber) "
      + "          IN (SELECT po.orderableid, MAX(po.orderableversionnumber) "
      + "FROM referencedata.program_orderables po GROUP BY po.orderableid) ";

  static final String FROM_ORDERABLES_CLAUSE = " FROM Orderable o";
  static final String WHERE_LATEST_ORDERABLE = " WHERE (o.identity.id, o.identity.versionNumber)"
      + " IN (SELECT identity.id, MAX(identity.versionNumber)"
      + " FROM Orderable GROUP BY identity.id)";
  static final String ORDER_BY_PAGEABLE = " ORDER BY ?#{#pageable}";
  static final String SELECT_ORDERABLE = "Select o";


  public static final String SELECT_PROGRAM_ADDITIONAL_ORDERABLE =
      "select new org.siglus.common.dto.ProgramAdditionalOrderableDto(ao.id, ao.programId, "
          + "ao.additionalOrderableId, "
          + "o.productCode, o.fullProductName, o.description, ao.orderableOriginProgramId) ";

  public static final String FROM_ADDITIONAL_ORDERABLE = "from Orderable o, "
      + "ProgramAdditionalOrderable ao ";

  public static final String WHERE_QUERY_ADDITIONAL_ORDERABLE = "where o.identity.versionNumber = "
      + "(select max(oo.identity.versionNumber) from Orderable oo "
      + "where o.identity.id = oo.identity.id) "
      + "and ao.additionalOrderableId = o.identity.id "
      + "and ao.programId = :programId "
      + "and upper(o.productCode.code) like :code "
      + "and upper(o.fullProductName) like :name ";

  public static final String AND_ORDERABLE_ORIGIN_PROGRAM_ID = "and ao.orderableOriginProgramId "
      + "= :orderableOriginProgramId";

  private RepositoryConstants() {
  }
}
