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

public class SystemNotificationMessageKeys extends MessageKeys {

  private static final String ERROR = join(SERVICE_ERROR, SYSTEM_NOTIFICATION);
  private static final String START_DATE = "startDate";
  private static final String EXPIRY_DATE = "expiryDate";
  private static final String CREATED_DATE = "createdDate";
  private static final String AUTHOR = "author";
  private static final String ACTIVE = "active";
  private static final String MESSAGE = "message";

  public static final String ERROR_NULL = join(ERROR, NULL);

  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_NOT_FOUND_WITH_ID = join(ERROR_NOT_FOUND, WITH, ID);

  public static final String ERROR_CREATED_DATE_REQUIRED = join(ERROR, CREATED_DATE, REQUIRED);
  public static final String ERROR_AUTHOR_REQUIRED = join(ERROR, AUTHOR, REQUIRED);
  public static final String ERROR_ACTIVE_FLAG_REQUIRED = join(ERROR, ACTIVE, REQUIRED);
  public static final String ERROR_MESSAGE_REQUIRED = join(ERROR, MESSAGE, REQUIRED);

  public static final String ERROR_ID_MISMATCH = join(ERROR, ID_MISMATCH);
  public static final String ERROR_ID_PROVIDED = join(ERROR, ID, "provided");

  public static final String ERROR_START_DATE_AFTER_EXPIRY_DATE =
      join(ERROR, START_DATE, "after", EXPIRY_DATE);
  public static final String ERROR_EXPIRY_DATE_BEFORE_START_DATE =
      join(ERROR, EXPIRY_DATE, "before", START_DATE);

  public static final String ERROR_INVALID_PARAMS = join(ERROR, INVALID_PARAMS);
}
