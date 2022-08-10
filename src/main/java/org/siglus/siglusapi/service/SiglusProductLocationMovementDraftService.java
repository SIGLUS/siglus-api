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

package org.siglus.siglusapi.service;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MOVEMENT_DRAFT_EXISTS;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;
import org.siglus.siglusapi.validator.StockManagementDraftValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiglusProductLocationMovementDraftService {

  @Autowired
  private ProductLocationMovementDraftRepository productLocationMovementDraftRepository;

  @Autowired
  StockManagementDraftValidator stockManagementDraftValidator;

  public ProductLocationMovementDraftDto createEmptyProductLocationMovementDraft(
      ProductLocationMovementDraftDto productLocationMovementDraftDto) {
    log.info("create stock movement draft");
    stockManagementDraftValidator.validateEmptyStockMovementDraft(productLocationMovementDraftDto);

    checkIfMovementDraftExists(productLocationMovementDraftDto);
    ProductLocationMovementDraft emptyProductLocationMovementDraft = ProductLocationMovementDraft
        .createEmptyStockMovementDraft(productLocationMovementDraftDto);
    ProductLocationMovementDraft savedDraft = productLocationMovementDraftRepository
        .save(emptyProductLocationMovementDraft);
    return ProductLocationMovementDraftDto.from(savedDraft);
  }

  private void checkIfMovementDraftExists(ProductLocationMovementDraftDto stockManagementDraftDto) {
    List<ProductLocationMovementDraft> drafts = productLocationMovementDraftRepository
        .findByProgramIdAndFacilityId(stockManagementDraftDto.getProgramId(), stockManagementDraftDto.getFacilityId());
    if (CollectionUtils.isNotEmpty(drafts)) {
      throw new ValidationMessageException(
          new Message(ERROR_STOCK_MOVEMENT_DRAFT_EXISTS, stockManagementDraftDto.getProgramId(),
              stockManagementDraftDto.getFacilityId()));
    }
  }
}
