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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_EXISTS;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockCardLocationMovementDraftValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SiglusStockCardLocationMovementDraftService {

  @Autowired
  private StockCardLocationMovementDraftRepository stockCardLocationMovementDraftRepository;

  @Autowired
  StockCardLocationMovementDraftValidator stockCardLocationMovementDraftValidator;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private ActiveDraftValidator draftValidator;

  @Autowired
  private OperatePermissionService operatePermissionService;

  @Transactional
  public StockCardLocationMovementDraftDto createEmptyMovementDraft(
      StockCardLocationMovementDraftDto stockCardLocationMovementDraftDto) {
    stockCardLocationMovementDraftValidator.validateEmptyMovementDraft(stockCardLocationMovementDraftDto);
    checkIfMovementDraftExists(stockCardLocationMovementDraftDto);
    log.info("create stock movement draft");
    StockCardLocationMovementDraft emptyStockCardLocationMovementDraft = StockCardLocationMovementDraft
        .createEmptyStockMovementDraft(stockCardLocationMovementDraftDto);
    StockCardLocationMovementDraft savedDraft = stockCardLocationMovementDraftRepository
        .save(emptyStockCardLocationMovementDraft);
    return StockCardLocationMovementDraftDto.from(savedDraft);
  }

  private void checkIfMovementDraftExists(StockCardLocationMovementDraftDto stockManagementDraftDto) {
    List<StockCardLocationMovementDraft> drafts = stockCardLocationMovementDraftRepository
        .findByProgramIdAndFacilityId(stockManagementDraftDto.getProgramId(), stockManagementDraftDto.getFacilityId());
    if (CollectionUtils.isNotEmpty(drafts)) {
      throw new ValidationMessageException(
          new Message(ERROR_MOVEMENT_DRAFT_EXISTS, stockManagementDraftDto.getProgramId(),
              stockManagementDraftDto.getFacilityId()));
    }
  }

  public List<StockCardLocationMovementDraftDto> searchMovementDrafts(UUID programId) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    operatePermissionService.checkPermission(facilityId);
    draftValidator.validateProgramId(programId);
    draftValidator.validateFacilityId(facilityId);

    List<StockCardLocationMovementDraft> stockCardLocationMovementDrafts = stockCardLocationMovementDraftRepository
        .findByProgramIdAndFacilityId(programId, facilityId);

    return StockCardLocationMovementDraftDto.from(stockCardLocationMovementDrafts);
  }

  public StockCardLocationMovementDraftDto searchMovementDraft(UUID id) {
    StockCardLocationMovementDraft movementDraft = stockCardLocationMovementDraftRepository.findOne(id);
    stockCardLocationMovementDraftValidator.validateMovementDraft(movementDraft);
    return StockCardLocationMovementDraftDto.from(movementDraft);
  }

  @Transactional
  public StockCardLocationMovementDraftDto updateMovementDraft(StockCardLocationMovementDraftDto movementDraftDto,
      UUID movementDraftId) {
    stockCardLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
    checkStockOnHand(movementDraftDto);
    StockCardLocationMovementDraft stockMovementDraft = StockCardLocationMovementDraft
        .createMovementDraft(movementDraftDto);
    log.info("update movement draft with id: {}", movementDraftId);
    StockCardLocationMovementDraft savedMovementDraft = stockCardLocationMovementDraftRepository
        .save(stockMovementDraft);
    return StockCardLocationMovementDraftDto.from(savedMovementDraft);
  }

  @Transactional
  public void deleteMovementDraft(UUID movementDraftId) {
    StockCardLocationMovementDraft movementDraft = stockCardLocationMovementDraftRepository.findOne(movementDraftId);
    stockCardLocationMovementDraftValidator.validateMovementDraft(movementDraft);
    log.info("delete movement draft with id: {}", movementDraftId);
    stockCardLocationMovementDraftRepository.delete(movementDraft);
  }

  private void checkStockOnHand(StockCardLocationMovementDraftDto movementDraftDto) {
    List<StockCardLocationMovementDraftLineItemDto> lineItems = movementDraftDto.getLineItems();
    Set<Entry<String, List<StockCardLocationMovementDraftLineItemDto>>> groupByOrderableIdLotIdSrcAreaAndSrcLocationCode
        = lineItems.stream().collect(Collectors.groupingBy(this::fetchGroupKey)).entrySet();
    groupByOrderableIdLotIdSrcAreaAndSrcLocationCode.forEach(entry -> {
      int totalMoveQuantity = entry.getValue()
          .stream()
          .mapToInt(StockCardLocationMovementDraftLineItemDto::getQuantity)
          .sum();
      if (totalMoveQuantity > entry.getValue().get(0).getStockOnHand()) {
        throw new BusinessDataException(new Message(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND),
            "movement quantity more than stock on hand");
      }
    });
  }

  private String fetchGroupKey(StockCardLocationMovementDraftLineItemDto lineItemDto) {
    return Boolean.TRUE.equals(lineItemDto.getIsKit()) ? lineItemDto.getOrderableId().toString()
        + lineItemDto.getSrcArea()
        + lineItemDto.getSrcLocationCode() : lineItemDto.getOrderableId().toString()
        + lineItemDto.getLotId().toString()
        + lineItemDto.getSrcArea()
        + lineItemDto.getSrcLocationCode();
  }
}
