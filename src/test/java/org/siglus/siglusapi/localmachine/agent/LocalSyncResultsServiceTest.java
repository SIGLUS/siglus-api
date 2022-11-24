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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import junit.framework.TestCase;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.siglus.siglusapi.localmachine.domain.LastSyncReplayRecord;
import org.siglus.siglusapi.localmachine.repository.ErrorRecordRepository;
import org.siglus.siglusapi.localmachine.repository.LastSyncRecordRepository;
import org.siglus.siglusapi.localmachine.webapi.LocalSyncResultsResponse;

@RunWith(MockitoJUnitRunner.class)
public class LocalSyncResultsServiceTest extends TestCase {

  @InjectMocks
  private LocalSyncResultsService localSyncResultsService;

  @Mock
  private LastSyncRecordRepository lastSyncRecordRepository;

  @Mock
  private ErrorRecordRepository errorRecordRepository;

  @Mock
  private Synchronizer synchronizer;

  private final ZonedDateTime lastSyncTime = ZonedDateTime.now();
  private final ZonedDateTime lastReplayTime = ZonedDateTime.now();
  private final LastSyncReplayRecord syncRecord = LastSyncReplayRecord.builder()
      .id(FieldConstants.LAST_SYNC_RECORD_ID)
      .lastSyncedTime(lastSyncTime)
      .lastReplayedTime(lastReplayTime)
      .build();

  @Test
  public void shouldGetSyncResult() {
    //given
    ErrorRecord errorRecord = ErrorRecord.builder().type(ErrorType.SYNC_UP).build();

    //when
    when(lastSyncRecordRepository.findFirstByOrderByLastSyncedTimeDesc()).thenReturn(syncRecord);
    when(errorRecordRepository.findLastTenErrorRecords()).thenReturn(Lists.newArrayList(errorRecord));
    doNothing().when(synchronizer).sync();
    LocalSyncResultsResponse syncResults = localSyncResultsService.doSync();

    //then
    assertEquals(lastSyncTime, syncResults.getLatestSyncedTime());
    assertEquals(1, syncResults.getErrors().size());
  }
}