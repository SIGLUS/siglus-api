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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.localmachine.domain.LastSyncReplayRecord;
import org.siglus.siglusapi.localmachine.repository.LastSyncRecordRepository;

@RunWith(MockitoJUnitRunner.class)
public class SyncRecordServiceTest extends TestCase {

  @InjectMocks
  private SyncRecordService syncRecordService;

  @Mock
  private LastSyncRecordRepository lastSyncRecordRepository;

  private final ZonedDateTime lastSyncTime = ZonedDateTime.now();
  private final ZonedDateTime lastReplayTime = ZonedDateTime.now();
  private final LastSyncReplayRecord syncRecord = LastSyncReplayRecord.builder()
      .id(FieldConstants.LAST_SYNC_RECORD_ID)
      .lastSyncedTime(lastSyncTime)
      .lastReplayedTime(lastReplayTime)
      .build();

  @Test
  public void shouldStoreLastSyncRecord() {
    //when
    when(lastSyncRecordRepository.findById(FieldConstants.LAST_SYNC_RECORD_ID)).thenReturn(Optional.of(syncRecord));
    syncRecordService.storeLastSyncRecord();

    //then
    verify(lastSyncRecordRepository).save(any(LastSyncReplayRecord.class));
  }

  @Test
  public void shouldStoreLastReplayRecord() {
    //when
    when(lastSyncRecordRepository.findById(FieldConstants.LAST_SYNC_RECORD_ID)).thenReturn(Optional.of(syncRecord));
    syncRecordService.storeLastReplayRecord();

    //then
    verify(lastSyncRecordRepository).save(any(LastSyncReplayRecord.class));
  }
}