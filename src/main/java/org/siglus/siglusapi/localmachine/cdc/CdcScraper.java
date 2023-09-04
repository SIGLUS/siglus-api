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
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.siglus.siglusapi.localmachine.Machine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@SuppressWarnings("PMD.UnusedPrivateField")
public class CdcScraper {

  static final long DUMMY_TX_ID = -1;
  private static final int DISPATCH_BUFFER_CAPACITY = 10240;
  private final AtomicLong currentTxId = new AtomicLong(DUMMY_TX_ID);
  private final Machine machine;
  private final CdcRecordRepository cdcRecordRepository;
  final LinkedList<CdcRecord> dispatchBuffer = new LinkedList<>();
  final BlockingDeque<CdcRecord> dispatchQueueForOnlineWeb = new LinkedBlockingDeque<>(DISPATCH_BUFFER_CAPACITY);
  final BlockingDeque<Long> dispatchQueueForLocalMachine = new LinkedBlockingDeque<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final ExecutorService dispatchExecutor = Executors.newSingleThreadExecutor();
  private final CdcDispatcher cdcDispatcher;
  private final ConfigBuilder baseConfig;
  private final PublicationPreparer publicationPreparer;
  private DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine;
  // suppress warning - unused private field
  // the wrapper is injected here to ensure the Scraper is constructed after backing store ready
  private final OffsetBackingStoreWrapper offsetBackingStoreWrapper;

  public CdcScraper(
      Machine machine,
      CdcRecordRepository cdcRecordRepository,
      CdcDispatcher cdcDispatcher,
      ConfigBuilder baseConfig,
      PublicationPreparer publicationPreparer,
      OffsetBackingStoreWrapper offsetBackingStoreWrapper) {
    this.machine = machine;
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
    Struct sourceRecordChangeValue = (Struct) sourceRecord.value();
    if (Objects.isNull(sourceRecordChangeValue)) {
      log.info("receive null source record value");
      return;
    }
    Operation operation = Operation.forCode((String) sourceRecordChangeValue.get(OPERATION));
    if (operation == Operation.READ) {
      return;
    }
    Map<String, Object> payload = extractPayload(sourceRecordChangeValue, operation);
    CdcRecord cdcRecord = buildCdcRecord(sourceRecord, operation, payload);
    log.info("receive cdc record: {}, payload: {} operation: {}", sourceRecord, payload, operation.name());
    if (machine.isOnlineWeb()) {
      dispatchQueueForOnlineWeb.put(cdcRecord);
    } else {
      cdcRecordRepository.save(cdcRecord);
      mayNeedToDispatch(cdcRecord.getTxId());
    }
  }

  void mayNeedToDispatch(Long txId) throws InterruptedException {
    long previousTxId = currentTxId.getAndSet(txId);
    boolean previousTxEnded = previousTxId != txId;
    if (previousTxEnded) {
      dispatchQueueForLocalMachine.put(previousTxId);
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
        .with("offset.flush.interval.ms", "1000")
        .with("table.include.list", tablesForCapture)
        .with("include.schema.changes", "false")
        .with("snapshot.mode", "never")
        .build();
  }

  @PostConstruct
  private void start() {
    // todo: the engine should be run with distributed lock
    Configuration config = config();
    this.publicationPreparer.prepare(config);
    this.debeziumEngine =
        DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
            .using(config.asProperties())
            .notifying(this::handleChangeEvent)
            .build();
    this.executor.execute(debeziumEngine);
    if (machine.isOnlineWeb()) {
      this.dispatchExecutor.execute(this::dispatchingForOnlineWeb);
    } else {
      this.dispatchExecutor.execute(this::dispatchingForLocalMachine);
    }
  }

  @PreDestroy
  private void stop() throws IOException, InterruptedException {
    if (this.debeziumEngine != null) {
      this.debeziumEngine.close();
      this.executor.shutdown();
      if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
        log.error("timeout when waiting for debezium executor shutdown");
      }
    }
    this.dispatchExecutor.shutdown();
    if (!this.dispatchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
      log.error("timeout when waiting for dispatcher shutdown");
    }
  }

  private void dispatchingForOnlineWeb() {
    while (true) {
      try {
        CdcRecord currenRecord = dispatchQueueForOnlineWeb.poll(30, TimeUnit.SECONDS);
        doDispatch(currenRecord);
      } catch (InterruptedException e) {
        log.warn("dispatching task interrupted, continue to flush buff");
        doDispatch((CdcRecord) null);
        Thread.currentThread().interrupt();
        return;
      } catch (Throwable e) {
        log.error("got error when dispatching, err:", e);
      }
    }
  }

  private void dispatchingForLocalMachine() {
    while (true) {
      try {
        Long txId = dispatchQueueForLocalMachine.poll(1, TimeUnit.MINUTES);
        doDispatch(txId);
      } catch (InterruptedException e) {
        log.warn("dispatching task interrupted, continue to flush buff");
        doDispatch((Long) null);
        Thread.currentThread().interrupt();
        return;
      } catch (Throwable e) {
        log.error("got error when dispatching, err:", e);
      }
    }
  }


  @VisibleForTesting
  void doDispatch(CdcRecord current) {
    boolean timeoutOccurs = Objects.isNull(current);
    boolean buffCapacityIsFull = dispatchBuffer.size() > DISPATCH_BUFFER_CAPACITY;
    boolean buffIsNotEmpty = !dispatchBuffer.isEmpty();
    boolean isPreviousTxEnded =
        (buffIsNotEmpty && !Objects.isNull(current))
            && (!dispatchBuffer.getLast().getTxId().equals(current.getTxId()));
    boolean needToFlush = buffIsNotEmpty && (timeoutOccurs || buffCapacityIsFull || isPreviousTxEnded);
    if (needToFlush) {
      log.info("flush cdc records: {}", dispatchBuffer.size());
      CdcRecord[] records = dispatchBuffer.toArray(new CdcRecord[0]);
      cdcDispatcher.doDispatch(Arrays.asList(records));
      dispatchBuffer.clear();
    }
    if (Objects.isNull(current)) {
      return;
    }
    dispatchBuffer.addLast(current);
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
