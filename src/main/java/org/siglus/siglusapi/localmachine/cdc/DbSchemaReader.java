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

import io.debezium.config.Configuration;
import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.debezium.connector.postgresql.PostgresSchema;
import io.debezium.connector.postgresql.PostgresTopicSelector;
import io.debezium.connector.postgresql.PostgresValueConverter;
import io.debezium.connector.postgresql.TypeRegistry;
import io.debezium.connector.postgresql.connection.PostgresConnection;
import io.debezium.connector.postgresql.connection.PostgresConnection.PostgresValueConverterBuilder;
import io.debezium.connector.postgresql.connection.PostgresDefaultValueConverter;
import io.debezium.relational.TableId;
import io.debezium.schema.TopicSelector;
import java.nio.charset.Charset;
import lombok.SneakyThrows;

public class DbSchemaReader extends PostgresSchema {
  private DbSchemaReader(
      PostgresConnectorConfig config,
      TypeRegistry typeRegistry,
      PostgresDefaultValueConverter defaultValueConverter,
      TopicSelector<TableId> topicSelector,
      PostgresValueConverter valueConverter) {
    super(config, typeRegistry, defaultValueConverter, topicSelector, valueConverter);
  }

  public static DbSchemaReader of(Configuration config) {
    PostgresConnectorConfig connectorConfig = new PostgresConnectorConfig(config);
    final Charset databaseCharset;
    try (PostgresConnection tempConnection =
        new PostgresConnection(
            connectorConfig.getJdbcConfig(), PostgresConnection.CONNECTION_GENERAL)) {
      databaseCharset = tempConnection.getDatabaseCharset();
    }
    final PostgresValueConverterBuilder valueConverterBuilder =
        typeRegistry -> PostgresValueConverter.of(connectorConfig, databaseCharset, typeRegistry);
    PostgresConnection jdbcConnection =
        new PostgresConnection(
            connectorConfig.getJdbcConfig(),
            valueConverterBuilder,
            PostgresConnection.CONNECTION_GENERAL);
    final TypeRegistry typeRegistry = jdbcConnection.getTypeRegistry();
    final PostgresDefaultValueConverter defaultValueConverter =
        jdbcConnection.getDefaultValueConverter();
    final TopicSelector<TableId> topicSelector = PostgresTopicSelector.create(connectorConfig);
    return new DbSchemaReader(
            connectorConfig,
            typeRegistry,
            defaultValueConverter,
            topicSelector,
            valueConverterBuilder.build(typeRegistry))
        .refresh(jdbcConnection, false);
  }

  @SneakyThrows
  @Override
  protected DbSchemaReader refresh(PostgresConnection connection, boolean printReplicaIdentityInfo) {
    super.refresh(connection, printReplicaIdentityInfo);
    return this;
  }
}
