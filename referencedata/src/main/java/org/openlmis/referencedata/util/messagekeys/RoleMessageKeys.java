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

public abstract class RoleMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, ROLE);

  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_MUST_HAVE_A_RIGHT = join(ERROR, "mustHaveARight");
  public static final String ERROR_RIGHTS_ARE_DIFFERENT_TYPES =
      join(ERROR, "rightsAreDifferentTypes");
  public static final String ERROR_INVALID_PARAMS = join(ERROR, SEARCH, INVALID_PARAMS);
  public static final String ERROR_MUST_HAVE_A_UNIQUE_NAME = join(ERROR, MUST_BE_UNIQUE);
}
