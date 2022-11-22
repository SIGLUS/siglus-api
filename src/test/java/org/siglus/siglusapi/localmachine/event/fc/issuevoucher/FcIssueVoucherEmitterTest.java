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

package org.siglus.siglusapi.localmachine.event.fc.issuevoucher;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.localmachine.EventPublisher;

@RunWith(MockitoJUnitRunner.class)
public class FcIssueVoucherEmitterTest {

  @InjectMocks
  private FcIssueVoucherEmitter emitter;

  @Mock
  private EventPublisher eventPublisher;

  @Test
  public void shouldEmitSuccessfully() {
    // given
    IssueVoucherDto issueVoucherDto = new IssueVoucherDto();
    FcIssueVoucherEvent event = new FcIssueVoucherEvent();
    event.setIssueVoucherDto(issueVoucherDto);
    UUID facilityId = UUID.randomUUID();
    String realRequisitionNumber = "RN12344";

    // when
    emitter.emit(issueVoucherDto, facilityId, realRequisitionNumber);

    // then
    verify(eventPublisher, times(1)).emitGroupEvent(realRequisitionNumber, facilityId, event);
  }
}