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

package org.siglus.siglusapi.validator;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_FACILITY_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_ID_MISMATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_ID_SHOULD_NULL;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PROGRAM_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_ID_MISSING;

import java.util.UUID;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("StockCardLocationMovementDraftValidator")
public class StockCardLocationMovementDraftValidator {

  @Autowired
  private StockCardLocationMovementDraftRepository stockCardLocationMovementDraftRepository;

  public void validateEmptyMovementDraft(StockCardLocationMovementDraftDto stockCardLocationMovementDraftDto) {
    if (stockCardLocationMovementDraftDto.getId() != null) {
      throw new ValidationMessageException(ERROR_MOVEMENT_DRAFT_ID_SHOULD_NULL);
    }
    validateNotNull(stockCardLocationMovementDraftDto.getProgramId(), ERROR_PROGRAM_ID_MISSING);
    validateNotNull(stockCardLocationMovementDraftDto.getUserId(), ERROR_USER_ID_MISSING);
    validateNotNull(stockCardLocationMovementDraftDto.getFacilityId(), ERROR_FACILITY_ID_MISSING);
  }

  public void validateMovementDraft(StockCardLocationMovementDraft movementDraft) {
    if (movementDraft == null) {
      throw new NotFoundException(ERROR_MOVEMENT_DRAFT_NOT_FOUND);
    }
  }

  public void validateMovementDraftAndLineItems(StockCardLocationMovementDraftDto movementDraftDto, UUID id) {
    if (!movementDraftDto.getId().equals(id)) {
      throw new ValidationMessageException(ERROR_MOVEMENT_DRAFT_ID_MISMATCH);
    }
    StockCardLocationMovementDraft movementDraft = stockCardLocationMovementDraftRepository.findOne(id);
    validateMovementDraft(movementDraft);
  }

  private void validateNotNull(Object field, String errorMessage) {
    if (field == null) {
      throw new ValidationMessageException(errorMessage);
    }
  }
}
