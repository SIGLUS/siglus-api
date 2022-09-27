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

import static io.debezium.data.Envelope.FieldName.AFTER;
import static io.debezium.data.Envelope.FieldName.BEFORE;
import static io.debezium.data.Envelope.FieldName.OPERATION;
import static io.debezium.data.Envelope.Operation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import io.debezium.util.Strings;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@SuppressWarnings("PMD.UnusedPrivateField")
public class CdcScraper {

  static final long DUMMY_TX_ID = -1;
  final BlockingDeque<Long> dispatchQueue = new LinkedBlockingDeque<>();
  private final Executor executor = Executors.newSingleThreadExecutor();
  private final Executor dispatchExecutor = Executors.newSingleThreadExecutor();
  private final CdcRecordRepository cdcRecordRepository;
  private final CdcDispatcher cdcDispatcher;
  private final ConfigBuilder baseConfig;
  private final AtomicLong currentTxId = new AtomicLong(DUMMY_TX_ID);
  private final PublicationPreparer publicationPreparer;
  private DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine;
  private final OffsetBackingStoreWrapper offsetBackingStoreWrapper;

  public CdcScraper(
      CdcRecordRepository cdcRecordRepository,
      CdcDispatcher cdcDispatcher,
      ConfigBuilder baseConfig,
      PublicationPreparer publicationPreparer,
      OffsetBackingStoreWrapper offsetBackingStoreWrapper) {
    this.cdcRecordRepository = cdcRecordRepository;
    this.cdcDispatcher = cdcDispatcher;
    this.baseConfig = baseConfig;
    this.offsetBackingStoreWrapper = offsetBackingStoreWrapper;
    this.publicationPreparer = publicationPreparer;
  }

  @SneakyThrows
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleChangeEvent(RecordChangeEvent<SourceRecord> sourceRecordRecordChangeEvent) {
    SourceRecord sourceRecord = sourceRecordRecordChangeEvent.record();
    log.debug("receive record:{}", sourceRecord);
    Struct sourceRecordChangeValue = (Struct) sourceRecord.value();
    if (Objects.isNull(sourceRecordChangeValue)) {
      log.warn("receive null source record value");
      return;
    }
    Operation operation = Operation.forCode((String) sourceRecordChangeValue.get(OPERATION));
    if (operation == Operation.READ) {
      return;
    }
    Map<String, Object> payload = extractPayload(sourceRecordChangeValue, operation);
    CdcRecord cdcRecord = buildCdcRecord(sourceRecord, operation, payload);
    log.debug("save cdc record: {} with operation: {}", payload, operation.name());
    cdcRecordRepository.save(cdcRecord);
    mayNeedToDispatch(cdcRecord.getTxId());
  }

  void mayNeedToDispatch(Long txId) throws InterruptedException {
    long previousTxId = currentTxId.getAndSet(txId);
    boolean previousTxEnded = previousTxId != txId;
    if (previousTxEnded) {
      dispatchQueue.put(previousTxId);
    }
  }

  private Map<String, Object> extractPayload(Struct sourceRecordChangeValue, Operation operation) {
    String record = getRecordName(operation);
    Struct struct = (Struct) sourceRecordChangeValue.get(record);
    Map<String, Object> payload = Maps.newHashMap();
    struct.schema().fields().stream()
        .map(Field::name)
        .forEach(filedName -> payload.put(filedName, struct.get(filedName)));
    return payload;
  }

  String getRecordName(Operation operation) {
    return operation == Operation.DELETE ? BEFORE : AFTER;
  }

  private CdcRecord buildCdcRecord(
      SourceRecord sourceRecord, Operation operation, Map<String, Object> payload) {
    Long lsn = ((Number) sourceRecord.sourceOffset().get("lsn")).longValue();
    Long txId = ((Number) sourceRecord.sourceOffset().get("txId")).longValue();
    String[] topic = sourceRecord.topic().split("\\.");
    String schemaName = topic[1];
    String tableName = topic[2];
    return CdcRecord.builder()
        .id(lsn)
        .txId(txId)
        .payload(payload)
        .operationCode(operation.code())
        .schema(schemaName)
        .table(tableName)
        .capturedAt(ZonedDateTime.now())
        .build();
  }

  @SneakyThrows
  io.debezium.config.Configuration config() {
    // fixme: persist the offset in db
    File offsetStorageFile = new File(".data", "offsets.dat");
    FileUtils.forceMkdirParent(offsetStorageFile);
    Set<String> subscribedTableIds = cdcDispatcher.getTablesForCapture();
    Set<String> includedTableIds = new HashSet<>(subscribedTableIds);
    // add dummy table id, in case of empty subscribedTableIds
    includedTableIds.add("----dummy-----");
    String tablesForCapture = Strings.join(",", includedTableIds);
    log.info("tables for capture:{}", tablesForCapture);
    return baseConfig
        .debeziumConfigBuilder()
        .with("name", "local-scraper")
        .with("plugin.name", "pgoutput")
        .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
        .with("offset.storage", "org.siglus.siglusapi.localmachine.cdc.OffsetBackingStore")
        .with("offset.storage.file.filename", offsetStorageFile.getAbsolutePath())
        .with("offset.flush.interval.ms", "10000")
        .with("table.include.list", tablesForCapture)
        .with("include.schema.changes", "false")
        .with("snapshot.mode", "never")
        .build();
  }

  @PostConstruct
  private void start() {
    // todo: the engine should be run with distributed lock
    // todo: exit application if debezium dead due to exception?
    Configuration config = config();
    this.publicationPreparer.prepare(config);
    this.debeziumEngine =
        DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
            .using(config.asProperties())
            .notifying(this::handleChangeEvent)
            .build();
    this.executor.execute(debeziumEngine);
    this.dispatchExecutor.execute(this::dispatching);
  }

  @PreDestroy
  private void stop() throws IOException {
    if (this.debeziumEngine != null) {
      this.debeziumEngine.close();
    }
  }

  private void dispatching() {
    while (true) {
      try {
        Long txId = dispatchQueue.poll(1, TimeUnit.MINUTES);
        doDispatch(txId);
      } catch (InterruptedException e) {
        log.warn("dispatching task interrupted, exit");
        Thread.currentThread().interrupt();
        return;
      } catch (Throwable e) {
        log.error("got error when dispatching, err:", e);
      }
    }
  }

  @VisibleForTesting
  void doDispatch(Long txId) {
    if (Objects.isNull(txId)) {
      // timeout
      cdcDispatcher.dispatchAll();
      return;
    }
    if (DUMMY_TX_ID == txId) {
      cdcDispatcher.dispatchAll();
      return;
    }
    cdcDispatcher.dispatchByTxId(txId);
  }
}
