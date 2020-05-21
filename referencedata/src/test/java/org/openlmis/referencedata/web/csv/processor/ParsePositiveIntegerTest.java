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
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

public class ParsePositiveIntegerTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @Mock
  private CsvContext csvContext;

  private ParsePositiveInteger parseAmount;

  @Before
  public void beforeEach() {
    parseAmount = new ParsePositiveInteger();
  }

  @Test
  public void shouldParseValidAmount() throws Exception {
    Integer result = (Integer) parseAmount.execute("1000", csvContext);
    assertEquals(Integer.valueOf(1000), result);
  }

  @Test
  public void shouldThrownExceptionWhenParameterIsNotInteger() {
    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage("'abc' could not be parsed to integer amount");

    parseAmount.execute("abc", csvContext);
  }

  @Test
  public void shouldThrownExceptionWhenParameterIsNull() {
    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage("this processor does not accept null input - "
        + "if the column is optional then chain an Optional() processor before this one");

    parseAmount.execute(null, csvContext);
  }
  
  @Test
  public void shouldThrownExceptionWhenParameterIsNegativeInteger() {
    expectedEx.expect(SuperCsvCellProcessorException.class);
    expectedEx.expectMessage("'-1000' is less than 0");

    parseAmount.execute("-1000", csvContext);
  }
}
