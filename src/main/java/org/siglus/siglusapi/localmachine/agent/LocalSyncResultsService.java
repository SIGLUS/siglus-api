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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.siglus.siglusapi.localmachine.domain.LastSyncReplayRecord;
import org.siglus.siglusapi.localmachine.repository.ErrorRecordRepository;
import org.siglus.siglusapi.localmachine.repository.LastSyncRecordRepository;
import org.siglus.siglusapi.localmachine.webapi.LocalSyncResultsResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalSyncResultsService {

  private final LastSyncRecordRepository lastSyncRecordRepository;

  private final ErrorRecordRepository errorRecordsRepository;

  private final Synchronizer synchronizer;

  public LocalSyncResultsResponse doSync() {
    synchronizer.sync();
    LastSyncReplayRecord lastSyncTime = lastSyncRecordRepository.findFirstByOrderByLastSyncedTimeDesc();
    List<ErrorRecord> errorRecords = errorRecordsRepository.findLastTenErrorRecords();

    return LocalSyncResultsResponse.builder()
        .latestSyncedTime(lastSyncTime.getLastSyncedTime())
        .errors(errorRecords)
        .build();
  }
}
