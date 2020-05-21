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

package org.openlmis.referencedata.web.csv.processor;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.openlmis.referencedata.dto.BasicFacilityDto;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

public class FormatFacilityTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @Mock
  private CsvContext csvContext;

  private FormatFacility formatFacility;

  @Before
  public void beforeEach() {
    formatFacility = new FormatFacility();
  }

  @Test
  public void shouldFormatValidFacility() throws Exception {
    BasicFacilityDto facility = new BasicFacilityDto();
    facility.setCode("facility-code");

    String result = (String) formatFacility.execute(facility, csvContext);

    assertEquals("facility-code", result);
  }

  @Test
  public void shouldThrownExceptionWhenCodeIsNull() {
    BasicFacilityDto facility = new BasicFacilityDto();
    facility.setCode(null);

    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage(String.format("Cannot get code from '%s'.", facility.toString()));

    formatFacility.execute(facility, csvContext);
  }

  @Test
  public void shouldThrownExceptionWhenValueIsNotBasicFacilityDtoType() {
    String invalid = "invalid-type";

    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage(String.format("Cannot get code from '%s'.", invalid));

    formatFacility.execute(invalid, csvContext);
  }
}
