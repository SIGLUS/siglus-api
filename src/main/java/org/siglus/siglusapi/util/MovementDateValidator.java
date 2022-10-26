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

package org.siglus.siglusapi.util;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_VALIDATE_STOCK_MOVEMENT_DATE;

import java.time.LocalDate;
import java.util.UUID;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.MovementStartDateDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.springframework.stereotype.Service;

@Service
public class MovementDateValidator {

  private final ProcessingPeriodRepository processingPeriodRepository;

  private final SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;

  public MovementDateValidator(ProcessingPeriodRepository processingPeriodRepository,
      SiglusStockCardLineItemRepository siglusStockCardLineItemRepository) {
    this.processingPeriodRepository = processingPeriodRepository;
    this.siglusStockCardLineItemRepository = siglusStockCardLineItemRepository;
  }


  public MovementStartDateDto getMovementStartDate(LocalDate input, UUID facilityId) {
    LocalDate currentPeriodStartDate = processingPeriodRepository.getCurrentPeriodStartDate(input);
    LocalDate facilityLastMovementDate = siglusStockCardLineItemRepository.findFacilityLastMovementDate(facilityId);
    if (facilityLastMovementDate == null || currentPeriodStartDate.isAfter(facilityLastMovementDate)) {
      return new MovementStartDateDto(currentPeriodStartDate);
    }
    return new MovementStartDateDto(facilityLastMovementDate);
  }

  public void validateMovementDate(LocalDate input, UUID facilityId) {
    LocalDate startDate = getMovementStartDate(input, facilityId).getLastMovementDate();
    if (input.isBefore(startDate)) {
      throw new BusinessDataException(new Message(ERROR_VALIDATE_STOCK_MOVEMENT_DATE));
    }
  }
}
