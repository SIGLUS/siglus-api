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

public abstract class SupervisoryNodeMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, SUPERVISORY_NODE);
  private static final String REQUISITION_GROUP = "requisitionGroup";
  private static final String CODE = "code";

  public static final String ERROR_NULL = join(ERROR, NULL);
  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_NOT_FOUND_WITH_ID = join(ERROR_NOT_FOUND, WITH, ID);
  public static final String ERROR_CODE_REQUIRED = join(ERROR, CODE, REQUIRED);
  public static final String ERROR_CODE_MUST_BE_UNIQUE = join(ERROR, CODE, MUST_BE_UNIQUE);
  public static final String ERROR_NAME_REQUIRED = join(ERROR, NAME, REQUIRED);
  public static final String ERROR_INVALID_PARAMS = join(ERROR, SEARCH, INVALID_PARAMS);
  public static final String ERROR_NAME_MUST_BE_UNIQUE = join(ERROR, NAME, MUST_BE_UNIQUE);
  public static final String ERROR_REQUISITION_GROUP_REQUIRED =
      join(ERROR, REQUISITION_GROUP, REQUIRED);
  public static final String ERROR_UPDATING_REQUISITION_GROUP_SAVE_FAILED =
      join(ERROR, REQUISITION_GROUP, SAVE, FAILED);
}
