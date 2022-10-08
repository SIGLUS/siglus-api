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

package org.siglus.siglusapi.localmachine.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.localmachine.exception.DbOperationException;
import org.siglus.siglusapi.localmachine.repository.MasterDataSql;
import org.siglus.siglusapi.localmachine.repository.MovementSql;
import org.siglus.siglusapi.localmachine.repository.RequisitionOrderSql;
import org.siglus.siglusapi.localmachine.repository.TableCopyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineWebService {

  private final TableCopyRepository tableCopyRepository;
  private final Map<String, String> tableNameToMasterSql = MasterDataSql.getMasterDataSqlMap();
  private final Map<String, String> tableNameToMovementSql = MovementSql.getMovementSql();
  private final Map<String, String> tableNameToRequisitionOrderSql = RequisitionOrderSql.getRequisitionOrderSql();
  private static final String ZIP_SUFFIX = ".zip";
  private static final String CONTENT_TYPE = "application/zip";
  private static final String DISPOSITION_BASE = "attachment; filename=";
  @Value("${resync.zip.export.path}")
  private String zipExportPath;

  public void reSyncData(UUID homeFacilityId, HttpServletResponse response) {
    String facilityDir = homeFacilityId + "_" + System.currentTimeMillis();
    String zipDirectory = zipExportPath + facilityDir + "/";
    String zipName = homeFacilityId + "_" + System.currentTimeMillis() + ZIP_SUFFIX;
    response.setContentType(CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_BASE + zipName);
    File directory = new File(zipDirectory);
    boolean mkdirs = directory.mkdirs();
    if (!mkdirs) {
      log.warn("facility {} resync zip dir make fail", homeFacilityId);
      return;
    }
    File zipFile = generateZipFile(zipDirectory, zipName, homeFacilityId);
    try (FileInputStream fileInputStream = new FileInputStream(zipFile);) {
      IOUtils.copy(fileInputStream, response.getOutputStream());
      response.flushBuffer();
      FileUtils.deleteDirectory(directory);
    } catch (IOException e) {
      log.error("facilityId {} delete directory fail,{}", homeFacilityId, e);
    }
  }

  private File generateZipFile(String zipDirectory, String zipName, UUID homeFacilityId) {
    File zipFile = new File(zipDirectory + zipName);
    List<File> tableFiles = Stream
        .of(tableCopyRepository.copyDateToFile(zipDirectory, tableNameToMasterSql, homeFacilityId),
            tableCopyRepository.copyDateToFile(zipDirectory, tableNameToMovementSql, homeFacilityId),
            tableCopyRepository.copyDateToFile(zipDirectory, tableNameToRequisitionOrderSql, homeFacilityId))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);) {
      byte[] buffer = new byte[8 * 1024];
      for (File srcFile : tableFiles) {
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
    } catch (Exception e) {
      log.error("facilityId {} generate zip file fail,{}", homeFacilityId, e);
      throw new DbOperationException(e, new Message(homeFacilityId + " generate zip file fail"));
    }
    return zipFile;
  }

}
