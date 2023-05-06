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

package org.siglus.siglusapi.localmachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.eventstore.EventRecord;
import org.siglus.siglusapi.localmachine.eventstore.EventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.backup.EventPayloadBackup;
import org.siglus.siglusapi.localmachine.eventstore.backup.EventPayloadBackupRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventBackupTask {

  private final EventRecordRepository eventRecordRepository;
  private final EventPayloadBackupRepository eventPayloadBackupRepository;
  private final EventBackupDeleteTask eventBackupDeleteTask;

  @Scheduled(cron = "${event.archive.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "localmachine_archive_event")
  public void run() {
    log.info("start archiving events ...");
    boolean hasMoreRecords = true;
    List<UUID> archiveEventIds = new ArrayList<>(Collections.emptySet());
    while (hasMoreRecords) {
      List<EventRecord> archiveEventRecords = eventRecordRepository
          .findFirst100ByArchivedFalseAndReceiverSyncedTrueAndOnlineWebSyncedTrueAndLocalReplayedTrue();
      if (CollectionUtils.isEmpty(archiveEventRecords)) {
        hasMoreRecords = false;
      } else {
        List<EventPayloadBackup> backups = archiveEventRecords.stream()
            .map(item -> new EventPayloadBackup(item.getId(), item.getPayload()))
            .collect(Collectors.toList());
        eventPayloadBackupRepository.save(backups);
        archiveEventRecords.forEach(event -> event.setArchived(true));
        archiveEventIds.addAll(archiveEventRecords.stream().map(EventRecord::getId).collect(Collectors.toSet()));
        eventRecordRepository.save(archiveEventRecords);
        log.info("archived 100 events.");
      }
    }

    log.info("delete event_payload, total: {}", archiveEventIds.size());
    if (!archiveEventIds.isEmpty()) {
      int batchSize = 100;
      List<List<UUID>> archiveEventBatches = new ArrayList<>();
      for (int i = 0; i < archiveEventIds.size(); i += batchSize) {
        archiveEventBatches.add(archiveEventIds.subList(i, Math.min(i + batchSize, archiveEventIds.size())));
      }
      for (List<UUID> batch : archiveEventBatches) {
        log.info("delete event_payload, batch size: {}", batch.size());
        eventBackupDeleteTask.delete(batch);
      }
    }

    log.info("finish archived events.");
  }

}
