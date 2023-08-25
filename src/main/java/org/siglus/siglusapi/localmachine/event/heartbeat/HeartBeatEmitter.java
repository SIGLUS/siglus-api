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

package org.siglus.siglusapi.localmachine.event.heartbeat;

import java.time.LocalDateTime;
import java.util.List;
import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.cdc.CdcListener;
import org.siglus.siglusapi.localmachine.cdc.CdcOffsetBackingRepository;
import org.siglus.siglusapi.localmachine.cdc.CdcRecord;
import org.siglus.siglusapi.localmachine.domain.CdcHeartBeat;
import org.siglus.siglusapi.repository.CdcHeartBeatRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeartBeatEmitter implements CdcListener {
  private static final int MAX_MINUTES_BEHIND_LAST_HEART_BEAT = 3;
  private static final int MAX_MINUTES_BEHIND_STARTED = 10;
  private LocalDateTime lastHeartBeatTime;
  private LocalDateTime startTime;

  private final CdcHeartBeatRepository cdcHeartBeatRepository;
  private final CdcOffsetBackingRepository cdcOffsetBackingRepository;
  private final ApplicationContext context;


  @PostConstruct
  private void init() {
    startTime = LocalDateTime.now();
  }

  @Scheduled(fixedRate = 60 * 1000)
  public void emit() {
    List<CdcHeartBeat> heartBeats = cdcHeartBeatRepository.findAll();
    CdcHeartBeat cdcHeartBeat;
    if (CollectionUtils.isEmpty(heartBeats)) {
      cdcHeartBeat = new CdcHeartBeat(LocalDateTime.now());
    } else {
      cdcHeartBeat = heartBeats.get(0);
      cdcHeartBeat.setUpdatedTime(LocalDateTime.now());
    }
    cdcHeartBeatRepository.save(cdcHeartBeat);

    if (lastHeartBeatTime != null
            && lastHeartBeatTime.plusMinutes(MAX_MINUTES_BEHIND_LAST_HEART_BEAT).isBefore(LocalDateTime.now())) {
      log.error("2 cdcHeartBeat lost, drop debezium slot and publication, exit process");
      try {
        cdcHeartBeatRepository.deleteDebeziumSlot();
      } finally {
        System.exit(SpringApplication.exit(context));
      }
    }

    if (lastHeartBeatTime == null
            && startTime.plusMinutes(MAX_MINUTES_BEHIND_STARTED).isBefore(LocalDateTime.now())) {
      log.error("debezium not working, drop cdc_offset_backing, drop debezium slot and publication, exit process");
      try {
        cdcOffsetBackingRepository.deleteAll();
        cdcHeartBeatRepository.deleteDebeziumSlot();
      } finally {
        System.exit(SpringApplication.exit(context));
      }
    }
  }

  @Override
  public String[] acceptedTables() {
    return new String[]{
        "localmachine.cdc_heart_beat"
    };
  }

  @Transactional
  @Override
  public void on(List<CdcRecord> records) {
    lastHeartBeatTime = LocalDateTime.now();
  }
}
