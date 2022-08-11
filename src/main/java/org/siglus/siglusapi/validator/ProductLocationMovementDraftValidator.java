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

import static org.aspectj.util.LangUtil.isEmpty;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_FACILITY_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_ID_MISMATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_ID_SHOULD_NULL;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_LINE_ITEMS_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_ORDERABLE_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PROGRAM_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_ID_MISSING;

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("ProductLocationMovementDraftValidator")
public class ProductLocationMovementDraftValidator {

  @Autowired
  private ProductLocationMovementDraftRepository productLocationMovementDraftRepository;

  public void validateEmptyMovementDraft(ProductLocationMovementDraftDto productLocationMovementDraftDto) {
    if (productLocationMovementDraftDto.getId() != null) {
      throw new ValidationMessageException(ERROR_MOVEMENT_DRAFT_ID_SHOULD_NULL);
    }
    validateNotNull(productLocationMovementDraftDto.getProgramId(), ERROR_PROGRAM_ID_MISSING);
    validateNotNull(productLocationMovementDraftDto.getUserId(), ERROR_USER_ID_MISSING);
    validateNotNull(productLocationMovementDraftDto.getFacilityId(), ERROR_FACILITY_ID_MISSING);
  }

  public void validateMovementDraft(ProductLocationMovementDraft movementDraft) {
    if (movementDraft == null) {
      throw new NotFoundException(ERROR_MOVEMENT_DRAFT_NOT_FOUND);
    }
  }

  public void validateMovementDraftAndLineItems(ProductLocationMovementDraftDto movementDraftDto, UUID id) {
    if (!movementDraftDto.getId().equals(id)) {
      throw new ValidationMessageException(ERROR_MOVEMENT_DRAFT_ID_MISMATCH);
    }
    ProductLocationMovementDraft movementDraft = productLocationMovementDraftRepository.findOne(id);
    validateMovementDraft(movementDraft);
    List<ProductLocationMovementDraftLineItemDto> lineItems = movementDraftDto.getLineItems();
    validateLineItems(lineItems);
  }

  private void validateLineItems(List<ProductLocationMovementDraftLineItemDto> lineItems) {
    if (isEmpty(lineItems)) {
      throw new ValidationMessageException(ERROR_MOVEMENT_DRAFT_LINE_ITEMS_MISSING);
    }

    boolean orderableMissing = lineItems.stream().anyMatch(lineItem -> lineItem.getOrderableId() == null);
    if (orderableMissing) {
      throw new ValidationMessageException(ERROR_MOVEMENT_DRAFT_ORDERABLE_MISSING);
    }
  }

  private void validateNotNull(Object field, String errorMessage) {
    if (field == null) {
      throw new ValidationMessageException(errorMessage);
    }
  }
}
