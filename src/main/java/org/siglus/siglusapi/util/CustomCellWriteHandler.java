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

import static org.siglus.siglusapi.constant.FieldConstants.BASIC_COLUMN;
import static org.siglus.siglusapi.constant.FieldConstants.BASIC_ROW;
import static org.siglus.siglusapi.constant.FieldConstants.GREEN_MARK;
import static org.siglus.siglusapi.constant.FieldConstants.PURPLE_MARK;
import static org.siglus.siglusapi.constant.FieldConstants.RED_MARK;
import static org.siglus.siglusapi.constant.FieldConstants.YELLOW_MARK;

import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;


@AllArgsConstructor
public class CustomCellWriteHandler implements CellWriteHandler {

  private int[][] colorArrays;
  private boolean paintForLegenda;

  @Override
  public void afterCellDispose(CellWriteHandlerContext context) {
    WriteCellData<?> cellData = context.getFirstCellData();
    Cell cell = context.getCell();
    WriteCellStyle writeCellStyle = cellData.getOrCreateStyle();
    int columnIndex = cell.getColumnIndex();
    int rowIndex = cell.getRowIndex();
    rowIndex = rowIndex - BASIC_ROW;
    if (!paintForLegenda) {
      columnIndex = columnIndex - BASIC_COLUMN;
    }
    if (rowIndex < 0 || columnIndex < 0 || colorArrays[rowIndex][columnIndex] == 0) {
      return;
    }

    if (colorArrays[rowIndex][columnIndex] == RED_MARK) {
      writeCellStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
    } else if (colorArrays[rowIndex][columnIndex] == YELLOW_MARK) {
      writeCellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    } else if (colorArrays[rowIndex][columnIndex] == GREEN_MARK) {
      writeCellStyle.setFillForegroundColor(IndexedColors.LIME.getIndex());
    } else if (colorArrays[rowIndex][columnIndex] == PURPLE_MARK) {
      writeCellStyle.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
    }
    writeCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
  }
}

