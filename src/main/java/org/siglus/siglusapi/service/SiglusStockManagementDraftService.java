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

import static java.util.stream.Collectors.toList;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_INITIAL_DRAFT_EXISTS;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_MORE_THAN_TEN;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFT_NOT_ALL_SUBMITTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.domain.StockManagementInitialDraft;
import org.siglus.siglusapi.dto.MergedLineItemDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.repository.StockManagementInitialDraftsRepository;
import org.siglus.siglusapi.util.ConflictOrderableInSubDraftsService;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockManagementDraftValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusStockManagementDraftService {

  @Autowired
  private ActiveDraftValidator draftValidator;

  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Autowired
  private StockManagementInitialDraftsRepository stockManagementInitialDraftsRepository;

  @Autowired
  StockManagementDraftValidator stockManagementDraftValidator;

  @Autowired
  private SiglusStockCardService stockCardService;

  @Autowired
  private SiglusValidSourceDestinationService validSourceDestinationService;

  @Autowired
  private OperatePermissionService operatePermissionService;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private ConflictOrderableInSubDraftsService conflictOrderableInSubDraftsService;

  private static final Integer DRAFTS_LIMITATION = 10;
  private static final Integer DRAFTS_INCREMENT = 1;

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
  public StockManagementDraftDto createNewSubDraft(StockManagementDraftDto dto) {
    log.info("create physical inventory subDraft");
    stockManagementDraftValidator.validateEmptyDraft(dto);
    draftValidator.validateInitialDraftId(dto.getInitialDraftId());
    draftValidator.validateDraftType(dto.getDraftType());

    checkIfSameDraftsOversize(dto);

    StockManagementDraft draft = StockManagementDraft.createEmptySubDraft(dto);
    UUID initialDraftId = draft.getInitialDraftId();
    int subDraftsQuantity = stockManagementDraftRepository.countByInitialDraftId(initialDraftId);
    draft.setDraftNumber(subDraftsQuantity + DRAFTS_INCREMENT);
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);

    return StockManagementDraftDto.from(savedDraft);
  }

  @Transactional
  public StockManagementDraftDto updateDraft(StockManagementDraftDto subDraftDto, UUID id) {
    log.info("update issue draft");
    stockManagementDraftValidator.validateDraft(subDraftDto, id);
    if (subDraftDto.getDraftType().equals(FieldConstants.ISSUE)
        || subDraftDto.getDraftType().equals(FieldConstants.RECEIVE)) {
      StockManagementDraft subDraft = stockManagementDraftRepository.findOne(id);
      draftValidator.validateSubDraftStatus(subDraft);
      conflictOrderableInSubDraftsService.checkConflictOrderableBetweenSubDrafts(subDraftDto);
      StockManagementDraft newDraft = setNewAttributesInOriginalDraft(subDraftDto, id);
      StockManagementDraft savedDraft = stockManagementDraftRepository.save(newDraft);
      return StockManagementDraftDto.from(savedDraft);
    }
    StockManagementDraft draft = StockManagementDraft.createStockManagementDraft(subDraftDto, true);
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);
    return StockManagementDraftDto.from(savedDraft);
  }

  public StockManagementDraft setNewAttributesInOriginalDraft(StockManagementDraftDto dto,
      UUID id) {
    StockManagementDraft originalDraft = stockManagementDraftRepository.findOne(id);
    StockManagementDraft newDraft = new StockManagementDraft();
    BeanUtils.copyProperties(originalDraft, newDraft);
    newDraft.setSignature(dto.getSignature());
    newDraft.setStatus(PhysicalInventorySubDraftEnum.DRAFT);
    List<StockManagementDraftLineItemDto> lineItemDtos = dto.getLineItems();
    if (lineItemDtos != null) {
      newDraft.setLineItems(lineItemDtos.stream()
          .map(lineItemDto -> StockManagementDraftLineItem.from(lineItemDto, newDraft))
          .collect(toList()));
    }
    return newDraft;
  }

  public List<StockManagementDraftDto> findStockManagementDraft(UUID programId, String type,
      Boolean isDraft) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    draftValidator.validateProgramId(programId);
    draftValidator.validateFacilityId(facilityId);
    draftValidator.validateDraftType(type);
    draftValidator.validateIsDraft(isDraft);
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId, facilityId, isDraft, type);
    return StockManagementDraftDto.from(drafts);
  }

  public List<StockManagementDraftDto> findStockManagementDrafts(UUID initialDraftId) {
    draftValidator.validateInitialDraftId(initialDraftId);
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByInitialDraftId(initialDraftId);
    List<StockManagementDraft> sortedDrafts = drafts.stream()
        .sorted(Comparator.comparingInt(StockManagementDraft::getDraftNumber))
        .collect(toList());
    return StockManagementDraftDto.from(sortedDrafts);
  }

  public void deleteStockManagementDraft(StockEventDto dto) {
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getFacilityId(), true,
            dto.getType());
    if (!drafts.isEmpty()) {
      log.info("delete stock management draft(s), programId: {}, facilityId: {}",
          dto.getProgramId(),
          dto.getFacilityId());
      stockManagementDraftRepository.delete(drafts);
      UUID initialDraftId = drafts.get(0).getInitialDraftId();
      if (initialDraftId != null) {
        log.info("delete stock management initial draft with id: {}", initialDraftId);
        stockManagementInitialDraftsRepository.delete(initialDraftId);
      }
    }
  }

  @Transactional
  public void deleteStockManagementDraft(UUID id) {
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(id);
    draftValidator.validateSubDraft(subDraft);
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    operatePermissionService.checkPermission(facilityId);
    log.info("delete stockmanagement draft: {}", id);
    stockManagementDraftRepository.delete(subDraft);
    if (!subDraft.getDraftType().equals(FieldConstants.ADJUSTMENT)) {
      resetDraftNumber(subDraft);
    }
  }

  @Transactional
  public void resetDraftNumber(StockManagementDraft draft) {
    List<StockManagementDraft> subDrafts = stockManagementDraftRepository
        .findByInitialDraftId(draft.getInitialDraftId());
    List<StockManagementDraft> filterSubDrafts = subDrafts.stream()
        .filter(subDraft -> subDraft.getDraftNumber() > draft.getDraftNumber())
        .collect(toList());
    if (!filterSubDrafts.isEmpty()) {
      filterSubDrafts.forEach(
          subDraft -> subDraft.setDraftNumber(subDraft.getDraftNumber() - DRAFTS_INCREMENT));
      stockManagementDraftRepository.save(filterSubDrafts);
    }
  }

  private void checkIfDraftExists(StockManagementDraftDto dto) {
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getFacilityId(),
            true,
            dto.getDraftType());
    if (CollectionUtils.isNotEmpty(drafts)) {
      throw new ValidationMessageException(
          new Message(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS, dto.getProgramId(),
              dto.getFacilityId()));
    }
  }

  private void checkIfSameDraftsOversize(StockManagementDraftDto dto) {
    Integer subDraftsQuantity = stockManagementDraftRepository
        .countByInitialDraftId(dto.getInitialDraftId());
    if (subDraftsQuantity > DRAFTS_LIMITATION - 1) {
      throw new BusinessDataException(
          new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_MORE_THAN_TEN, dto.getProgramId(),
              dto.getFacilityId()), "subDrafts are more than limitation");
    }
  }

  private String findDestinationName(UUID destinationNodeId, UUID facilityId) {
    Collection<ValidSourceDestinationDto> destinationsForAllProducts = validSourceDestinationService
        .findDestinationsForAllProducts(facilityId);

    return destinationsForAllProducts
        .stream().filter(destination -> (
            destination.getNode().getId().equals(destinationNodeId)
        )).findFirst()
        .orElseThrow(() -> new NotFoundException("No such destination node with id: " + destinationNodeId))
        .getName();
  }

  @Transactional
  public StockManagementInitialDraftDto createInitialDraft(
      StockManagementInitialDraftDto initialDraftDto) {
    operatePermissionService.checkPermission(initialDraftDto.getFacilityId());
    log.info("create stock management initial draft");
    stockManagementDraftValidator.validateInitialDraft(initialDraftDto);

    checkIfInitialDraftExists(
        initialDraftDto.getProgramId(),
        initialDraftDto.getFacilityId(),
        initialDraftDto.getDraftType());

    StockManagementInitialDraft initialDraft = StockManagementInitialDraft
        .createInitialDraft(initialDraftDto);

    StockManagementInitialDraft savedInitialDraft = stockManagementInitialDraftsRepository
        .save(initialDraft);
    StockManagementInitialDraftDto initialDraftDtoResponse = StockManagementInitialDraftDto
        .from(savedInitialDraft);

    if (initialDraftDto.getDraftType().equals(FieldConstants.ISSUE)) {
      String destinationName = findDestinationName(savedInitialDraft.getDestinationId(),
          savedInitialDraft.getFacilityId());
      initialDraftDtoResponse.setDestinationName(destinationName);
      return initialDraftDtoResponse;
    } else if (initialDraftDto.getDraftType().equals(FieldConstants.RECEIVE)) {
      String sourceName = findSourceName(savedInitialDraft.getSourceId(),
          savedInitialDraft.getFacilityId());
      initialDraftDtoResponse.setSourceName(sourceName);
      return initialDraftDtoResponse;
    }
    return new StockManagementInitialDraftDto();
  }

  public StockManagementInitialDraftDto findStockManagementInitialDraft(
      UUID programId, String draftType) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    operatePermissionService.checkPermission(facilityId);
    draftValidator.validateProgramId(programId);
    draftValidator.validateFacilityId(facilityId);
    draftValidator.validateDraftType(draftType);
    boolean canMergeOrDeleteSubDrafts = authenticationHelper
        .isTheCurrentUserCanMergeOrDeleteSubDrafts();
    List<StockManagementInitialDraft> initialDrafts = stockManagementInitialDraftsRepository
        .findByProgramIdAndFacilityIdAndDraftType(programId, facilityId, draftType);
    StockManagementInitialDraft initialDraft = initialDrafts.stream().findFirst().orElse(null);
    if (initialDraft != null) {
      StockManagementInitialDraftDto stockManagementInitialDraftDto = StockManagementInitialDraftDto
          .from(initialDraft);
      if (draftType.equals(FieldConstants.ISSUE)) {
        String destinationName = findDestinationName(initialDraft.getDestinationId(), facilityId);
        stockManagementInitialDraftDto.setDestinationName(destinationName);
        stockManagementInitialDraftDto.setCanMergeOrDeleteSubDrafts(canMergeOrDeleteSubDrafts);
        return stockManagementInitialDraftDto;
      } else if (draftType.equals(FieldConstants.RECEIVE)) {
        String sourceName = findSourceName(initialDraft.getSourceId(),
            initialDraft.getFacilityId());
        stockManagementInitialDraftDto.setSourceName(sourceName);
        stockManagementInitialDraftDto.setCanMergeOrDeleteSubDrafts(canMergeOrDeleteSubDrafts);
        return stockManagementInitialDraftDto;
      }
    }
    return new StockManagementInitialDraftDto();
  }

  private String findSourceName(UUID sourceNodeId, UUID facilityId) {
    Collection<ValidSourceDestinationDto> sourcesForAllProducts = validSourceDestinationService
        .findSourcesForAllProducts(facilityId);

    return sourcesForAllProducts
        .stream().filter(source -> (
            source.getNode().getId().equals(sourceNodeId)
        )).findFirst()
        .orElseThrow(() -> new NotFoundException("No such source node with id: " + sourceNodeId))
        .getName();
  }

  private void checkIfInitialDraftExists(UUID programId, UUID facilityId, String draftType) {
    List<StockManagementInitialDraft> initialDrafts = stockManagementInitialDraftsRepository
        .findByProgramIdAndFacilityIdAndDraftType(programId, facilityId, draftType);
    if (!initialDrafts.isEmpty()) {
      throw new BusinessDataException(
          new Message(ERROR_STOCK_MANAGEMENT_INITIAL_DRAFT_EXISTS, programId, facilityId,
              draftType), "same initial draft exists");
    }
  }

  @Transactional
  public StockManagementDraftDto updateOperatorAndStatus(StockManagementDraftDto draftDto) {
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(draftDto.getId());
    draftValidator.validateSubDraft(subDraft);
    subDraft.setStatus(PhysicalInventorySubDraftEnum.DRAFT);
    subDraft.setOperator(draftDto.getOperator());
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(subDraft);
    return StockManagementDraftDto.from(savedDraft);
  }

  public StockManagementDraftDto searchDraft(UUID id) {
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(id);
    draftValidator.validateSubDraft(subDraft);
    return StockManagementDraftDto.from(subDraft);
  }

  @Transactional
  public StockManagementDraftDto restoreSubDraftWhenDoDelete(StockManagementDraftDto dto) {
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(dto.getId());
    draftValidator.validateSubDraft(subDraft);
    draftValidator.validateSubDraftStatus(subDraft);
    StockManagementDraft currentDraft = new StockManagementDraft();
    BeanUtils.copyProperties(subDraft, currentDraft);
    currentDraft.setStatus(PhysicalInventorySubDraftEnum.NOT_YET_STARTED);
    currentDraft.setOperator(null);
    currentDraft.setLineItems(Collections.emptyList());
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(currentDraft);
    return StockManagementDraftDto.from(savedDraft);
  }

  @Transactional
  public StockManagementDraftDto updateStatusAfterSubmit(StockManagementDraftDto subDraftDto) {
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(subDraftDto.getId());
    draftValidator.validateSubDraft(subDraft);
    conflictOrderableInSubDraftsService.checkConflictOrderableAndLotInSubDraft(subDraftDto);
    draftValidator.validateSubDraftStatus(subDraft);
    conflictOrderableInSubDraftsService.checkConflictOrderableBetweenSubDrafts(subDraftDto);
    subDraft.setStatus(PhysicalInventorySubDraftEnum.SUBMITTED);
    subDraft.setSignature(subDraftDto.getSignature());
    List<StockManagementDraftLineItemDto> lineItems = subDraftDto.getLineItems();
    List<StockManagementDraftLineItem> draftLineItems = lineItems.stream()
        .map(lineItemDto -> StockManagementDraftLineItem.from(lineItemDto, subDraft))
        .collect(toList());
    StockManagementDraft submitDraft = new StockManagementDraft();
    BeanUtils.copyProperties(subDraft, submitDraft);
    submitDraft.setLineItems(draftLineItems);

    StockManagementDraft updatedDraft = stockManagementDraftRepository.save(submitDraft);
    return StockManagementDraftDto.from(updatedDraft);
  }

  public List<MergedLineItemDto> mergeSubDrafts(UUID initialDraftId) {
    draftValidator.validateInitialDraftId(initialDraftId);
    List<StockManagementDraft> subDrafts = stockManagementDraftRepository
        .findByInitialDraftId(initialDraftId);

    if (subDrafts.isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY),
          "subDrafts empty");
    }
    boolean isAllSubmitted = subDrafts.stream()
        .allMatch(subDraft -> subDraft.getStatus().equals(PhysicalInventorySubDraftEnum.SUBMITTED));
    if (isAllSubmitted) {
      List<MergedLineItemDto> mergedLineItemDtos = fillingMergedLineItemsFields(subDrafts);
      mergedLineItemDtos.forEach(this::fillingStockOnHandField);
      return mergedLineItemDtos;
    }
    throw new BusinessDataException(new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_NOT_ALL_SUBMITTED),
        "subDrafts not all submitted");
  }

  private void fillingStockOnHandField(MergedLineItemDto mergedLineItemDto) {
    StockCardDto stockCardByOrderable = stockCardService
        .findStockCardByOrderable(mergedLineItemDto.getOrderableId());
    mergedLineItemDto.setStockOnHand(stockCardByOrderable == null ? null : stockCardByOrderable.getStockOnHand());
  }

  private List<MergedLineItemDto> fillingMergedLineItemsFields(
      List<StockManagementDraft> subDrafts) {
    List<MergedLineItemDto> mergedLineItemDtos = new ArrayList<>();
    subDrafts.forEach(subDraft -> {
      List<MergedLineItemDto> subDraftLineItemDto = subDraft.getLineItems().stream()
          .map(lineItem -> {
            return MergedLineItemDto.builder().subDraftId(subDraft.getId())
                .productName(lineItem.getProductName())
                .productCode(lineItem.getProductCode())
                .expirationDate(lineItem.getExpirationDate())
                .lotId(lineItem.getLotId())
                .orderableId(lineItem.getOrderableId())
                .occurredDate(lineItem.getOccurredDate())
                .quantity(lineItem.getQuantity())
                .lotCode(lineItem.getLotCode())
                .build();
          }).collect(toList());
      mergedLineItemDtos.addAll(subDraftLineItemDto);
    });
    return mergedLineItemDtos;
  }

  @Transactional
  public void deleteInitialDraft(UUID initialDraftId) {
    draftValidator.validateInitialDraftId(initialDraftId);
    log.info("delete subDrafts with initial draft with id: {}", initialDraftId);
    stockManagementDraftRepository.deleteAllByInitialDraftId(initialDraftId);
    log.info("delete initial draft with id: {}", initialDraftId);
    stockManagementInitialDraftsRepository.delete(initialDraftId);
  }
}
