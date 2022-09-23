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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import io.debezium.data.Envelope.FieldName;
import io.debezium.data.Envelope.Operation;
import io.debezium.engine.RecordChangeEvent;
import java.util.Collections;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CdcScraperTest {
  @Mock private CdcRecordRepository cdcRecordRepository;
  @Mock private CdcDispatcher cdcDispatcher;
  @Mock private ConfigBuilder configBuilder;
  @Mock private DebeziumWrapper debeziumWrapper;
  @InjectMocks private CdcScraper cdcScraper;

  @Before
  public void setup() {
    given(configBuilder.sinkConfig()).willCallRealMethod();
    doNothing().when(debeziumWrapper).cleanSlot();
  }

  @Test
  public void shouldReturnBeforeWhenGetRecordNameGivenOperationIsDelete() {
    assertThat(cdcScraper.getRecordName(Operation.DELETE)).isEqualTo(FieldName.BEFORE);
  }

  @Test
  public void shouldNotPutPreviousTxIdWhenCheckIfNeedToDispatchGivenCurrentTxIdSameAsPrevious()
      throws InterruptedException {
    // given
    cdcScraper.dispatchQueue.clear();
    // when
    cdcScraper.mayNeedToDispatch(CdcScraper.DUMMY_TX_ID);
    // then
    assertThat(cdcScraper.dispatchQueue.isEmpty()).isTrue();
  }

  @Test
  public void shouldDispatchTargetTxIdWhenDoDispatchGivenActualTxId() {
    // given
    long txId = 1L;
    // when
    cdcScraper.doDispatch(txId);
    // then
    verify(cdcDispatcher, times(1)).dispatchByTxId(txId);
  }

  @Test
  public void shouldDispatchAllWhenDoDispatchGivenDummyTxId() {
    // when
    cdcScraper.doDispatch(CdcScraper.DUMMY_TX_ID);
    // then
    verify(cdcDispatcher, times(1)).dispatchAll();
  }

  @Test
  public void shouldDispatchAllWhenDoDispatchGivenTxIdIsNull() {
    // when
    cdcScraper.doDispatch(null);
    // then
    verify(cdcDispatcher, times(1)).dispatchAll();
  }

  @Test
  public void shouldNotPersistCdcRecordWhenHandleChangeEventGivenNullSource() {
    // given
    RecordChangeEvent<SourceRecord> changeEvent = mock(RecordChangeEvent.class);
    given(changeEvent.record()).willReturn(mock(SourceRecord.class));
    // when
    cdcScraper.handleChangeEvent(changeEvent);
    // then
    verify(cdcRecordRepository, times(0)).save(any(CdcRecord.class));
  }

  @Test
  public void shouldNotPersistCdcRecordWhenHandleChangeEventGivenReadSource() {
    // given
    RecordChangeEvent<SourceRecord> changeEvent = getChangeEvent(Operation.READ.code());
    // when
    cdcScraper.handleChangeEvent(changeEvent);
    // then
    verify(cdcRecordRepository, times(0)).save(any(CdcRecord.class));
  }

  @Test
  public void shouldPersistCdcRecordWhenHandleChangeEventGivenNonReadSource() {
    // given
    ArgumentCaptor<CdcRecord> cdcRecordCaptor = ArgumentCaptor.forClass(CdcRecord.class);
    RecordChangeEvent<SourceRecord> changeEvent = getChangeEvent(Operation.UPDATE.code());
    // when
    cdcScraper.handleChangeEvent(changeEvent);
    verify(cdcRecordRepository).save(cdcRecordCaptor.capture());
    // then
    assertThat(cdcRecordCaptor.getValue().getPayload().get("name")).isEqualTo("name value");
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
            ImmutableMap.of("txId", 1L),
            "server.schema.table",
            0,
            valueSchema,
            valueStruct);
  }
}
