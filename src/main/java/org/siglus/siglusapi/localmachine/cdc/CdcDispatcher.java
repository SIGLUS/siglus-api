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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class CdcDispatcher {

  private final Map<String, List<CdcListener>> tableIdToListeners = new LinkedHashMap<>();
  private final CdcRecordRepository cdcRecordRepository;

  public CdcDispatcher(List<CdcListener> cdcListeners, CdcRecordRepository cdcRecordRepository) {
    this.cdcRecordRepository = cdcRecordRepository;
    cdcListeners.forEach(
        it ->
            Arrays.stream(it.acceptedTables())
                .forEach(
                    tableId -> {
                      List<CdcListener> listeners =
                          tableIdToListeners.getOrDefault(tableId, new LinkedList<>());
                      listeners.add(it);
                      tableIdToListeners.put(tableId, listeners);
                    }));
  }

  public Set<String> getTablesForCapture() {
    return tableIdToListeners.keySet();
  }

  public void doDispatch(List<CdcRecord> cdcRecords) {
    cdcRecords.stream()
        .collect(Collectors.groupingBy(CdcRecord::tableId, LinkedHashMap::new, Collectors.toList()))
        .forEach(
            (key, value) -> {
              List<CdcListener> listeners =
                  tableIdToListeners.getOrDefault(key, Collections.emptyList());
              if (listeners.isEmpty()) {
                log.warn("no listeners for table id: {}", key);
              }
              for (CdcListener cdcListener : listeners) {
                cdcListener.on(value);
              }
            });
  }

  @Transactional
  public synchronized void dispatchByTxId(Long txId) {
    // todo: get distribute lock to dispatch
    List<CdcRecord> cdcRecords = cdcRecordRepository.findCdcRecordByTxIdOrderById(txId);
    cdcRecords.stream()
            .collect(Collectors.groupingBy(CdcRecord::tableId, LinkedHashMap::new, Collectors.toList()))
            .forEach(
                (key, value) -> {
                    List<CdcListener> listeners =
                        tableIdToListeners.getOrDefault(key, Collections.emptyList());
                    if (listeners.isEmpty()) {
                      log.warn("no listeners for table id: {}", key);
                    }
                    for (CdcListener cdcListener : listeners) {
                      cdcListener.on(value);
                    }
                    cdcRecordRepository.deleteInBatch(value);
                });
  }

  @Transactional
  public void dispatchAll() {
    cdcRecordRepository.allTxIds().stream()
        .map(BigInteger::longValue)
        .forEach(this::dispatchByTxId);
  }
}
