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

import static org.siglus.common.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS;
import static org.siglus.common.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.util.Message;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockManagementDraftValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SiglusStockManagementDraftService {

  @Autowired
  private ActiveDraftValidator draftValidator;

  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Autowired
  StockManagementDraftValidator stockManagementDraftValidator;

  @Transactional
  public StockManagementDraftDto createNewDraft(StockManagementDraftDto dto) {
    log.info("create physical inventory draft");
    stockManagementDraftValidator.validateEmptyDraft(dto);
    checkIfDraftExists(dto);

    draftValidator.validateProgramId(dto.getProgramId());
    draftValidator.validateFacilityId(dto.getFacilityId());
    draftValidator.validateUserId(dto.getUserId());
    draftValidator.validateDraftType(dto.getDraftType());

    StockManagementDraft draft = StockManagementDraft.createEmptyDraft(dto);
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);

    return StockManagementDraftDto.from(savedDraft);
  }

  @Transactional
  public StockManagementDraftDto saveDraft(StockManagementDraftDto dto, UUID id) {
    log.info("save physical inventory draft");
    stockManagementDraftValidator.validateDraft(dto, id);
    StockManagementDraft draft = StockManagementDraft.createStockManagementDraft(dto, true);
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);
    return StockManagementDraftDto.from(savedDraft);
  }

  public List<StockManagementDraftDto> findStockManagementDraft(UUID programId, UUID userId, String type,
      Boolean isDraft) {
    draftValidator.validateProgramId(programId);
    draftValidator.validateUserId(userId);
    draftValidator.validateDraftType(type);
    draftValidator.validateIsDraft(isDraft);
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndUserIdAndIsDraftAndDraftType(programId, userId, isDraft, type);
    return StockManagementDraftDto.from(drafts);
  }

  @Transactional
  public void deleteStockManagementDraft(UUID id) {
    StockManagementDraft drafts = stockManagementDraftRepository.findOne(id);
    if (drafts != null) {
      draftValidator.validateDraftUser(drafts);
      log.info("delete stockmanagement draft: {}", drafts);
      stockManagementDraftRepository.delete(drafts);
    } else {
      throw new ResourceNotFoundException(
          new org.openlmis.stockmanagement.util.Message(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND, id));
    }
  }


  public void deleteStockManagementDraft(StockEventDto dto) {
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndUserIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getUserId(), true, dto.getType());
    if (!drafts.isEmpty()) {
      log.info("delete stockmanagement draft: {}", drafts);
      stockManagementDraftRepository.delete(drafts);
    }
  }

  private void checkIfDraftExists(StockManagementDraftDto dto) {
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndUserIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getUserId(), true, dto.getDraftType());
    if (!drafts.isEmpty()) {
      throw new ValidationMessageException(
          new Message(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS, dto.getProgramId(), dto.getUserId()));
    }
  }
}
