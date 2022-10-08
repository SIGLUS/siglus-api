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
