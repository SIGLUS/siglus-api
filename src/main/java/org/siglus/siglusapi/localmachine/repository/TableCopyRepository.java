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

package org.siglus.siglusapi.localmachine.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.DbOperationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TableCopyRepository {

  private final JdbcTemplate jdbc;

  public List<File> copyDateToFile(String zipDirectory, Map<String, String> tableNameToSql, UUID homeFacilityId) {
    List<File> tableFiles;
    try (BaseConnection connection = (BaseConnection) jdbc.getDataSource().getConnection().getMetaData()
        .getConnection();) {
      connection.setAutoCommit(false);
      tableFiles = handleTableNameToSqlMap(zipDirectory, tableNameToSql, homeFacilityId, connection);
      connection.commit();
    } catch (SQLException e) {
      log.error("facilityId {} copy business data table to file fail,{}", homeFacilityId, e);
      throw new DbOperationException(e, new Message("copy business data table to file fail"));
    }
    return tableFiles;
  }

  public List<File> copyMasterDateToFile(String zipDirectory, Map<String, String> tableNameToSql, UUID homeFacilityId) {
    List<File> tableFiles;
    String lockTablesSql = buildLockTablesSql(tableNameToSql);
    try (BaseConnection connection = (BaseConnection) jdbc.getDataSource().getConnection().getMetaData()
        .getConnection();
        PreparedStatement psLockTables = connection.prepareStatement(lockTablesSql);) {
      connection.setAutoCommit(false);
      psLockTables.execute();
      tableFiles = handleTableNameToSqlMap(zipDirectory, tableNameToSql, homeFacilityId, connection);
      connection.commit();
    } catch (SQLException e) {
      log.error("facilityId {} copy master data table to file fail,{}", homeFacilityId, e);
      throw new DbOperationException(e, new Message("copy master data table to file fail"));
    }
    return tableFiles;
  }

  private String buildLockTablesSql(Map<String, String> tableNameToSql) {
    StringBuilder lockTablesSql = new StringBuilder("LOCK TABLE ");
    Set<String> tableNameKeys = tableNameToSql.keySet();
    for (int i = 0; i < tableNameKeys.size(); i++) {
      lockTablesSql.append(tableNameKeys.toArray()[i]);
      if (i != tableNameKeys.size() - 1) {
        lockTablesSql.append(',');
      }
    }
    lockTablesSql.append(" IN SHARE MODE;");
    return lockTablesSql.toString();
  }

  private List<File> handleTableNameToSqlMap(String zipDirectory, Map<String, String> tableNameToSql,
      UUID homeFacilityId, BaseConnection connection) {
    List<File> tableFiles = new ArrayList<>();
    tableNameToSql.forEach((tableName, selectSql) -> {
      File file = new File(zipDirectory + tableName + ".txt");
      if (homeFacilityId != null) {
        selectSql = selectSql.replace("@@", homeFacilityId.toString());
      }
      copyToFile(file, selectSql, connection);
      tableFiles.add(file);
    });
    return tableFiles;
  }

  private void copyToFile(File file, String querySql, BaseConnection connection) {
    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
      CopyManager copyManager = new CopyManager(connection);
      copyManager.copyOut("COPY (" + querySql + ") TO STDOUT", fileOutputStream);
    } catch (SQLException | IOException e) {
      log.error("querySql {} copy table data to file fail,{}", querySql, e);
      throw new DbOperationException(e, new Message("copy table data to file fail"));
    }
  }
}
