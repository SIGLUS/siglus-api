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

public abstract class ProgramMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, PROGRAM);

  public static final String ERROR_NULL = join(ERROR, NULL);

  public static final String ERROR_ID_NULL = join(ERROR, ID, NULL);
  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_NOT_FOUND_WITH_ID = join(ERROR_NOT_FOUND, WITH, ID);
  public static final String ERROR_CODE_OR_ID_REQUIRED = join(ERROR, ID, CODE, NULL);
  public static final String ERROR_CODE_REQUIRED = join(ERROR, CODE, REQUIRED);
  public static final String ERROR_CODE_DUPLICATED = join(ERROR, CODE, DUPLICATED);
  public static final String ERROR_CODE_IS_INVARIABLE = join(ERROR, CODE, IS_INVARIABLE);
}
