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

package org.siglus.siglusapi.localmachine.scheduledtask;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecord;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecordRepository;
import org.siglus.siglusapi.localmachine.server.OnlineWebService;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class WebOnlineTaskTest {

  @InjectMocks
  private OnlineWebTask task;

  @Mock
  private OnlineWebService onlineWebService;

  @Mock
  private MasterDataEventRecordRepository repository;

  @Before
  public void setup() {
    ReflectionTestUtils.setField(task, "masterDataChangesCount", 500);
    ReflectionTestUtils.setField(task, "masterDataNotUpdateInterval", 1);
  }

  @Test
  public void shouldSuccessWhenScheduledTaskExecute() {
    // given
    when(repository.findTopBySnapshotVersionIsNotNullOrderByIdDesc()).thenReturn(null);

    // when
    task.generateMasterDataZip();

    // then
    verify(onlineWebService).generateMasterData();
  }


  @Test
  public void shouldExecuteTaskWhenTimeMoreThan1Month() {
    // given
    when(repository.findTopBySnapshotVersionIsNotNullOrderByIdDesc()).thenReturn(
        MasterDataEventRecord.builder()
            .id(1L)
            .occurredTime(ZonedDateTime.now().minusMonths(2))
            .build()
    );
    // when
    task.generateMasterDataZip();
    // then
    verify(onlineWebService).generateMasterData();
  }

  @Test
  public void shouldExecuteTaskWhenChangesMoreThanMax() {
    // given
    // given
    when(repository.findTopBySnapshotVersionIsNotNullOrderByIdDesc()).thenReturn(
        MasterDataEventRecord.builder()
            .id(1L)
            .occurredTime(ZonedDateTime.now())
            .build()
    );
    when(repository.findChangesCountAfterLatestSnapshotVersion(1L)).thenReturn(1000);
    // when
    task.generateMasterDataZip();
    // then
    verify(onlineWebService).generateMasterData();
  }

}