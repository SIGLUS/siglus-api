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

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.domain.LatestSyncReplayRecord;
import org.siglus.siglusapi.localmachine.domain.ReplayErrorRecords;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.localmachine.repository.LatestSyncReplayRecordRepository;
import org.siglus.siglusapi.localmachine.repository.ReplayErrorRecordsRepository;
import org.siglus.siglusapi.localmachine.webapi.LocalSyncResultsResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalSyncResultsService {

  private final LatestSyncReplayRecordRepository latestSyncReplayRecordRepository;

  private final ReplayErrorRecordsRepository errorRecordsRepository;

  private final PayloadSerializer serializer;

  public LocalSyncResultsResponse getSyncResults() {
    LatestSyncReplayRecord firstByLatestSyncedTimeDesc = latestSyncReplayRecordRepository
        .findFirstByOrderByLatestSyncedTimeDesc();

    List<ReplayErrorRecords> errorRecords = errorRecordsRepository
        .findTopTenWithCreationDateTimeAfter(firstByLatestSyncedTimeDesc.getLatestReplayedTime());
    List<Object> errors = errorRecords.stream().map(ReplayErrorRecords::getErrors).map(serializer::load)
        .collect(Collectors.toList());

    return LocalSyncResultsResponse.builder().latestSyncedTime(firstByLatestSyncedTimeDesc.getLatestSyncedTime())
        .errors(errors)
        .build();
  }
}
