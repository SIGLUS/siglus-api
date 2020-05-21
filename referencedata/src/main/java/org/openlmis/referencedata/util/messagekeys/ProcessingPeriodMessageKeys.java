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

public abstract class ProcessingPeriodMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, PROCESSING_PERIOD);
  private static final String NAME = "name";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String PROCESSING_SCHEDULE = "processingSchedule";

  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_START_DATE_NULL = join(ERROR, START_DATE, NULL);
  public static final String ERROR_END_DATE_NULL = join(ERROR, END_DATE, NULL);
  public static final String ERROR_START_DATE_AFTER_END_DATE =
      join(ERROR, START_DATE, "after", END_DATE);
  public static final String ERROR_END_DATE_BEFORE_START_DATE =
      join(ERROR, END_DATE, "before", START_DATE);
  public static final String ERROR_GAP_BETWEEN_LAST_END_DATE_AND_START_DATE =
      join(ERROR, "gap", "between", "lastEndDate", AND, START_DATE);
  public static final String ERROR_INVALID_SORTING_COLUMN = join(ERROR, "invalidSortingColumn");
  public static final String ERROR_SCHEDULE_ID_SINGLE_PARAMETER =
      join(ERROR, PROCESSING_SCHEDULE, ID, "singleParameter");
  public static final String ERROR_NAME_NULL = join(ERROR, NAME, NULL);
  public static final String ERROR_SCHEDULE_NULL = join(ERROR, PROCESSING_SCHEDULE, NULL);
}
