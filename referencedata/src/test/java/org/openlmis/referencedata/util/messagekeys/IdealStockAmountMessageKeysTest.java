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

public class IdealStockAmountMessageKeysTest {

  @Test
  public void itShouldHaveValidMessageKeys() {
    assertEquals("referenceData.error.idealStockAmount.format.notAllowed",
        IdealStockAmountMessageKeys.ERROR_FORMAT_NOT_ALLOWED);
    assertEquals("referenceData.error.idealStockAmount.field.required",
        IdealStockAmountMessageKeys.ERROR_FROM_FIELD_REQUIRED);
    assertEquals("referenceData.error.idealStockAmount.facility.notFound",
        IdealStockAmountMessageKeys.ERROR_FACILITY_NOT_FOUND);
    assertEquals("referenceData.error.idealStockAmount.processingPeriod.notFound",
        IdealStockAmountMessageKeys.ERROR_PROCESSING_PERIOD_NOT_FOUND);
    assertEquals("referenceData.error.idealStockAmount.commodityType.notFound",
        IdealStockAmountMessageKeys.ERROR_COMMODITY_TYPE_NOT_FOUND);
  }
}
