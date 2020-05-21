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

public class OrderableFulFillMessageKeysTest {

  @Test
  public void itShouldHaveValidMessageKeys() {
    assertEquals("referenceData.error.orderableFulFill.search.invalidParams",
        OrderableFulFillMessageKeys.ERROR_INVALID_PARAMS);
    assertEquals("referenceData.error.orderableFulFill.search.providedFacilityIdWithoutProgramId",
        OrderableFulFillMessageKeys.ERROR_PROVIDED_FACILITY_ID_WITHOUT_PROGRAM_ID);
    assertEquals("referenceData.error.orderableFulFill.search.providedProgramIdWithoutFacilityId",
        OrderableFulFillMessageKeys.ERROR_PROVIDED_PROGRAM_ID_WITHOUT_FACILITY_ID);
    assertEquals("referenceData.error.orderableFulFill.search"
            + ".idsCannotBeProvidedTogetherWithFacilityIdAndProgramId",
        OrderableFulFillMessageKeys
            .ERROR_IDS_CANNOT_BY_PROVIDED_TOGETHER_WITH_FACILITY_ID_AND_PROGRAM_ID);
  }
}
