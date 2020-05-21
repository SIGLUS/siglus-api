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

public class SupervisoryNodeMessageKeysTest {

  @Test
  public void itShouldHaveValidMessageKeys() {
    assertEquals("referenceData.error.supervisoryNode.null",
        SupervisoryNodeMessageKeys.ERROR_NULL);
    assertEquals("referenceData.error.supervisoryNode.notFound",
        SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    assertEquals("referenceData.error.supervisoryNode.notFound.with.id",
        SupervisoryNodeMessageKeys.ERROR_NOT_FOUND_WITH_ID);
    assertEquals("referenceData.error.supervisoryNode.code.required",
        SupervisoryNodeMessageKeys.ERROR_CODE_REQUIRED);
    assertEquals("referenceData.error.supervisoryNode.code.mustBeUnique",
        SupervisoryNodeMessageKeys.ERROR_CODE_MUST_BE_UNIQUE);
    assertEquals("referenceData.error.supervisoryNode.name.required",
        SupervisoryNodeMessageKeys.ERROR_NAME_REQUIRED);
    assertEquals("referenceData.error.supervisoryNode.search.invalidParams",
        SupervisoryNodeMessageKeys.ERROR_INVALID_PARAMS);
  }
}
