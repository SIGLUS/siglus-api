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
import lombok.RequiredArgsConstructor;
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
import org.siglus.siglusapi.dto.MergedLineItemWithLocationDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemWithLocationDto;
import org.siglus.siglusapi.dto.StockManagementDraftWithLocationDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusStockManagementDraftService {

  private final ActiveDraftValidator draftValidator;
  private final StockManagementDraftRepository stockManagementDraftRepository;
  private final StockManagementInitialDraftsRepository stockManagementInitialDraftsRepository;
  private final StockManagementDraftValidator stockManagementDraftValidator;
  private final SiglusStockCardService stockCardService;
  private final SiglusValidSourceDestinationService validSourceDestinationService;
  private final OperatePermissionService operatePermissionService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final ConflictOrderableInSubDraftsService conflictOrderableInSubDraftsService;
  private final SiglusLotLocationService lotLocationService;

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
  public StockManagementDraftWithLocationDto updateDraftWithLocation(StockManagementDraftWithLocationDto subDraftDto,
      UUID id) {
    stockManagementDraftValidator.validateDraft(StockManagementDraftDto.from(subDraftDto), id);
    if (FieldConstants.ISSUE.equals(subDraftDto.getDraftType())
        || FieldConstants.RECEIVE.equals(subDraftDto.getDraftType())
        || FieldConstants.ISSUE_WITH_LOCATION.equals(subDraftDto.getDraftType())
        || FieldConstants.RECEIVE_WITH_LOCATION.equals(subDraftDto.getDraftType())) {
      StockManagementDraft subDraft = stockManagementDraftRepository.findOne(id);
      draftValidator.validateSubDraftStatus(subDraft);
      conflictOrderableInSubDraftsService.checkConflictOrderableBetweenSubDrafts(StockManagementDraftDto
          .from(subDraftDto));
      StockManagementDraft newDraft = setNewAttributesInOriginalDraftWithLocation(subDraftDto, id);
      log.info("save issue or receive stockManagementDraft with location, id : {}", newDraft.getId());
      StockManagementDraft savedDraft = stockManagementDraftRepository.save(newDraft);
      return StockManagementDraftWithLocationDto.from(savedDraft);
    }
    StockManagementDraft draft = StockManagementDraft.createStockManagementDraftWithLocation(subDraftDto, true);
    log.info("save other stockManagementDraft with location, id : {}", draft.getId());
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);
    return StockManagementDraftWithLocationDto.from(savedDraft);
  }

  @Transactional
  public StockManagementDraftDto updateDraft(StockManagementDraftDto subDraftDto, UUID id) {
    stockManagementDraftValidator.validateDraft(subDraftDto, id);
    if (subDraftDto.getDraftType().equals(FieldConstants.ISSUE)
        || subDraftDto.getDraftType().equals(FieldConstants.RECEIVE)) {
      StockManagementDraft subDraft = stockManagementDraftRepository.findOne(id);
      draftValidator.validateSubDraftStatus(subDraft);
      conflictOrderableInSubDraftsService.checkConflictOrderableBetweenSubDrafts(subDraftDto);
      StockManagementDraft newDraft = setNewAttributesInOriginalDraft(subDraftDto, id);
      log.info("save issue or receive stockManagementDraft, id : {}", newDraft.getId());
      StockManagementDraft savedDraft = stockManagementDraftRepository.save(newDraft);
      return StockManagementDraftDto.from(savedDraft);
    }
    StockManagementDraft draft = StockManagementDraft.createStockManagementDraft(subDraftDto, true);
    log.info("save other stockManagementDraft with location, id : {}", draft.getId());
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);
    return StockManagementDraftDto.from(savedDraft);
  }

  public StockManagementDraft setNewAttributesInOriginalDraft(StockManagementDraftDto dto, UUID id) {
    StockManagementDraft newDraft = copyAndUpdateStockManagementDraft(id, dto.getSignature());
    List<StockManagementDraftLineItemDto> lineItemDtos = dto.getLineItems();
    if (lineItemDtos != null) {
      newDraft.setLineItems(lineItemDtos.stream()
          .map(lineItemDto -> StockManagementDraftLineItem.from(lineItemDto, newDraft))
          .collect(toList()));
    }
    return newDraft;
  }

  public StockManagementDraft setNewAttributesInOriginalDraftWithLocation(StockManagementDraftWithLocationDto dto,
      UUID id) {
    StockManagementDraft newDraft = copyAndUpdateStockManagementDraft(id, dto.getSignature());
    List<StockManagementDraftLineItemWithLocationDto> lineItemDtos = dto.getLineItems();
    if (lineItemDtos != null) {
      newDraft.setLineItems(lineItemDtos.stream()
          .map(lineItemDto -> StockManagementDraftLineItem.from(lineItemDto, newDraft))
          .collect(toList()));
    }
    return newDraft;
  }

  public List<StockManagementDraftWithLocationDto> findStockManagementDraftWithLocation(UUID programId, String type,
      Boolean isDraft) {
    List<StockManagementDraft> drafts = getStockManagementDrafts(programId, type, isDraft);
    return StockManagementDraftWithLocationDto.from(drafts);
  }

  public List<StockManagementDraftDto> findStockManagementDraft(UUID programId, String type, Boolean isDraft) {
    List<StockManagementDraft> drafts = getStockManagementDrafts(programId, type, isDraft);
    return StockManagementDraftDto.from(drafts);
  }

  public List<StockManagementDraftDto> findStockManagementDrafts(UUID initialDraftId) {
    List<StockManagementDraft> sortedDrafts = getStockManagementDrafts(initialDraftId);
    return StockManagementDraftDto.from(sortedDrafts);
  }

  private List<StockManagementDraft> getStockManagementDrafts(UUID initialDraftId) {
    draftValidator.validateInitialDraftId(initialDraftId);
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByInitialDraftId(initialDraftId);
    return drafts.stream()
        .sorted(Comparator.comparingInt(StockManagementDraft::getDraftNumber))
        .collect(toList());
  }

  private List<StockManagementDraft> getStockManagementDrafts(UUID programId, String type, Boolean isDraft) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    draftValidator.validateProgramId(programId);
    draftValidator.validateFacilityId(facilityId);
    draftValidator.validateDraftType(type);
    draftValidator.validateIsDraft(isDraft);
    return stockManagementDraftRepository
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId, facilityId, isDraft, type);
  }

  @Transactional
  public void deleteStockManagementDraft(StockEventDto dto) {
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getFacilityId(), true,
            dto.getType());
    if (drafts.isEmpty()) {
      return;
    }
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

  @Transactional
  public void deleteStockManagementDraft(UUID id) {
    StockManagementDraft subDraft = getAndValidateStockManagementDraft(id);
    draftValidator.validateSubDraftStatus(subDraft);
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    operatePermissionService.checkPermission(facilityId);
    log.info("delete stockmanagement draft: {}", id);
    stockManagementDraftRepository.delete(subDraft);
    if (!FieldConstants.ADJUSTMENT.equals(subDraft.getDraftType())) {
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
      filterSubDrafts.forEach(subDraft -> subDraft.setDraftNumber(subDraft.getDraftNumber() - DRAFTS_INCREMENT));
      stockManagementDraftRepository.save(filterSubDrafts);
    }
  }

  private StockManagementDraft copyAndUpdateStockManagementDraft(UUID id, String signature) {
    StockManagementDraft originalDraft = stockManagementDraftRepository.findOne(id);
    StockManagementDraft newDraft = new StockManagementDraft();
    BeanUtils.copyProperties(originalDraft, newDraft);
    newDraft.setSignature(signature);
    newDraft.setStatus(PhysicalInventorySubDraftEnum.DRAFT);
    newDraft.setOperator(authenticationHelper.getCurrentUser().getUsername());
    return newDraft;
  }

  private void checkIfDraftExists(StockManagementDraftDto dto) {
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getFacilityId(), true,
            dto.getDraftType());
    if (CollectionUtils.isNotEmpty(drafts)) {
      throw new ValidationMessageException(new Message(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS, dto.getProgramId(),
          dto.getFacilityId()));
    }
  }

  private void checkIfSameDraftsOversize(StockManagementDraftDto dto) {
    int subDraftsQuantity = stockManagementDraftRepository.countByInitialDraftId(dto.getInitialDraftId());
    if (subDraftsQuantity > DRAFTS_LIMITATION - 1) {
      throw new BusinessDataException(new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_MORE_THAN_TEN, dto.getProgramId(),
          dto.getFacilityId()), "subDrafts are more than limitation");
    }
  }

  private String findDestinationName(UUID destinationNodeId, UUID facilityId) {
    Collection<ValidSourceDestinationDto> destinationsForAllProducts = validSourceDestinationService
        .findDestinationsForAllPrograms(facilityId);

    return destinationsForAllProducts
        .stream().filter(destination -> (destination.getNode().getId().equals(destinationNodeId)))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("No such destination node with id: " + destinationNodeId))
        .getName();
  }

  @Transactional
  public StockManagementInitialDraftDto createInitialDraft(StockManagementInitialDraftDto initialDraftDto) {
    operatePermissionService.checkPermission(initialDraftDto.getFacilityId());
    stockManagementDraftValidator.validateInitialDraft(initialDraftDto);

    checkIfInitialDraftExists(initialDraftDto.getProgramId(), initialDraftDto.getFacilityId(),
        initialDraftDto.getDraftType());

    StockManagementInitialDraft initialDraft = StockManagementInitialDraft
        .createInitialDraft(initialDraftDto);

    log.info("create stock management initial draft with facility id: {} and program id: {}",
        initialDraft.getFacilityId(), initialDraft.getProgramId());
    StockManagementInitialDraft savedInitialDraft = stockManagementInitialDraftsRepository.save(initialDraft);
    StockManagementInitialDraftDto initialDraftDtoResponse = StockManagementInitialDraftDto.from(savedInitialDraft);
    boolean canMergeOrDeleteSubDrafts = authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts();
    initialDraftDtoResponse.setCanMergeOrDeleteSubDrafts(canMergeOrDeleteSubDrafts);
    String draftType = initialDraftDto.getDraftType();
    if (FieldConstants.ISSUE.equals(draftType) || FieldConstants.ISSUE_WITH_LOCATION.equals(draftType)) {
      if (savedInitialDraft.getDestinationId() != null) {
        String destinationName = findDestinationName(savedInitialDraft.getDestinationId(),
                savedInitialDraft.getFacilityId());
        initialDraftDtoResponse.setDestinationName(destinationName);
      }
      return initialDraftDtoResponse;
    } else if (FieldConstants.RECEIVE.equals(draftType) || FieldConstants.RECEIVE_WITH_LOCATION.equals(draftType)) {
      String sourceName = findSourceName(savedInitialDraft.getSourceId(), savedInitialDraft.getFacilityId());
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
    boolean canMergeOrDeleteSubDrafts = authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts();
    List<StockManagementInitialDraft> initialDrafts = stockManagementInitialDraftsRepository
        .findByProgramIdAndFacilityIdAndDraftType(programId, facilityId, draftType);
    StockManagementInitialDraft initialDraft = initialDrafts.stream().findFirst().orElse(null);
    if (initialDraft != null) {
      StockManagementInitialDraftDto stockManagementInitialDraftDto = StockManagementInitialDraftDto.from(initialDraft);
      if (draftType.equals(FieldConstants.ISSUE) || draftType.equals(FieldConstants.ISSUE_WITH_LOCATION)) {
        if (initialDraft.getDestinationId() != null) {
          String destinationName = findDestinationName(initialDraft.getDestinationId(), facilityId);
          stockManagementInitialDraftDto.setDestinationName(destinationName);
        }
      } else if (draftType.equals(FieldConstants.RECEIVE) || draftType.equals(FieldConstants.RECEIVE_WITH_LOCATION)) {
        String sourceName = findSourceName(initialDraft.getSourceId(), initialDraft.getFacilityId());
        stockManagementInitialDraftDto.setSourceName(sourceName);
      }
      stockManagementInitialDraftDto.setCanMergeOrDeleteSubDrafts(canMergeOrDeleteSubDrafts);
      return stockManagementInitialDraftDto;
    }
    return new StockManagementInitialDraftDto();
  }

  private String findSourceName(UUID sourceNodeId, UUID facilityId) {
    Collection<ValidSourceDestinationDto> sourcesForAllProducts = validSourceDestinationService
        .findSourcesForAllPrograms(facilityId);

    return sourcesForAllProducts.stream().filter(source -> (source.getNode().getId().equals(sourceNodeId))).findFirst()
        .orElseThrow(() -> new NotFoundException("No such source node with id: " + sourceNodeId)).getName();
  }

  private void checkIfInitialDraftExists(UUID programId, UUID facilityId, String draftType) {
    List<StockManagementInitialDraft> initialDrafts = stockManagementInitialDraftsRepository
        .findByProgramIdAndFacilityIdAndDraftType(programId, facilityId, draftType);
    if (!initialDrafts.isEmpty()) {
      throw new BusinessDataException(
          new Message(ERROR_STOCK_MANAGEMENT_INITIAL_DRAFT_EXISTS, programId, facilityId, draftType),
          "same initial draft exists");
    }
  }

  @Transactional
  public StockManagementDraftDto updateOperatorAndStatus(StockManagementDraftDto draftDto) {
    StockManagementDraft subDraft = getAndValidateStockManagementDraft(draftDto.getId());
    draftValidator.validateSubDraftStatus(subDraft);
    subDraft.setStatus(PhysicalInventorySubDraftEnum.DRAFT);
    subDraft.setOperator(draftDto.getOperator());
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(subDraft);
    return StockManagementDraftDto.from(savedDraft);
  }

  public StockManagementDraftDto searchDraft(UUID id) {
    StockManagementDraft subDraft = getAndValidateStockManagementDraft(id);
    return StockManagementDraftDto.from(subDraft);
  }

  public StockManagementDraftWithLocationDto searchDraftWithLocation(UUID id) {
    StockManagementDraft subDraft = getAndValidateStockManagementDraft(id);
    return StockManagementDraftWithLocationDto.from(subDraft);
  }

  @Transactional
  public StockManagementDraftDto restoreSubDraftWhenDoDelete(StockManagementDraftDto dto) {
    StockManagementDraft subDraft = getAndValidateStockManagementDraft(dto.getId());
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
  public StockManagementDraftWithLocationDto updateStatusAfterSubmitWithLocation(
      StockManagementDraftWithLocationDto subDraftDto) {
    StockManagementDraft subDraft = getAndValidateStockManagementDraft(subDraftDto.getId());
    conflictOrderableInSubDraftsService.checkConflictOrderableAndLotInSubDraft(subDraftDto);
    draftValidator.validateSubDraftStatus(subDraft);
    conflictOrderableInSubDraftsService.checkConflictOrderableBetweenSubDrafts(subDraftDto);
    subDraft.setStatus(PhysicalInventorySubDraftEnum.SUBMITTED);
    subDraft.setOperator(authenticationHelper.getCurrentUser().getUsername());
    subDraft.setSignature(subDraftDto.getSignature());
    List<StockManagementDraftLineItem> draftLineItems = subDraftDto.getLineItems().stream()
        .map(lineItemDto -> StockManagementDraftLineItem.from(lineItemDto, subDraft))
        .collect(toList());
    StockManagementDraft submitDraft = new StockManagementDraft();
    BeanUtils.copyProperties(subDraft, submitDraft);
    submitDraft.setLineItems(draftLineItems);

    StockManagementDraft updatedDraft = stockManagementDraftRepository.save(submitDraft);
    return StockManagementDraftWithLocationDto.from(updatedDraft);
  }

  @Transactional
  public StockManagementDraftDto updateStatusAfterSubmit(StockManagementDraftDto subDraftDto) {
    StockManagementDraft subDraft = getAndValidateStockManagementDraft(subDraftDto.getId());
    conflictOrderableInSubDraftsService.checkConflictOrderableAndLotInSubDraft(subDraftDto);
    draftValidator.validateSubDraftStatus(subDraft);
    conflictOrderableInSubDraftsService.checkConflictOrderableBetweenSubDrafts(subDraftDto);
    subDraft.setStatus(PhysicalInventorySubDraftEnum.SUBMITTED);
    subDraft.setOperator(authenticationHelper.getCurrentUser().getUsername());
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

  public List<MergedLineItemWithLocationDto> mergeSubDraftsWithLocation(UUID initialDraftId) {
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
      return fillingMergedLineItemsFieldsWithLocation(subDrafts);
    }
    throw new BusinessDataException(new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_NOT_ALL_SUBMITTED),
        "subDrafts not all submitted");
  }

  private StockManagementDraft getAndValidateStockManagementDraft(UUID id) {
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(id);
    draftValidator.validateSubDraft(subDraft);
    return subDraft;
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
          .map(lineItem -> MergedLineItemDto.builder().subDraftId(subDraft.getId())
              .productName(lineItem.getProductName())
              .productCode(lineItem.getProductCode())
              .expirationDate(lineItem.getExpirationDate())
              .lotId(lineItem.getLotId())
              .orderableId(lineItem.getOrderableId())
              .occurredDate(lineItem.getOccurredDate())
              .quantity(lineItem.getQuantity())
              .lotCode(lineItem.getLotCode())
              .sourceId(lineItem.getSourceId())
              .destinationId(lineItem.getDestinationId())
              .documentNumber(lineItem.getDocumentNumber())
              .build()).collect(toList());
      mergedLineItemDtos.addAll(subDraftLineItemDto);
    });
    return mergedLineItemDtos;
  }

  private List<MergedLineItemWithLocationDto> fillingMergedLineItemsFieldsWithLocation(
      List<StockManagementDraft> subDrafts) {
    List<MergedLineItemWithLocationDto> mergedLineItemWithLocationDtos = new ArrayList<>();
    subDrafts.forEach(subDraft -> {
      List<MergedLineItemWithLocationDto> subDraftLineItemDto = subDraft.getLineItems().stream()
          .map(lineItem -> MergedLineItemWithLocationDto.builder().subDraftId(subDraft.getId())
              .productName(lineItem.getProductName())
              .productCode(lineItem.getProductCode())
              .expirationDate(lineItem.getExpirationDate())
              .lotId(lineItem.getLotId())
              .orderableId(lineItem.getOrderableId())
              .occurredDate(lineItem.getOccurredDate())
              .quantity(lineItem.getQuantity())
              .lotCode(lineItem.getLotCode())
              .stockOnHand(lineItem.getStockOnHand())
              .locationCode(lineItem.getLocationCode())
              .area(lineItem.getArea())
              .build()).collect(toList());
      mergedLineItemWithLocationDtos.addAll(subDraftLineItemDto);
    });
    return mergedLineItemWithLocationDtos;
  }

  @Transactional
  public void deleteInitialDraft(UUID initialDraftId) {
    draftValidator.validateInitialDraftId(initialDraftId);
    log.info("delete subDrafts with initial draft with id: {}", initialDraftId);
    stockManagementDraftRepository.deleteAllByInitialDraftId(initialDraftId);
    log.info("delete initial draft with id: {}", initialDraftId);
    stockManagementInitialDraftsRepository.delete(initialDraftId);
  }

  public StockManagementDraftWithLocationDto createEmptyStockManagementDraftWithLocation(
      StockManagementDraftWithLocationDto dto) {
    StockManagementDraftDto draftDto = StockManagementDraftDto.from(dto);
    StockManagementDraftDto subDraftDto = createNewSubDraft(draftDto);
    return StockManagementDraftWithLocationDto.from(subDraftDto);
  }

  public List<StockManagementDraftWithLocationDto> findSubDraftsWithLocation(UUID initialDraftId) {
    List<StockManagementDraft> sortedDrafts = getStockManagementDrafts(initialDraftId);
    return StockManagementDraftWithLocationDto.from(sortedDrafts);
  }
}
