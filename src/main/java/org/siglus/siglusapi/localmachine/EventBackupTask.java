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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.eventstore.EventPayloadRepository;
import org.siglus.siglusapi.localmachine.eventstore.EventRecord;
import org.siglus.siglusapi.localmachine.eventstore.EventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.backup.EventPayloadBackup;
import org.siglus.siglusapi.localmachine.eventstore.backup.EventPayloadBackupRepository;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventBackupTask {
  private final AgentInfoRepository agentInfoRepository;
  private final EventRecordRepository eventRecordRepository;
  private final EventPayloadBackupRepository eventPayloadBackupRepository;
  private final EventPayloadRepository eventPayloadRepository;

  @Scheduled(cron = "${localmachine.archive.event.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "localmachine_archive_event")
  @Transactional
  public void run() {
    log.info("start archiving event consumed.");
    List<EventRecord> eventRecords = eventRecordRepository.findByArchived(false);
    if (CollectionUtils.isEmpty(eventRecords)) {
      return;
    }
    /*
     * when I am a localmachine:
     * then I can do archive when event 3 flag = true.
     * when I am a onlineweb server:
     * then I can do archive when event is locally replayed.
     */
    Set<EventRecord> archiveEventRecords;
    if (isLocalMachine()) {
      archiveEventRecords = eventRecords.stream()
          .filter(item -> item.isReceiverSynced() && item.isOnlineWebSynced() && item.isLocalReplayed())
          .collect(Collectors.toSet());
    } else {
      archiveEventRecords = eventRecords.stream().filter(EventRecord::isLocalReplayed).collect(Collectors.toSet());
    }
    if (archiveEventRecords.isEmpty()) {
      return;
    }
    List<EventPayloadBackup> backups = archiveEventRecords.stream()
        .map(item -> new EventPayloadBackup(item.getId(), item.getPayload()))
        .collect(Collectors.toList());
    eventPayloadBackupRepository.save(backups);

    List<org.siglus.siglusapi.localmachine.eventstore.EventPayload> payloads = archiveEventRecords.stream()
        .map(item -> new org.siglus.siglusapi.localmachine.eventstore.EventPayload(item.getId(), item.getPayload()))
        .collect(Collectors.toList());
    eventPayloadRepository.delete(payloads);

    archiveEventRecords.forEach(item-> item.setArchived(true));
    eventRecordRepository.save(archiveEventRecords);
  }

  private boolean isLocalMachine() {
    return agentInfoRepository.count() == 1;
  }
}
