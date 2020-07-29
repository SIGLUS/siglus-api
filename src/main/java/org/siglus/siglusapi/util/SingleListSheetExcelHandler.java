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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.siglus.siglusapi.dto.simam.CellMeta;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class SingleListSheetExcelHandler extends ExcelHandler {

  @Override
  public void createDataRows(Sheet tempSheet, List<Map<String, String>> dataList) {
    //parse data variable from template
    Map<String, CellMeta> tempDataColumns = parseTemplateVariables(tempSheet);

    //Write data
    for (int l = 0; l < dataList.size(); l++) {
      Map<String, String> oneData = dataList.get(l);
      Row dataRow = tempSheet.createRow((l + 1));
      for (Map.Entry<String, CellMeta> cellMetaEntry : tempDataColumns.entrySet()) {
        createRowCell(cellMetaEntry.getValue(), oneData, dataRow);
      }
    }
  }

  public Map<String, CellMeta> parseTemplateVariables(Sheet tempSheet) {

    Row variablesTemplate = tempSheet.getRow(1);
    Map<String, CellMeta> tempDataColumns = new HashMap<>();
    Iterator<Cell> variables = variablesTemplate.cellIterator();
    while (variables.hasNext()) {
      Cell rowCell = variables.next();
      CellMeta cellMeta = parseCellMeta(rowCell);
      tempDataColumns.put(cellMeta.getName(), cellMeta);
    }
    return tempDataColumns;
  }


  public Cell createRowCell(CellMeta cellMeta, Map<String, String> data, Row dataRow) {
    Cell cell = dataRow.createCell(cellMeta.getColumn());
    if (cellMeta.isVariable()) {
      String key = cellMeta.getName();
      if (key != null && key.length() > 0) {
        Object tmp = data.get(key);
        String cellData = (tmp == null ? "" : tmp.toString());
        cell.setCellValue(cellData);
      }
    } else {
      cell.setCellValue(cellMeta.getDisplay());
    }
    return cell;
  }
}