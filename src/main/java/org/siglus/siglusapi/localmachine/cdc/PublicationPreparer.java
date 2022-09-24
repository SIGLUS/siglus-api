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

package org.siglus.siglusapi.localmachine.cdc;

import static io.debezium.connector.postgresql.connection.ReplicationConnection.Builder.DEFAULT_PUBLICATION_NAME;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.subtract;

import io.debezium.config.Configuration;
import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.debezium.connector.postgresql.connection.PostgresConnection;
import io.debezium.relational.RelationalTableFilters;
import io.debezium.relational.TableId;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PublicationPreparer {

  private static final String ALTER_PUBLICATION_ADD_TABLE = "ALTER PUBLICATION %s ADD TABLE %s";
  private static final String ALTER_PUBLICATION_DROP_TABLE = "ALTER PUBLICATION %s DROP TABLE %s";
  private static final String CREATE_PUBLICATION = "CREATE PUBLICATION %s FOR TABLE %s";

  @SneakyThrows
  public void prepare(Configuration config) {
    alignPublication(config);
  }

  protected void alignPublication(Configuration config) throws SQLException {
    PostgresConnectorConfig connectorConfig = new PostgresConnectorConfig(config);
    RelationalTableFilters tableFilter = connectorConfig.getTableFilters();
    try (PostgresConnection conn = getConn(connectorConfig)) {
      conn.setAutoCommit(false);
      Set<TableId> capturedTableIds = getCapturedTableIds(tableFilter, conn);
      Set<TableId> existingPublicationTables = getExistingPublicationTables(conn);
      Collection<TableId> toAdd = subtract(capturedTableIds, existingPublicationTables);
      String sqlToUpdatePublication = getSqlToUpdatePublication(existingPublicationTables);
      updatePublication(toAdd, sqlToUpdatePublication, conn);
      Collection<TableId> toRemove = subtract(existingPublicationTables, capturedTableIds);
      updatePublication(toRemove, ALTER_PUBLICATION_DROP_TABLE, conn);
      conn.commit();
      conn.setAutoCommit(true);
    }
  }

  protected PostgresConnection getConn(PostgresConnectorConfig connectorConfig) {
    return new PostgresConnection(
        connectorConfig.getJdbcConfig(), PostgresConnection.CONNECTION_GENERAL);
  }

  private String getSqlToUpdatePublication(Set<TableId> existingPublicationTables) {
    return existingPublicationTables.isEmpty() ? CREATE_PUBLICATION : ALTER_PUBLICATION_ADD_TABLE;
  }

  private Set<TableId> getCapturedTableIds(
      RelationalTableFilters tableFilter, PostgresConnection conn) throws SQLException {
    return conn.readAllTableNames(null).stream()
        .filter(it -> tableFilter.dataCollectionFilter().isIncluded(it))
        .collect(Collectors.toSet());
  }

  private Set<TableId> getExistingPublicationTables(PostgresConnection conn) throws SQLException {
    return conn.queryAndMap(
        format(
            "SELECT schemaname, tablename FROM pg_publication_tables WHERE pubname='%s'",
            DEFAULT_PUBLICATION_NAME),
        rs -> {
          Set<TableId> tableIds = new HashSet<>();
          while (rs.next()) {
            String schema = rs.getString(1);
            String table = rs.getString(2);
            tableIds.add(new TableId("", schema, table));
          }
          return tableIds;
        });
  }

  private void updatePublication(
      Collection<TableId> targetTables, String sql, PostgresConnection conn) throws SQLException {
    if (targetTables.isEmpty()) {
      return;
    }
    String tables =
        targetTables.stream().map(TableId::toDoubleQuotedString).collect(Collectors.joining(", "));
    String createPublicationStmt = format(sql, DEFAULT_PUBLICATION_NAME, tables);
    log.info(
        "operate publication '{}' with statement '{}'",
        DEFAULT_PUBLICATION_NAME,
        createPublicationStmt);
    conn.execute(createPublicationStmt);
  }
}
