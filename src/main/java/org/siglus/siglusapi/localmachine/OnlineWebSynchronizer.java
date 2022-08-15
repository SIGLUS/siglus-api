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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnlineWebSynchronizer implements Runnable {
  private final EventQueue masterEventQueue;
  private final LocalMachine localMachine;
  private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
  private final OnlineWebClient webClient;

  @PostConstruct
  public void start() {
    executorService.scheduleAtFixedRate(this, 5 * 60, 30, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    executorService.shutdown();
  }

  @Override
  public void run() {
    if (succeedToConnectWeb()) {
      this.exchange();
    }
  }

  private void exchange() {
    if (localMachine.isWebServer()) {
      return;
    }
    syncUp();
    pull();
  }

  private void pull() {
    try {
      List<Event> events = webClient.fetchEvents(localMachine.getKnownFacilityIds());
      List<Acknowledge> acks = webClient.fetchAcks(localMachine.getKnownFacilityIds());
      // FIXME: 2022/8/14 relay events, save acks
    } catch (Throwable e) {
      log.error("got err when pulling", e);
    }
  }

  private void syncUp() {
    try {
      List<Event> events = masterEventQueue.getForOnlineWeb();
      Acknowledge acknowledge = webClient.sync(events);
      // FIXME: 2022/8/14 save ack
    } catch (Throwable e) {
      log.error("got err when sync up", e);
    }
  }

  private boolean succeedToConnectWeb() {
    return webClient.sendHeartbeat();
  }
}
