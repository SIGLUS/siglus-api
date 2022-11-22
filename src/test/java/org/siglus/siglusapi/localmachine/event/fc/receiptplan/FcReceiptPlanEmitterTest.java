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

package org.siglus.siglusapi.localmachine.event.fc.receiptplan;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.FC_RECEIPT_PLAN;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.localmachine.EventPublisher;

@RunWith(MockitoJUnitRunner.class)
public class FcReceiptPlanEmitterTest {

  @InjectMocks
  private FcReceiptPlanEmitter emitter;

  @Mock
  private EventPublisher eventPublisher;

  @Test
  public void shouldEmitSuccessfully() {
    // given
    ReceiptPlanDto receiptPlanDto = new ReceiptPlanDto();
    FcReceiptPlanEvent event = new FcReceiptPlanEvent();
    event.setReceiptPlanDto(receiptPlanDto);
    UUID facilityId = UUID.randomUUID();
    String realRequisitionNumber = "RN123445566";

    // when
    emitter.emit(receiptPlanDto, facilityId, realRequisitionNumber);

    // then
    verify(eventPublisher, times(1)).emitGroupEvent(realRequisitionNumber, facilityId, event, FC_RECEIPT_PLAN);
  }
}