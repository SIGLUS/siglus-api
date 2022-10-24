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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import io.debezium.relational.TableId;
import io.debezium.relational.TableSchema;
import java.util.Arrays;
import java.util.List;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent.RowChangeEvent;

@RunWith(MockitoJUnitRunner.class)
public class JdbcSinkerTest {
  private final JdbcSinker sinker = mock(JdbcSinker.class);
  @Mock private DbSchemaReader schemaReader;

  @Before
  public void setup() {
    given(sinker.convertEvent(any(), any())).willCallRealMethod();
  }

  @Test
  public void shouldExtractSinkRecordsWhen() {
    // given
    TableId tableId = new TableId("", "schema", "table");
    Schema keySchema = SchemaBuilder.struct().field("id", SchemaBuilder.STRING_SCHEMA).build();
    Schema valueSchema = SchemaBuilder.struct()
        .field("col1", SchemaBuilder.STRING_SCHEMA)
        .field("id", SchemaBuilder.STRING_SCHEMA).build();

    given(schemaReader.schemaFor(tableId))
        .willReturn(new TableSchema(tableId, keySchema, null, null, valueSchema, null));
    RowChangeEvent nonDeletionRow = new RowChangeEvent(false, Arrays.asList("id1", "col1-value"));
    RowChangeEvent deletionRow = new RowChangeEvent(true, Arrays.asList("id2", "col1-value"));
    TableChangeEvent event =
        TableChangeEvent.builder()
            .tableName("table")
            .schemaName("schema")
            .schemaVersion("version")
            .columns(Arrays.asList("id", "col1"))
            .rowChangeEvents(Arrays.asList(nonDeletionRow, deletionRow))
            .build();
    // when
    List<SinkRecord> sinkRecords = sinker.convertEvent(schemaReader, event);
    // then
    assertThat(sinkRecords.size()).isEqualTo(2);
    assertThat(sinkRecords.get(0).value()).isNotNull();
    assertThat(sinkRecords.get(1).value()).isNull();
  }
}
