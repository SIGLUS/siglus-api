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
import org.openlmis.referencedata.dto.ProcessingPeriodDto;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

public class ParseProcessingPeriodTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @Mock
  private CsvContext csvContext;

  private ParseProcessingPeriod parseProcessingPeriod;

  @Before
  public void beforeEach() {
    ParseProcessingPeriod.SEPARATOR = "|";
    parseProcessingPeriod = new ParseProcessingPeriod();
  }

  @Test
  public void shouldParseValidProcessingPeriod() throws Exception {
    ProcessingPeriodDto result = (ProcessingPeriodDto) parseProcessingPeriod
        .execute("schedule|period", csvContext);
    assertEquals("period", result.getName());
    assertEquals("schedule", result.getProcessingSchedule().getCode().toString());
  }

  @Test
  public void shouldThrownExceptionWhenParameterIsNotString() {
    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage("'1' could not be parsed to Processing Period");

    parseProcessingPeriod.execute(1, csvContext);
  }

  @Test
  public void shouldThrownExceptionWhenParameterHasNoSeparator() {
    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage("'something' could not be parsed to Processing Period");

    parseProcessingPeriod.execute("something", csvContext);
  }

  @Test
  public void shouldThrownExceptionWhenParameterHasMoreThanTwoParts() {
    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage("'one|two|three' could not be parsed to Processing Period");

    parseProcessingPeriod.execute("one|two|three", csvContext);
  }
}
