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
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_DRAFT_DOCUMENT_NUMBER_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_DRAFT_TYPE_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_FACILITY_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PROGRAM_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_MISMATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_SHOULD_NULL;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_LINE_ITEMS_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_DISABLED_VVM;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_ID_MISSING;

import java.util.List;
import java.util.UUID;
import org.openlmis.stockmanagement.validators.VvmValidator;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("StockManagementDraftValidator")
public class StockManagementDraftValidator {

  @Autowired
  private VvmValidator vvmValidator;

  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;

  public void validateEmptyDraft(StockManagementDraftDto inventory) {
    if (inventory.getId() != null) {
      throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_ID_SHOULD_NULL);
    }
    validateNotNull(inventory.getProgramId(), ERROR_PROGRAM_ID_MISSING);
    validateNotNull(inventory.getUserId(), ERROR_USER_ID_MISSING);
    validateNotNull(inventory.getFacilityId(), ERROR_FACILITY_ID_MISSING);
  }

  public void validateInitialDraft(StockManagementInitialDraftDto stockManagementInitialDraftDto) {
    if (stockManagementInitialDraftDto.getId() != null) {
      throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_ID_SHOULD_NULL);
    }
    validateNotNull(stockManagementInitialDraftDto.getProgramId(), ERROR_PROGRAM_ID_MISSING);
    validateNotNull(stockManagementInitialDraftDto.getFacilityId(), ERROR_FACILITY_ID_MISSING);
    validateNotNull(stockManagementInitialDraftDto.getDraftType(), ERROR_DRAFT_TYPE_MISSING);
    validateNotNull(stockManagementInitialDraftDto.getDocumentNumber(),
        ERROR_DRAFT_DOCUMENT_NUMBER_MISSING);
  }

  private void validateNotNull(Object field, String errorMessage) {
    if (field == null) {
      throw new ValidationMessageException(errorMessage);
    }
  }

  public void validateDraft(StockManagementDraftDto draft, UUID id) {
    if (!draft.getId().equals(id)) {
      throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_ID_MISMATCH);
    }
    StockManagementDraft foundDraft = stockManagementDraftRepository.findOne(id);
    if (foundDraft == null) {
      throw new ValidationMessageException(
          new Message(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND) + id.toString());
    } else if (Boolean.TRUE.equals(!foundDraft.getIsDraft())) {
      throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED);
    }
    List<StockManagementDraftLineItemDto> lineItems = draft.getLineItems();
    validateLineItems(lineItems);
    vvmValidator.validate(lineItems, ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_DISABLED_VVM, false);
  }

  private void validateLineItems(List<StockManagementDraftLineItemDto> lineItems) {
    if (isEmpty(lineItems)) {
      throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_LINE_ITEMS_MISSING);
    }

    boolean orderableMissing = lineItems.stream()
        .anyMatch(lineItem -> lineItem.getOrderableId() == null);
    if (orderableMissing) {
      throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_MISSING);
    }
  }
}
