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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.siglus.siglusapi.dto.simam.CellMeta;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public abstract class ExcelHandler {

  public enum PathType {
    FILE,
    CLASSPATH
  }

  public static final String VARIABLE_PREFIX = "$";

  public Workbook readXssTemplateFile(String templateFileName, PathType type) {
    Workbook wb = null;
    try {
      InputStream templateIn = getClasspathFileInputStream(templateFileName, type);
      wb = WorkbookFactory.create(templateIn);
    } catch (Exception e) {
      log.error("Read file with exception: {}", e.getMessage());
    }
    return wb;
  }

  private InputStream getClasspathFileInputStream(String templateFileName, PathType type) {
    switch (type) {
      case FILE:
        ClassPathResource classPathResource = new ClassPathResource(
            "/static/simam/" + templateFileName);
        InputStream inputStream = null;
        try {
          inputStream = classPathResource.getInputStream();
        } catch (IOException e) {
          log.error("load template {} error", templateFileName);
        }
        return inputStream;
      case CLASSPATH:
        return ExcelHandler.class.getClassLoader().getResourceAsStream(templateFileName);
      default:
        throw new IllegalArgumentException("Path type not supported!");
    }
  }

  public abstract void createDataRows(Sheet tempSheet, List<Map<String, String>> dataList);

  public String createXssFile(Workbook wb, String fileName) {
    String filePath = "/tmp/" + fileName;
    try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
      wb.write(fileOut);
    } catch (IOException e) {
      log.error("Create file: {} with error {}", fileName, e.getMessage());
    }
    return filePath;
  }

  protected CellMeta parseCellMeta(Cell rowCell) {
    CellMeta cellMeta;
    String var = rowCell.getRichStringCellValue().getString();

    if (var != null && var.startsWith(VARIABLE_PREFIX)) {
      var = var.substring(2, var.length() - 1);
      cellMeta = new CellMeta(var, null, rowCell.getColumnIndex(), true);
    } else {
      cellMeta = new CellMeta(var, var, rowCell.getColumnIndex(), false);
    }
    return cellMeta;

  }
}