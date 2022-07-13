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
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_EVENT_ORDERABLE_INVALID;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_PROGRAM_NOT_SUPPORTED;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ISSUE_CONFLICT_SUB_DRAFT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_MORE_THAN_TEN;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_INITIAL_DRAFT_EXISTS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.service.PermissionService;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.domain.StockManagementInitialDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProductSubDraftConflictDto;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.repository.StockManagementInitialDraftsRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
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
  private PermissionService permissionService;

  @Autowired
  private SiglusValidSourceDestinationService validSourceDestinationService;

  @Autowired
  private SiglusOrderableReferenceDataService siglusOrderableReferenceDataService;

  @Autowired
  private SupportedProgramsHelper supportedProgramsHelper;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

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
  public StockManagementDraftDto createNewIssueDraft(StockManagementDraftDto dto) {
    log.info("create physical inventory draft");
    stockManagementDraftValidator.validateEmptyDraft(dto);
    draftValidator.validateInitialDraftId(dto.getInitialDraftId());
    draftValidator.validateDraftType(dto.getDraftType());

    checkIfSameDraftsOversize(dto);

    StockManagementDraft draft = StockManagementDraft.createEmptyIssueDraft(dto);
    UUID initialDraftId = draft.getInitialDraftId();
    List<StockManagementDraft> subDrafts = stockManagementDraftRepository
        .findByInitialDraftId(initialDraftId);
    draft.setDraftNumber(subDrafts.size() + DRAFTS_INCREMENT);
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);

    return StockManagementDraftDto.from(savedDraft);
  }

  @Transactional
  public StockManagementDraftDto updateDraft(StockManagementDraftDto dto, UUID id) {
    log.info("update issue draft");
    stockManagementDraftValidator.validateDraft(dto, id);
    checkConflictSubDraft(dto);
    StockManagementDraft newDraft = setNewAttributesInOriginalDraft(dto, id);
    StockManagementDraft savedDraft = stockManagementDraftRepository.save(newDraft);
    return StockManagementDraftDto.from(savedDraft);
  }

  private void checkConflictSubDraft(StockManagementDraftDto dto) {
    StockManagementDraft currentSubDraft = stockManagementDraftRepository.findOne(dto.getId());
    List<StockManagementDraft> subDrafts = stockManagementDraftRepository
        .findByInitialDraftId(currentSubDraft.getInitialDraftId());

    List<UUID> preSavedDraftOrderableIds = dto.getLineItems().stream()
        .map(StockManagementDraftLineItemDto::getOrderableId)
        .collect(toList());

    subDrafts.remove(currentSubDraft);

    List<ProductSubDraftConflictDto> subDraftConflictDtos = new ArrayList<>();
    subDrafts.forEach(subDraft -> {
      List<UUID> conflictOrderableIds = subDraft.getLineItems().stream()
          .map(StockManagementDraftLineItem::getOrderableId)
          .filter(preSavedDraftOrderableIds::contains).collect(toList());
      if (!conflictOrderableIds.isEmpty()) {
        conflictOrderableIds.forEach(id -> {
          ProductSubDraftConflictDto subDraftConflictDto = ProductSubDraftConflictDto.builder()
              .conflictWithSubDraftId(subDraft.getId())
              .orderableId(id)
              .conflictWith(subDraft.getDraftNumber().toString())
              .build();
          subDraftConflictDtos.add(subDraftConflictDto);
        });
      }
    });

    fillConflictDtos(subDraftConflictDtos);

    if (!subDraftConflictDtos.isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_ISSUE_CONFLICT_SUB_DRAFT),
          subDraftConflictDtos);
    }
  }

  private void fillConflictDtos(List<ProductSubDraftConflictDto> subDraftConflictDtos) {
    List<OrderableDto> orderableDtos = siglusOrderableReferenceDataService.findByIds(
        subDraftConflictDtos.stream().map(ProductSubDraftConflictDto::getOrderableId)
            .collect(toList()));
    subDraftConflictDtos.forEach(subDraftConflictDto -> {
      OrderableDto targetOrderableDto = orderableDtos.stream()
          .filter(orderableDto -> orderableDto.getId().equals(subDraftConflictDto.getOrderableId()))
          .findFirst()
          .orElseThrow(() -> new ResourceNotFoundException(ERROR_EVENT_ORDERABLE_INVALID));
      subDraftConflictDto.setProductCode(targetOrderableDto.getProductCode());
      subDraftConflictDto.setProductName(targetOrderableDto.getFullProductName());
    });
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

  //Delete after finish multi-user stock issue feature
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
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getFacilityId(),
            true,
            dto.getType());
    if (!drafts.isEmpty()) {
      log.info("delete stockmanagement draft, programId: {}, facilityId: {}", dto.getProgramId(),
          dto.getFacilityId());
      stockManagementDraftRepository.delete(drafts);
    }
  }

  @Transactional
  public void deleteStockManagementDraft(UUID id) {
    StockManagementDraft draft = stockManagementDraftRepository.findOne(id);
    if (draft != null) {
      UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
      checkPermission(facilityId);
      log.info("delete stockmanagement draft: {}", id);
      stockManagementDraftRepository.delete(draft);
      resetDraftNumber(draft);
    } else {
      throw new ResourceNotFoundException(
          new org.openlmis.stockmanagement.util.Message(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND,
              id));
    }
  }

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

  private void checkPermission(UUID facility) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    if (CollectionUtils.isEmpty(supportedPrograms)) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED,
              ALL_PRODUCTS_PROGRAM_ID));
    }
    supportedPrograms.forEach(i -> permissionService.canAdjustStock(i, facility));
  }

  //Delete after finish multi-user stock issue feature
  private void checkIfDraftExists(StockManagementDraftDto dto) {
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(dto.getProgramId(), dto.getFacilityId(),
            true,
            dto.getDraftType());
    if (!drafts.isEmpty()) {
      throw new ValidationMessageException(
          new Message(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS, dto.getProgramId(),
              dto.getFacilityId()));
    }
  }

  //Same draft means: same facilityid, programid, destinationid and drafttype
  private void checkIfSameDraftsOversize(StockManagementDraftDto dto) {
    List<StockManagementDraft> sameDrafts = stockManagementDraftRepository
        .findByInitialDraftId(dto.getInitialDraftId());
    if (sameDrafts.size() > DRAFTS_LIMITATION - 1) {
      throw new BusinessDataException(
          new Message(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_MORE_THAN_TEN, dto.getProgramId(),
              dto.getFacilityId()), "same drafts more than limitation");
    }
  }

  private String findDestinationName(UUID destinationId, UUID facilityId) {
    Collection<ValidSourceDestinationDto> destinationsForAllProducts = validSourceDestinationService
        .findDestinationsForAllProducts(facilityId);

    return destinationsForAllProducts
        .stream().filter(destination -> (
            destination.getId().equals(destinationId)
        )).findFirst()
        .orElseThrow(() -> new NotFoundException("No such destination with id: " + destinationId))
        .getName();
  }

  public StockManagementInitialDraftDto createInitialDraft(
      StockManagementInitialDraftDto initialDraftDto) {
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

    String destinationName = findDestinationName(savedInitialDraft.getDestinationId(),
        savedInitialDraft.getFacilityId());

    StockManagementInitialDraftDto initialDraftDtoResponse = StockManagementInitialDraftDto
        .from(savedInitialDraft);

    initialDraftDtoResponse.setDestinationName(destinationName);

    return initialDraftDtoResponse;
  }

  public StockManagementInitialDraftDto findStockManagementInitialDraft(
      UUID programId, String draftType) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    draftValidator.validateProgramId(programId);
    draftValidator.validateFacilityId(facilityId);
    draftValidator.validateDraftType(draftType);

    List<StockManagementInitialDraft> initialDrafts = stockManagementInitialDraftsRepository
        .findByProgramIdAndFacilityIdAndDraftType(programId, facilityId, draftType);
    StockManagementInitialDraft initialDraft = initialDrafts.stream().findFirst().orElse(null);
    if (initialDraft != null) {
      StockManagementInitialDraftDto stockManagementInitialDraftDto = StockManagementInitialDraftDto
          .from(initialDraft);
      if (draftType.equals(FieldConstants.ISSUE)) {
        String destinationName = findDestinationName(initialDraft.getDestinationId(), facilityId);
        stockManagementInitialDraftDto.setDestinationName(destinationName);
        return stockManagementInitialDraftDto;
      } else if (draftType.equals(FieldConstants.RECEIVE)) {
        String sourceName = findSourceName(initialDraft.getSourceId());
        stockManagementInitialDraftDto.setSourceName(sourceName);
        return stockManagementInitialDraftDto;
      }
    }
    return new StockManagementInitialDraftDto();
  }

  private String findSourceName(UUID sourceId) {
    //TODO: when do multi-user receive, get source name by id
    return null;
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

  public StockManagementDraftDto updateOperatorAndStatus(StockManagementDraftDto draftDto) {
    StockManagementDraft draft = stockManagementDraftRepository.findOne(draftDto.getId());

    if (draft != null) {
      draft.setStatus(PhysicalInventorySubDraftEnum.DRAFT);
      draft.setOperator(draftDto.getOperator());
      StockManagementDraft savedDraft = stockManagementDraftRepository.save(draft);
      return StockManagementDraftDto.from(savedDraft);
    }
    throw new NotFoundException(ERROR_STOCK_MANAGEMENT_DRAFT_NOT_FOUND);
  }

  public StockManagementDraftDto searchDraft(UUID id) {
    StockManagementDraft draft = stockManagementDraftRepository.findOne(id);
    if (draft != null) {
      return StockManagementDraftDto.from(draft);
    }
    throw new ResourceNotFoundException(
        new org.openlmis.stockmanagement.util.Message(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND,
            id));
  }

  public StockManagementDraftDto updatePartOfInfoWithDraft(StockManagementDraftDto dto) {
    StockManagementDraft targetDraft = stockManagementDraftRepository.findOne(dto.getId());
    if (targetDraft != null) {
      StockManagementDraft currentDraft = new StockManagementDraft();
      BeanUtils.copyProperties(targetDraft, currentDraft);
      currentDraft.setStatus(PhysicalInventorySubDraftEnum.NOT_YET_STARTED);
      currentDraft.setOperator(null);
      currentDraft.setLineItems(Collections.emptyList());
      StockManagementDraft savedDraft = stockManagementDraftRepository.save(currentDraft);
      return StockManagementDraftDto.from(savedDraft);
    }
    throw new ResourceNotFoundException(
        new org.openlmis.stockmanagement.util.Message(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND,
            dto.getId()));
  }
}
