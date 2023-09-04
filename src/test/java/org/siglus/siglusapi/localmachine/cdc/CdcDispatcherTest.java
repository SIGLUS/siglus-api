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

package org.siglus.siglusapi.localmachine.cdc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class CdcDispatcherTest {

  @Test
  public void shouldDispatchRecordsToListenerWhenDispatchAll() {
    // given
    CdcRecordRepository cdcRecordRepository = mock(CdcRecordRepository.class);
    CdcRecord cdcRecord = CdcRecord.builder().id(1L).schema("schema").table("table1").build();
    List<CdcRecord> cdcRecords = Collections.singletonList(cdcRecord);
    CdcListener cdcListener = mock(CdcListener.class);
    given(cdcListener.acceptedTables()).willReturn(new String[] {"schema.table1", "schema.table2"});
    CdcDispatcher cdcDispatcher = new CdcDispatcher(Collections.singletonList(cdcListener), cdcRecordRepository);
    // when
    cdcDispatcher.doDispatch(cdcRecords);
    // then
    verify(cdcListener, times(1)).on(anyListOf(CdcRecord.class));
  }
}
