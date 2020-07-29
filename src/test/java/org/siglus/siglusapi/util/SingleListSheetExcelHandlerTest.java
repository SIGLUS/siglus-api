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

package org.siglus.siglusapi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.simam.CellMeta;

@RunWith(MockitoJUnitRunner.class)
public class SingleListSheetExcelHandlerTest {

  private SingleListSheetExcelHandler util;
  private Workbook tempWb;
  private Sheet tempSheet;

  private static final String EXCEL_FACILITY = "facilityName";
  private static final String EXCEL_PRODUCT_CODE = "TEXT";
  private static final String EXCEL_BOP = "beginningBalance";
  private static final String EXCEL_QUANTIY_DISPENSED = "quantityDispensed";

  @Before
  public void setUp() {
    util = new SingleListSheetExcelHandler();
    tempWb = util.readXssTemplateFile("template_test.xlsx", ExcelHandler.PathType.CLASSPATH);
    tempSheet = tempWb.getSheetAt(0);
  }

  @Test
  public void shouldReturnCellRowGivenData() {
    CellMeta cellMeta = new CellMeta(EXCEL_FACILITY, "test", 0, true);
    Map<String, String> data = new HashMap<>();
    data.put(EXCEL_FACILITY,  "facility1");

    Row dataRow = tempSheet.createRow(1);
    Cell rowCell = util.createRowCell(cellMeta, data, dataRow);

    assertThat(rowCell.getStringCellValue(), is("facility1"));
  }

  @Test
  public void shouldReturnCellDisplayGivenData() {
    CellMeta cellMeta = new CellMeta(EXCEL_FACILITY, "facility2", 0, false);
    Map<String, String> data = new HashMap<>();
    data.put(EXCEL_FACILITY,  "facility1");

    Row dataRow = tempSheet.createRow(1);
    Cell rowCell = util.createRowCell(cellMeta, data, dataRow);

    assertThat(rowCell.getStringCellValue(), is("facility2"));
  }

  @Test
  public void shouldReturnKeyCellMetaMapGivenTempSheet() {
    Map<String, CellMeta> cells = util.parseTemplateVariables(tempSheet);
    assertThat(cells.get(EXCEL_FACILITY).getName(), is("facilityName"));
    assertThat(cells.get(EXCEL_FACILITY).getColumn(), is(0));
    assertThat(cells.get(EXCEL_FACILITY).isVariable(), is(true));
    assertThat(cells.get(EXCEL_PRODUCT_CODE).getDisplay(), is(EXCEL_PRODUCT_CODE));
    assertThat(cells.get(EXCEL_PRODUCT_CODE).isVariable(), is(false));
  }

  @Test
  public void shouldSetCellsGivenDataList() {
    Map<String, String> row1 = new HashMap<>();
    row1.put(EXCEL_BOP, "balanceX");
    row1.put(EXCEL_QUANTIY_DISPENSED, "321");
    Map<String, String> row2 = new HashMap<>();
    row2.put(EXCEL_BOP, "balanceY");
    row2.put(EXCEL_QUANTIY_DISPENSED, "123");

    List<Map<String, String>> dataList = new ArrayList<>();
    dataList.add(row1);
    dataList.add(row2);

    util.createDataRows(tempSheet, dataList);

    assertThat(tempSheet.getRow(0).getCell(3).getStringCellValue(), is("BOP"));
    assertThat(tempSheet.getRow(1).getCell(0).getStringCellValue(), is(""));
    assertThat(tempSheet.getRow(1).getCell(3).getStringCellValue(), is("balanceX"));
    assertThat(tempSheet.getRow(1).getCell(4).getStringCellValue(), is("321"));
    assertThat(tempSheet.getRow(1).getCell(2).getStringCellValue(), is(EXCEL_PRODUCT_CODE));
    assertThat(tempSheet.getRow(2).getCell(3).getStringCellValue(), is("balanceY"));
    assertThat(tempSheet.getRow(2).getCell(4).getStringCellValue(), is("123"));
    assertThat(tempSheet.getRow(2).getCell(2).getStringCellValue(), is(EXCEL_PRODUCT_CODE));
  }
}