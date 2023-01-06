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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import io.debezium.data.Envelope.FieldName;
import io.debezium.data.Envelope.Operation;
import io.debezium.engine.RecordChangeEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
public class CdcScraperTest {
  @Mock private CdcDispatcher cdcDispatcher;
  @Mock private ConfigBuilder configBuilder;
  @Mock private PublicationPreparer publicationPreparer;
  @InjectMocks private CdcScraper cdcScraper;

  @Before
  public void setup() {
    given(configBuilder.sinkConfig()).willCallRealMethod();
    doNothing().when(publicationPreparer).prepare(any());
    doNothing().when(cdcDispatcher).doDispatch(any());
    cdcScraper.dispatchQueue.clear();
    cdcScraper.dispatchBuffer.clear();
  }

  @Test
  public void shouldKeepCriticalConfigNotChanged() {
    // given
    given(configBuilder.debeziumConfigBuilder())
        .willReturn(
            new ConfigBuilder(
                    "jdbc:postgresql://localhost:5432/open_lmis?stringtype=unspecified",
                    "username",
                    "password")
                .debeziumConfigBuilder());
    List<String> criticalConfigKeys = Arrays.asList("name", "plugin.name", "connector.class", "snapshot.mode");
    // when
    Map<String, String> configSnap = cdcScraper.config().asMap().entrySet().stream()
        .filter(it -> criticalConfigKeys.contains(it.getKey())).collect(
            Collectors.toMap(Entry::getKey, Entry::getValue));
    // then
    assertThat(configSnap).containsEntry("name", "local-scraper");
    assertThat(configSnap).containsEntry("plugin.name", "pgoutput");
    assertThat(configSnap).containsEntry("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
    assertThat(configSnap).containsEntry("snapshot.mode", "never");
  }

  @Test
  public void shouldReturnBeforeWhenGetRecordNameGivenOperationIsDelete() {
    assertThat(cdcScraper.getRecordName(Operation.DELETE)).isEqualTo(FieldName.BEFORE);
  }

  @Test
  public void shouldNotPersistCdcRecordWhenHandleChangeEventGivenNullSource() {
    // given
    RecordChangeEvent<SourceRecord> changeEvent = mock(RecordChangeEvent.class);
    given(changeEvent.record()).willReturn(mock(SourceRecord.class));
    // when
    cdcScraper.handleChangeEvent(changeEvent);
    // then
    assertThat(cdcScraper.dispatchQueue.isEmpty()).isTrue();
  }

  @Test
  public void shouldNotPersistCdcRecordWhenHandleChangeEventGivenReadSource() {
    // given
    RecordChangeEvent<SourceRecord> changeEvent = getChangeEvent(Operation.READ.code());
    // when
    cdcScraper.handleChangeEvent(changeEvent);
    // then
    assertThat(cdcScraper.dispatchQueue.isEmpty()).isTrue();
  }

  @Test
  public void shouldPersistCdcRecordWhenHandleChangeEventGivenNonReadSource() {
    // given
    RecordChangeEvent<SourceRecord> changeEvent = getChangeEvent(Operation.UPDATE.code());
    // when
    cdcScraper.handleChangeEvent(changeEvent);
    // then
    CdcRecord cdcRecord = cdcScraper.dispatchQueue.peekLast();
    assertThat(cdcRecord).isNotNull();
    assertThat(cdcRecord.getPayload()).isNotEmpty();
    assertThat(cdcRecord.getPayload()).containsEntry("name", "name value");
  }

  @Test
  public void shouldNotFlushCdcRecordsGivenTxIdNotChanged() {
    // given
    long previousTxId = 1L;
    cdcScraper.dispatchBuffer.add(CdcRecord.builder().txId(previousTxId).build());
    // when
    cdcScraper.doDispatch(CdcRecord.builder().txId(previousTxId).build());
    // then
    verify(cdcDispatcher, times(0)).doDispatch(anyListOf(CdcRecord.class));
  }

  @Test
  public void shouldFlushCdcRecordsGivenTxIdChanged() {
    // given
    long previousTxId = 1L;
    cdcScraper.dispatchBuffer.add(CdcRecord.builder().txId(previousTxId).build());
    // when
    cdcScraper.doDispatch(CdcRecord.builder().txId(2L).build());
    // then
    verify(cdcDispatcher, times(1)).doDispatch(anyListOf(CdcRecord.class));
  }

  @Test
  public void shouldNotFlushCdcRecordsWhenPollTimeoutAndBufferIsEmpty() {
    // given
    cdcScraper.dispatchBuffer.clear();
    // when
    cdcScraper.doDispatch(null);
    // then
    verify(cdcDispatcher, times(0)).doDispatch(anyListOf(CdcRecord.class));
  }

  @Test
  public void shouldFlushCdcRecordsWhenPollTimeoutAndBufferIsNotEmpty() {
    // given
    long txId = 1L;
    cdcScraper.dispatchBuffer.add(CdcRecord.builder().txId(txId).build());
    // when
    cdcScraper.doDispatch(null);
    // then
    verify(cdcDispatcher, times(1)).doDispatch(anyListOf(CdcRecord.class));
  }

  private RecordChangeEvent<SourceRecord> getChangeEvent(String code) {
    Schema payloadSchema =
        SchemaBuilder.struct().field("name", SchemaBuilder.STRING_SCHEMA).build();
    Schema valueSchema =
        SchemaBuilder.struct()
            .field("op", SchemaBuilder.STRING_SCHEMA)
            .field("after", payloadSchema)
            .build();
    Struct payloadStruct = new Struct(payloadSchema).put("name", "name value");
    Struct valueStruct = new Struct(valueSchema).put("op", code).put("after", payloadStruct);
    return () ->
        new SourceRecord(
            Collections.emptyMap(),
            ImmutableMap.of("txId", 1L, "lsn", 2L),
            "server.schema.table",
            0,
            valueSchema,
            valueStruct);
  }
}
