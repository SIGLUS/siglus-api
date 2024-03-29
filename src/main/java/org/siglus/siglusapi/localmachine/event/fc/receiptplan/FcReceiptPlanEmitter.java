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

import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.FC_RECEIPT_PLAN;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcReceiptPlanEmitter {

  private final EventPublisher eventPublisher;

  public FcReceiptPlanEvent emit(ReceiptPlanDto receiptPlanDto, UUID facilityId, String realRequisitionNumber) {
    log.info("get event of fc receipt plan, requisitionNumber = " + receiptPlanDto.getReceiptPlanNumber());
    FcReceiptPlanEvent event = new FcReceiptPlanEvent();
    event.setReceiptPlanDto(receiptPlanDto);
    eventPublisher.emitGroupEvent(realRequisitionNumber, facilityId, event, FC_RECEIPT_PLAN);
    return event;
  }

}
