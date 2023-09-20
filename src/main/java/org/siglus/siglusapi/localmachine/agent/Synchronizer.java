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

package org.siglus.siglusapi.localmachine.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.ShedLockFactory;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"localmachine"})
public class Synchronizer {

  private final SyncService syncService;
  private final Machine machine;
  private final ShedLockFactory lockFactory;
  private static final int TIMEOUT_MILLIS = 3 * 1000;

  @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000)
  public void scheduledSync() {
    log.info("start scheduled synchronization with online web");
    this.sync();
  }

  public void sync() {
    if (!machine.isActive()) {
      log.info("machine is inactive, no need to sync");
      return;
    }
    try (AutoClosableLock lock = lockFactory.waitLock("lock.synchronizer", TIMEOUT_MILLIS)) {
      if (!lock.isPresent()) {
        log.warn("[sync] fail to get lock, cancel this round");
        return;
      }
      log.info("[LM] start sync down");
      syncService.pull();
      log.info("[LM] start exchange acks");
      syncService.exchangeAcks();
      log.info("[LM] start sync up");
      syncService.push();
    } catch (InterruptedException e) {
      log.error("sync thread is interrupted", e);
    }
  }
}
