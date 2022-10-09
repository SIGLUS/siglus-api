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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {

  public static void write(List<File> files, ZipOutputStream zipOutputStream) throws IOException {
    byte[] buffer = new byte[8 * 1024];
    for (File srcFile : files) {
      try (FileInputStream fileInputStream = new FileInputStream(srcFile);) {
        zipOutputStream.putNextEntry(new ZipEntry(srcFile.getName()));
        int length;
        while ((length = fileInputStream.read(buffer)) > 0) {
          zipOutputStream.write(buffer, 0, length);
        }
        zipOutputStream.closeEntry();
        if (srcFile.exists()) {
          Files.delete(srcFile.toPath());
        }
      }
    }
  }
}
