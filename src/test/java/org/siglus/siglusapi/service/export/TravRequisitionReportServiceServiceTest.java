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

package org.siglus.siglusapi.service.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.constant.ProgramConstants;

@RunWith(MockitoJUnitRunner.class)
public class TravRequisitionReportServiceServiceTest {

  @Test
  public void shouldGetSupportProgramCodeIsTrav() {
    TarvRequisitionReportService service = new TarvRequisitionReportService();
    Set<String> result = service.supportedProgramCodes();
    assertEquals(1, result.size());
    assertTrue(result.contains(ProgramConstants.TARV_PROGRAM_CODE));
  }
}
