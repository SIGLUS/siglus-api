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

import static java.util.stream.Collectors.toList;

import io.confluent.connect.jdbc.sink.JdbcSinkTask;
import io.debezium.relational.TableId;
import io.debezium.relational.TableSchema;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;
import lombok.SneakyThrows;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JdbcSinker {
  private final Logger logger = LoggerFactory.getLogger(JdbcSinker.class);
  private final Map<String, String> sinkConfig;
  private final DbSchemaReader schemaReader;
  private final JdbcSinkerContext context;
  private final JdbcSinkTask jdbcSinkTask;

  public JdbcSinker(ConfigBuilder configBuilder, DbSchemaReader schemaReader) {
    this.context = new JdbcSinkerContext();
    this.sinkConfig = configBuilder.sinkConfig();
    this.jdbcSinkTask = initJdbcSinkTask();
    this.schemaReader = schemaReader;
  }

  private JdbcSinkTask initJdbcSinkTask() {
    JdbcSinkTask jdbcSinkTask = new JdbcSinkTask();
    jdbcSinkTask.initialize(context);
    jdbcSinkTask.start(sinkConfig);
    return jdbcSinkTask;
  }

  public void sink(Collection<TableChangeEvent> events) {
    Collection<SinkRecord> sinkRecords = convertEvents(events);
    sinkByJdbcSinkTask(sinkRecords);
  }

  @SneakyThrows
  private void sinkByJdbcSinkTask(Collection<SinkRecord> sinkRecords) {
    try {
      jdbcSinkTask.put(sinkRecords);
    } catch (Exception e) {
      logger.error("fail to sink records, err:{}", e.getMessage());
      throw new IllegalStateException(e);
    }
  }

  private Collection<SinkRecord> convertEvents(Collection<TableChangeEvent> events) {
    return events.stream()
        .map(it -> this.convertEvent(schemaReader, it))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  List<SinkRecord> convertEvent(DbSchemaReader schemaReader, TableChangeEvent event) {
    TableId tableId = new TableId("", event.getSchemaName(), event.getTableName());
    TableSchema tableSchema = schemaReader.schemaFor(tableId);
    List<String> keyColumns =
        tableSchema.keySchema().fields().stream().map(Field::name).collect(toList());
    return event.getRowChangeEvents().stream()
        .map(
            row -> {
              List<String> columns = event.getColumns();
              List<Object> values = row.getValues();
              // key is identifier of the row
              Struct keyStruct = new Struct(tableSchema.keySchema());
              // values are value of all columns
              Struct valueStruct = new Struct(tableSchema.valueSchema());
              for (int i = 0; i < columns.size(); i++) {
                String column = columns.get(i);
                Object value = values.get(i);
                if (keyColumns.contains(column)) {
                  keyStruct.put(column, value);
                } else if (!row.isDeletion()) {
                  // for deletion record, the value may be truncated to zero and lost correct type,
                  // so don't put it in struct otherwise may cause validation failure.
                  valueStruct.put(column, value);
                }
              }
              String topic = tableId.identifier();
              Schema valueSchema = valueStruct.schema();
              if (row.isDeletion()) {
                // make the valueStruct to null as a flag of deletion
                valueStruct = null;
              }
              return new SinkRecord(
                  topic, 0, keyStruct.schema(), keyStruct, valueSchema, valueStruct, 0);
            })
        .collect(toList());
  }

  @PreDestroy
  private void stop() {
    this.jdbcSinkTask.stop();
  }
}
