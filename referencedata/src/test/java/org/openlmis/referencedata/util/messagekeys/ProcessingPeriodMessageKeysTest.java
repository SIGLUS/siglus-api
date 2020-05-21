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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProcessingPeriodMessageKeysTest {

  @Test
  public void itShouldHaveValidMessageKeys() {
    assertEquals("referenceData.error.processingPeriod.gap.between.lastEndDate.and.startDate",
        ProcessingPeriodMessageKeys.ERROR_GAP_BETWEEN_LAST_END_DATE_AND_START_DATE);
    assertEquals("referenceData.error.processingPeriod.startDate.null",
        ProcessingPeriodMessageKeys.ERROR_START_DATE_NULL);
    assertEquals("referenceData.error.processingPeriod.endDate.null",
        ProcessingPeriodMessageKeys.ERROR_END_DATE_NULL);
    assertEquals("referenceData.error.processingPeriod.startDate.after.endDate",
        ProcessingPeriodMessageKeys.ERROR_START_DATE_AFTER_END_DATE);
    assertEquals("referenceData.error.processingPeriod.endDate.before.startDate",
        ProcessingPeriodMessageKeys.ERROR_END_DATE_BEFORE_START_DATE);
    assertEquals("referenceData.error.processingPeriod.invalidSortingColumn",
        ProcessingPeriodMessageKeys.ERROR_INVALID_SORTING_COLUMN);
    assertEquals("referenceData.error.processingPeriod.notFound",
        ProcessingPeriodMessageKeys.ERROR_NOT_FOUND);
    assertEquals("referenceData.error.processingPeriod.processingSchedule.id.singleParameter",
        ProcessingPeriodMessageKeys.ERROR_SCHEDULE_ID_SINGLE_PARAMETER);
  }

}
