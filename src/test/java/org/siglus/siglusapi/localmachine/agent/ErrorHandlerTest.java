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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_FOUND_SYNC_RECORD;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.siglus.siglusapi.localmachine.repository.ErrorRecordRepository;

@RunWith(MockitoJUnitRunner.class)
public class ErrorHandlerTest extends TestCase {

  @InjectMocks
  private ErrorHandler errorHandler;

  @Mock
  private ErrorRecordRepository errorRecordRepository;

  @Mock
  private SyncRecordService syncRecordService;

  private final UUID eventId = UUID.randomUUID();

  @Test
  public void shouldStoreBusinessSyncErrorWithoutEvent() {
    //given
    BusinessDataException businessDataException = new BusinessDataException(new Message(ERROR_NOT_FOUND_SYNC_RECORD));

    //when
    doNothing().when(syncRecordService).storeLastSyncRecord();
    errorHandler.storeErrorRecord(businessDataException, ErrorType.SYNC_DOWN);

    //then
    verify(errorRecordRepository).save(any(ErrorRecord.class));
  }

  @Test
  public void shouldStoreGeneralSyncErrorWithoutEvent() {
    //given
    Exception nullPointerException = new NullPointerException();

    //when
    doNothing().when(syncRecordService).storeLastSyncRecord();
    errorHandler.storeErrorRecord(nullPointerException, ErrorType.SYNC_DOWN);

    //then
    verify(errorRecordRepository).save(any(ErrorRecord.class));
  }

  @Test
  public void shouldStoreBusinessReplayErrorWithEvent() {
    //given
    BusinessDataException businessDataException = new BusinessDataException(new Message(ERROR_NOT_FOUND_SYNC_RECORD));

    //when
    doNothing().when(syncRecordService).storeLastReplayRecord();
    errorHandler.storeErrorRecord(eventId, businessDataException, ErrorType.SYNC_DOWN);

    //then
    verify(errorRecordRepository).save(any(ErrorRecord.class));
  }

  @Test
  public void shouldStoreGeneralReplayErrorWithEvent() {
    //given
    Exception nullPointerException = new NullPointerException();

    //when
    doNothing().when(syncRecordService).storeLastReplayRecord();
    errorHandler.storeErrorRecord(nullPointerException, ErrorType.SYNC_DOWN);

    //then
    verify(errorRecordRepository).save(any(ErrorRecord.class));
  }

  @Test
  public void shouldStoreBusinessSyncUpErrorWithEvents() {
    //given
    BusinessDataException businessDataException = new BusinessDataException(new Message(ERROR_NOT_FOUND_SYNC_RECORD));
    ArrayList<UUID> eventIds = Lists.newArrayList(eventId);

    //when
    doNothing().when(syncRecordService).storeLastSyncRecord();
    errorHandler.storeErrorRecord(eventIds, businessDataException, ErrorType.SYNC_UP);

    //then
    verify(errorRecordRepository).save(anyListOf(ErrorRecord.class));
  }

  @Test
  public void shouldStoreGeneralSyncUpErrorWithEvents() {
    //given
    Exception nullPointerException = new NullPointerException();
    ArrayList<UUID> eventIds = Lists.newArrayList(eventId);
    //when
    doNothing().when(syncRecordService).storeLastSyncRecord();
    errorHandler.storeErrorRecord(eventIds, nullPointerException, ErrorType.SYNC_UP);

    //then
    verify(errorRecordRepository).save(anyListOf(ErrorRecord.class));
  }
}