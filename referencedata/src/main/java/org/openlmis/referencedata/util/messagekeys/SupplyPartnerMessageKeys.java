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

package org.openlmis.referencedata.util.messagekeys;

public abstract class SupplyPartnerMessageKeys extends MessageKeys {

  private static final String ERROR = join(SERVICE_ERROR, SUPPLY_PARTNER);
  private static final String ASSOCIATIONS = "associations";
  private static final String FACILITIES = "facilities";
  private static final String ORDERABLES = "orderables";

  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_NOT_FOUND_WITH_ID = join(ERROR_NOT_FOUND, WITH, ID);

  public static final String ERROR_ID_MISMATCH = join(ERROR, ID_MISMATCH);
  public static final String ERROR_ID_PROVIDED = join(ERROR, ID, "provided");

  public static final String ERROR_CODE_REQUIRED = join(ERROR, CODE, REQUIRED);
  public static final String ERROR_CODE_DUPLICATED = join(ERROR, CODE, DUPLICATED);

  public static final String ERROR_NAME_REQUIRED = join(ERROR, NAME, REQUIRED);

  public static final String ERROR_ASSOCIATION_DUPLICATED = join(ERROR, ASSOCIATIONS, DUPLICATED);
  public static final String ERROR_GLOBAL_UNIQUE = join(ERROR, ASSOCIATIONS, "globalUnique");

  public static final String ERROR_INVALID_SUPERVISORY_NODE =
      join(ERROR, ASSOCIATIONS, SUPERVISORY_NODE, INVALID);

  public static final String ERROR_MISSING_FACILITIES =
      join(ERROR, ASSOCIATIONS, FACILITIES, MISSING);
  public static final String ERROR_INVALID_FACILITY =
      join(ERROR, ASSOCIATIONS, FACILITIES, INVALID);

  public static final String ERROR_MISSING_ORDERABLES =
      join(ERROR, ASSOCIATIONS, ORDERABLES, MISSING);
  public static final String ERROR_INVALID_ORDERABLE =
      join(ERROR, ASSOCIATIONS, ORDERABLES, INVALID);

  public static final String ERROR_INVALID_PARAMS = join(ERROR, INVALID_PARAMS);
}
