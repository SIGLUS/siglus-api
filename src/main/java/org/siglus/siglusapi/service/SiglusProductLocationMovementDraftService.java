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
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.ProductLocationMovementDraftValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SiglusProductLocationMovementDraftService {

  @Autowired
  private ProductLocationMovementDraftRepository productLocationMovementDraftRepository;

  @Autowired
  ProductLocationMovementDraftValidator productLocationMovementDraftValidator;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private ActiveDraftValidator draftValidator;

  @Autowired
  private OperatePermissionService operatePermissionService;

  @Transactional
  public ProductLocationMovementDraftDto createEmptyMovementDraft(
      ProductLocationMovementDraftDto productLocationMovementDraftDto) {
    productLocationMovementDraftValidator.validateEmptyMovementDraft(productLocationMovementDraftDto);
    checkIfMovementDraftExists(productLocationMovementDraftDto);
    log.info("create stock movement draft");
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
          new Message(ERROR_MOVEMENT_DRAFT_EXISTS, stockManagementDraftDto.getProgramId(),
              stockManagementDraftDto.getFacilityId()));
    }
  }

  public List<ProductLocationMovementDraftDto> searchMovementDrafts(UUID programId) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    operatePermissionService.checkPermission(facilityId);
    draftValidator.validateProgramId(programId);
    draftValidator.validateFacilityId(facilityId);

    List<ProductLocationMovementDraft> productLocationMovementDrafts = productLocationMovementDraftRepository
        .findByProgramIdAndFacilityId(programId, facilityId);

    return ProductLocationMovementDraftDto.from(productLocationMovementDrafts);
  }

  public ProductLocationMovementDraftDto searchMovementDraft(UUID id) {
    ProductLocationMovementDraft movementDraft = productLocationMovementDraftRepository.findOne(id);
    productLocationMovementDraftValidator.validateMovementDraft(movementDraft);
    return ProductLocationMovementDraftDto.from(movementDraft);
  }

  @Transactional
  public ProductLocationMovementDraftDto updateMovementDraft(ProductLocationMovementDraftDto movementDraftDto,
      UUID movementDraftId) {
    productLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
    checkStockOnHand(movementDraftDto);
    ProductLocationMovementDraft stockMovementDraft = ProductLocationMovementDraft
        .createMovementDraft(movementDraftDto);
    log.info("update movement draft with id: {}", movementDraftId);
    ProductLocationMovementDraft savedMovementDraft = productLocationMovementDraftRepository.save(stockMovementDraft);
    return ProductLocationMovementDraftDto.from(savedMovementDraft);
  }

  @Transactional
  public void deleteMovementDraft(UUID movementDraftId) {
    ProductLocationMovementDraft movementDraft = productLocationMovementDraftRepository.findOne(movementDraftId);
    productLocationMovementDraftValidator.validateMovementDraft(movementDraft);
    log.info("delete movement draft with id: {}", movementDraftId);
    productLocationMovementDraftRepository.delete(movementDraft);
  }

  private void checkStockOnHand(ProductLocationMovementDraftDto movementDraftDto) {
    List<ProductLocationMovementDraftLineItemDto> lineItems = movementDraftDto.getLineItems();
    Set<Entry<String, List<ProductLocationMovementDraftLineItemDto>>> groupByOrderableIdLotIdSrcAreaAndSrcLocationCode =
        lineItems.stream().collect(Collectors.groupingBy(this::fetchGroupKey)).entrySet();
    groupByOrderableIdLotIdSrcAreaAndSrcLocationCode.forEach(entry -> {
      int totalMoveQuantity = entry.getValue()
          .stream()
          .mapToInt(ProductLocationMovementDraftLineItemDto::getQuantity)
          .sum();
      if (totalMoveQuantity > entry.getValue().get(0).getStockOnHand()) {
        throw new BusinessDataException(new Message(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND),
            "movement quantity more than stock on hand");
      }
    });
  }

  private String fetchGroupKey(ProductLocationMovementDraftLineItemDto movementDraftLineItemDto) {
    return movementDraftLineItemDto.getOrderableId().toString()
        + movementDraftLineItemDto.getLotId().toString()
        + movementDraftLineItemDto.getSrcArea()
        + movementDraftLineItemDto.getSrcLocationCode();
  }
}
