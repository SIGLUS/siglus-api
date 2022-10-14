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

import com.google.common.annotations.VisibleForTesting;
import io.confluent.connect.jdbc.dialect.DatabaseDialect;
import io.confluent.connect.jdbc.dialect.DatabaseDialectProvider.SubprotocolBasedProvider;
import io.confluent.connect.jdbc.dialect.PostgreSqlDatabaseDialect;
import io.debezium.time.Date;
import io.debezium.time.MicroTimestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.connect.data.Schema;
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

  @VisibleForTesting
  @Override
  protected TimeZone timeZone() {
    return super.timeZone();
  }

  @Override
  protected boolean maybeBindLogical(
      PreparedStatement statement, int index, Schema schema, Object value) throws SQLException {
    if (Objects.isNull(schema)) {
      return false;
    }
    if (Date.SCHEMA_NAME.equals(schema.name())) {
      statement.setDate(
          index, java.sql.Date.valueOf(LocalDate.ofEpochDay(((Number) value).longValue())));
      return true;
    }
    if (MicroTimestamp.schema().name().equals(schema.name())) {
      statement.setTimestamp(
          index,
          Timestamp.valueOf(
              LocalDateTime.ofInstant(
                  Instant.ofEpochMilli(((Number) value).longValue() / 1000),
                  this.timeZone().toZoneId())));
      return true;
    }
    if (io.debezium.time.Timestamp.SCHEMA_NAME.equals(schema.name())) {
      statement.setTimestamp(
          index,
          Timestamp.valueOf(
              LocalDateTime.ofInstant(
                  Instant.ofEpochMilli(((Number) value).longValue()), this.timeZone().toZoneId())));
      return true;
    }
    if (io.debezium.time.ZonedTimestamp.SCHEMA_NAME.equals(schema.name())) {
      statement.setTimestamp(
          index,
          Timestamp.valueOf(
              LocalDateTime.ofInstant(ZonedDateTime.parse((String) value).toInstant(), this.timeZone().toZoneId())));
      return true;
    }
    if (Optional.ofNullable(schema.name()).orElse("").contains("io.debezium.time")) {
      throw new IllegalArgumentException("can not bind value for schema " + schema.name());
    }
    return super.maybeBindLogical(statement, index, schema, value);
  }

  void configureConnAsReplicaRole(PgConnection conn) throws SQLException {
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
