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

import io.confluent.connect.jdbc.dialect.DatabaseDialect;
import io.confluent.connect.jdbc.dialect.DatabaseDialectProvider.SubprotocolBasedProvider;
import io.confluent.connect.jdbc.dialect.PostgreSqlDatabaseDialect;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.kafka.common.config.AbstractConfig;
import org.postgresql.jdbc.PgConnection;

public class PostgresDialect extends PostgreSqlDatabaseDialect {

  public PostgresDialect(AbstractConfig config) {
    super(config);
  }

  @Override
  public Connection getConnection() throws SQLException {
    PgConnection conn = (PgConnection) super.getConnection();
    configureConnAsReplicaRole(conn);
    return conn;
  }

  private void configureConnAsReplicaRole(PgConnection conn) throws SQLException {
    conn.execSQLUpdate("SET session_replication_role = 'replica'");
    if (!conn.getAutoCommit()) {
      conn.commit();
    }
  }

  public static class Provider extends SubprotocolBasedProvider {
    public Provider() {
      super(PostgresDialect.class.getSimpleName(), "postgresql-as-replica-role");
    }

    public DatabaseDialect create(AbstractConfig config) {
      return new PostgresDialect(config);
    }
  }
}
