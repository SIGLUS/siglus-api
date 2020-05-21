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

package org.openlmis.referencedata.domain;


public class RightName {
  public static final String GEOGRAPHIC_ZONES_MANAGE_RIGHT = "GEOGRAPHIC_ZONES_MANAGE";
  public static final String FACILITIES_MANAGE_RIGHT = "FACILITIES_MANAGE";
  public static final String USER_ROLES_MANAGE_RIGHT = "USER_ROLES_MANAGE";
  public static final String PROCESSING_SCHEDULES_MANAGE_RIGHT = "PROCESSING_SCHEDULES_MANAGE";
  public static final String USERS_MANAGE_RIGHT = "USERS_MANAGE";
  public static final String SUPERVISORY_NODES_MANAGE = "SUPERVISORY_NODES_MANAGE";
  public static final String REQUISITION_GROUPS_MANAGE = "REQUISITION_GROUPS_MANAGE";
  public static final String ORDERABLES_MANAGE = "ORDERABLES_MANAGE";
  public static final String SUPPLY_LINES_MANAGE = "SUPPLY_LINES_MANAGE";
  public static final String STOCK_ADJUSTMENT_REASONS_MANAGE = "STOCK_ADJUSTMENT_REASONS_MANAGE";
  public static final String FACILITY_APPROVED_ORDERABLES_MANAGE =
      "FACILITY_APPROVED_ORDERABLES_MANAGE";
  public static final String RIGHTS_VIEW = "RIGHTS_VIEW";
  public static final String PROGRAMS_MANAGE = "PROGRAMS_MANAGE";
  public static final String SYSTEM_IDEAL_STOCK_AMOUNTS_MANAGE =
      "SYSTEM_IDEAL_STOCK_AMOUNTS_MANAGE";
  public static final String SERVICE_ACCOUNTS_MANAGE = "SERVICE_ACCOUNTS_MANAGE";
  public static final String SUPPLY_PARTNERS_MANAGE = "SUPPLY_PARTNERS_MANAGE";
  public static final String SYSTEM_NOTIFICATIONS_MANAGE = "SYSTEM_NOTIFICATIONS_MANAGE";
  public static final String LOTS_MANAGE = "LOTS_MANAGE";

  private RightName() {
    throw new UnsupportedOperationException();
  }
}
