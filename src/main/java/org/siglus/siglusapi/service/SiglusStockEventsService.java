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

import static org.siglus.siglusapi.constant.FieldConstants.ADJUSTMENT;
import static org.siglus.siglusapi.constant.FieldConstants.ISSUE;
import static org.siglus.siglusapi.constant.FieldConstants.RECEIVE;
import static org.siglus.siglusapi.constant.FieldConstants.SEPARATOR;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ADJUSTMENT_LOCATION_IS_RESTRICTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_QUANTITY_NOT_MATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.openlmis.stockmanagement.domain.BaseEntity;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.repository.StockEventsRepository;
import org.openlmis.stockmanagement.service.StockEventProcessor;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.StockEventForMultiUserDto;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods"})
public class SiglusStockEventsService {

  private final SiglusPhysicalInventoryService siglusPhysicalInventoryService;
  private final SiglusStockManagementDraftService stockManagementDraftService;
  private final StockCardRepository stockCardRepository;
  private final StockCardExtensionRepository stockCardExtensionRepository;
  private final StockCardLineItemRepository stockCardLineItemRepository;
  private final StockEventsRepository stockEventsRepository;
  private final StockEventProcessor stockEventProcessor;
  private final SiglusArchiveProductService archiveProductService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final StockManagementDraftRepository stockManagementDraftRepository;
  private final ActiveDraftValidator draftValidator;
  private final StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;
  private final SiglusLotService siglusLotService;
  private final FacilityLocationsRepository facilityLocationsRepository;
  private final StockCardLineItemReasonRepository stockCardLineItemReasonRepository;
  private final CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  private final CalculatedStocksOnHandByLocationService calculatedStocksOnHandByLocationService;
  @Value("${stockmanagement.kit.unpack.destination.nodeId}")
  private UUID unpackKitDestinationNodeId;

  @Transactional
  public void processStockEventForMultiUser(StockEventForMultiUserDto stockEventForMultiUserDto, boolean isByLocation) {
    List<UUID> subDraftIds = stockEventForMultiUserDto.getSubDrafts();
    validatePreSubmitSubDraft(subDraftIds);
    processStockEvent(stockEventForMultiUserDto.getStockEvent(), isByLocation);
  }

  @Transactional
  public void processStockEvent(StockEventDto eventDto, boolean isByLocation) {
    setUserId(eventDto);
    siglusLotService.createAndFillLotId(eventDto);
    Set<UUID> programIds = getProgramIds(eventDto);
    List<StockEventDto> stockEventDtoByPrograms;
    if (eventDto.isPhysicalInventory()) {
      stockEventDtoByPrograms = getStockEventsWhenDoPhysicalInventory(eventDto, programIds);
    } else {
      stockEventDtoByPrograms = getStockEventsWhenDoStockMovements(eventDto, programIds);
    }
    if (eventDto.isAdjustment() && isByLocation) {
      validateAdjustmentLocationAndQuantity(eventDto);
    }
    createStockEvent(eventDto, stockEventDtoByPrograms, isByLocation);
    deleteDraft(eventDto);

    if (isByLocation) {
      calculatedStocksOnHandByLocationService.calculateStockOnHandByLocation(eventDto);
    }
  }

  private Set<UUID> getProgramIds(StockEventDto eventDto) {
    if (!isAllProgram(eventDto)) {
      eventDto.getLineItems().forEach(item -> item.setProgramId(eventDto.getProgramId()));
    }
    return eventDto.getLineItems().stream()
        .map(StockEventLineItemDto::getProgramId)
        .collect(Collectors.toSet());
  }

  private void deleteDraft(StockEventDto eventDto) {
    if (eventDto.isPhysicalInventory()) {
      if (isAllProgram(eventDto)) {
        siglusPhysicalInventoryService.deletePhysicalInventoryDraftForAllPrograms(eventDto.getFacilityId());
      } else {
        siglusPhysicalInventoryService.deletePhysicalInventoryDraftForOneProgram(eventDto.getFacilityId(),
            eventDto.getProgramId());
      }
    } else if (isNotUnpackKit(eventDto)) {
      eventDto.setType(getDraftType(eventDto));
      stockManagementDraftService.deleteStockManagementDraft(eventDto);
    }
  }

  private List<StockEventDto> getStockEventsWhenDoPhysicalInventory(StockEventDto eventDto, Set<UUID> programIds) {
    List<PhysicalInventoryDto> inventories = programIds.stream()
        .map(programId -> siglusPhysicalInventoryService
            .getPhysicalInventoryDtosDirectly(programId, eventDto.getFacilityId(), Boolean.TRUE))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(inventories)) {
      throw new ValidationMessageException("stockmanagement.error.physicalInventory.isSubmitted");
    }
    return inventories.stream().map(StockEventDto::fromPhysicalInventoryDto).collect(Collectors.toList());
  }

  private List<StockEventDto> getStockEventsWhenDoStockMovements(StockEventDto eventDto, Set<UUID> programIds) {
    if (isNotUnpackKit(eventDto) && isAllProgram(eventDto)) {
      List<StockManagementDraftDto> stockManagementDraftDtos = stockManagementDraftService
          .findStockManagementDraft(ALL_PRODUCTS_PROGRAM_ID, getDraftType(eventDto), true);
      if (CollectionUtils.isEmpty(stockManagementDraftDtos)) {
        throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED);
      }
    }
    return programIds.stream().map(StockEventDto::fromProgramId).collect(Collectors.toList());
  }

  private boolean isAllProgram(StockEventDto eventDto) {
    return ALL_PRODUCTS_PROGRAM_ID.equals(eventDto.getProgramId());
  }

  private void createStockEvent(StockEventDto eventDto, List<StockEventDto> stockEventDtoByPrograms,
      boolean isByLocation) {
    stockEventDtoByPrograms.forEach(stockEventDto -> {
      stockEventDto.setFacilityId(eventDto.getFacilityId());
      stockEventDto.setSignature(eventDto.getSignature());
      stockEventDto.setDocumentNumber(eventDto.getDocumentNumber());
      stockEventDto.setUserId(eventDto.getUserId());
      stockEventDto.setType(eventDto.getType());
      stockEventDto.setLineItems(eventDto.getLineItems().stream()
          .filter(lineItem -> lineItem.getProgramId() != null)
          .filter(lineItem -> lineItem.getProgramId().equals(stockEventDto.getProgramId()))
          .collect(Collectors.toList()));
      siglusCreateStockEvent(stockEventDto, isByLocation);
    });
  }

  private void siglusCreateStockEvent(StockEventDto eventDto, boolean isByLocation) {
    List<StockEventLineItemDto> lineItems = eventDto.getLineItems();
    lineItems.forEach(lineItem -> lineItem.setId(UUID.randomUUID()));
    eventDto.setLineItems(lineItems);
    UUID stockEventId = stockEventProcessor.process(eventDto);
    enhanceStockCard(eventDto, stockEventId, isByLocation);
  }

  private void enhanceStockCard(StockEventDto eventDto, UUID stockEventId, boolean isByLocation) {
    if (isByLocation) {
      addStockCardLineItemLocation(eventDto);
    }
    addStockCardCreateTime(eventDto);
    addStockCardLineItemDocumentNumber(eventDto, stockEventId);
    Set<UUID> orderableIds = eventDto.getLineItems().stream()
        .map(StockEventLineItemDto::getOrderableId)
        .collect(Collectors.toSet());
    archiveProductService.activateProducts(eventDto.getFacilityId(), orderableIds);
  }

  private void setUserId(StockEventDto eventDto) {
    if (eventDto.getUserId() == null) {
      eventDto.setUserId(authenticationHelper.getCurrentUser().getId());
    }
  }

  private void addStockCardLineItemLocation(StockEventDto eventDto) {
    List<StockEventLineItemDto> lineItems = eventDto.getLineItems();
    lineItems.stream().filter(lineItem -> lineItem.getLocationCode() != null)
        .forEach(lineItem -> {
          StockCardLineItemExtension stockCardLineItemExtension = StockCardLineItemExtension
              .builder()
              .locationCode(lineItem.getLocationCode())
              .area(lineItem.getArea())
              .stockCardLineItemId(lineItem.getId())
              .build();
          stockCardLineItemExtensionRepository.save(stockCardLineItemExtension);
        });
  }

  private void addStockCardCreateTime(StockEventDto eventDto) {
    List<StockCard> stockCards = stockCardRepository
        .findByProgramIdAndFacilityId(eventDto.getProgramId(), eventDto.getFacilityId());
    Set<UUID> stockCardIds = stockCards.stream().map(BaseEntity::getId).collect(Collectors.toSet());
    Map<UUID, StockCardExtension> stockCardIdToExtensionMap = Maps.uniqueIndex(
        stockCardExtensionRepository.findByStockCardIdIn(stockCardIds), StockCardExtension::getStockCardId);
    stockCards.forEach(stockCard -> {
      StockCardExtension extension = stockCardIdToExtensionMap.get(stockCard.getId());
      if (extension == null) {
        StockCardExtension stockCardExtension = StockCardExtension.builder()
            .stockCardId(stockCard.getId())
            .createDate(stockCard.getLineItems().get(0).getOccurredDate())
            .build();
        stockCardExtensionRepository.save(stockCardExtension);
      }
    });
  }

  private void addStockCardLineItemDocumentNumber(StockEventDto eventDto, UUID stockEventId) {
    if (eventDto.isPhysicalInventory()) {
      return;
    }
    Map<String, String> keyToDocumentNumber = eventDto.getLineItems()
        .stream()
        .collect(Collectors.toMap(
            stockEventLineItemDto -> stockEventLineItemDto.getOrderableId().toString()
                + stockEventLineItemDto.getLotId()
                + stockEventLineItemDto.getReasonId()
                + stockEventLineItemDto.getSourceId()
                + stockEventLineItemDto.getDestinationId(),
            stockEventLineItemDto -> Optional.ofNullable(stockEventLineItemDto.getDocumentationNo()).orElse(""),
            (v1, v2) -> v1));
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findByOriginEvent(stockEventsRepository.findOne(stockEventId));
    stockCardLineItems.forEach(stockCardLineItem -> {
      if (StringUtils.isNotEmpty(stockCardLineItem.getDocumentNumber())) {
        return;
      }
      UUID orderableId = stockCardLineItem.getStockCard().getOrderableId();
      stockCardLineItem.setDocumentNumber(keyToDocumentNumber.get(orderableId.toString()
          + stockCardLineItem.getStockCard().getLotId()
          + Optional.ofNullable(stockCardLineItem.getReason()).map(StockCardLineItemReason::getId).orElse(null)
          + Optional.ofNullable(stockCardLineItem.getSource()).map(Node::getId).orElse(null)
          + Optional.ofNullable(stockCardLineItem.getDestination()).map(Node::getId).orElse(null)
      ));
    });
    stockCardLineItemRepository.save(stockCardLineItems);
  }

  private boolean isNotUnpackKit(StockEventDto eventDto) {
    return !(eventDto.hasLineItems() && eventDto.getLineItems().stream().anyMatch(lineItem ->
        unpackKitDestinationNodeId.equals(lineItem.getDestinationId())));
  }

  private String getDraftType(StockEventDto eventDto) {
    if (eventDto.hasSource()) {
      return RECEIVE;
    } else if (eventDto.hasDestination()) {
      return ISSUE;
    } else {
      return ADJUSTMENT;
    }
  }

  private void validatePreSubmitSubDraft(List<UUID> subDraftIds) {
    if (subDraftIds.isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY), "subDrafts empty");
    }
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(subDraftIds.get(0));
    draftValidator.validateSubDraft(subDraft);
    int subDraftsQuantity = stockManagementDraftRepository.countByInitialDraftId(subDraft.getInitialDraftId());
    if (subDraftIds.size() != subDraftsQuantity) {
      throw new BusinessDataException(new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_QUANTITY_NOT_MATCH),
          "subDrafts quantity not match");
    }
  }

  private void validateAdjustmentLocationAndQuantity(StockEventDto eventDto) {
    Map<UUID, StockCardLineItemReason> reasonIdToStockCardLineItemReason = Maps.uniqueIndex(
        stockCardLineItemReasonRepository.findAll(), StockCardLineItemReason::getId);
    List<StockEventLineItemDto> positiveAdjustmentLineItem = eventDto.getLineItems().stream()
        .filter(lineItem -> isPositiveAdjustment(reasonIdToStockCardLineItemReason, lineItem.getReasonId()))
        .collect(Collectors.toList());
    List<StockEventLineItemDto> negativeAdjustmentLineItem = eventDto.getLineItems().stream()
        .filter(lineItem -> !isPositiveAdjustment(reasonIdToStockCardLineItemReason, lineItem.getReasonId()))
        .collect(Collectors.toList());
    validatePositiveAdjustmentLocationLimited(eventDto.getFacilityId(), positiveAdjustmentLineItem);
    validateNegativeAdjustmentQuantity(eventDto.getFacilityId(), negativeAdjustmentLineItem);
  }

  private void validateNegativeAdjustmentQuantity(UUID facilityId, List<StockEventLineItemDto> lineItems) {
    Map<String, List<StockEventLineItemDto>> lotLocationToStockEventLineItemDtoList = lineItems.stream()
        .collect(Collectors.groupingBy(e -> getUniqueKey(e.getOrderableId(), e.getLotId(), e.getLocationCode())));

    lotLocationToStockEventLineItemDtoList.forEach((lotLocation, stockEventLineItemDtoList) -> {
      StockEventLineItemDto stockEventLineItemDto = stockEventLineItemDtoList.get(0);
      StockCard stockCard = stockCardRepository.findByProgramIdAndFacilityIdAndOrderableIdAndLotId(
          stockEventLineItemDto.getProgramId(),
          facilityId,
          stockEventLineItemDto.getOrderableId(),
          stockEventLineItemDto.getLotId()
      );
      int soh = 0;
      if (null != stockCard) {
        soh = calculatedStockOnHandByLocationRepository.findRecentlySohByStockCardIdAndLocationCode(
            stockCard.getId(),
            stockEventLineItemDto.getLocationCode()).orElse(0);
      }
      int adjustmentSubValue = stockEventLineItemDtoList.stream().mapToInt(StockEventLineItemDto::getQuantity).sum();
      if (adjustmentSubValue > soh) {
        throw new BusinessDataException(new Message(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND));
      }
    });
  }

  private void validatePositiveAdjustmentLocationLimited(UUID facilityId,
      List<StockEventLineItemDto> lineItems) {
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    lineItems.forEach(e -> {
      if (locations.stream()
          .noneMatch(location -> Objects.equals(location.getLocationCode(), e.getLocationCode()))) {
        throw new BusinessDataException(new Message(ERROR_ADJUSTMENT_LOCATION_IS_RESTRICTED));
      }
    });
  }

  private boolean isPositiveAdjustment(Map<UUID, StockCardLineItemReason> reasonIdToStockCardLineItemReason,
      UUID reasonId) {
    return !reasonIdToStockCardLineItemReason.get(reasonId).getReasonType().equals(ReasonType.DEBIT);
  }

  private String getUniqueKey(UUID orderableId, UUID lotId, String locationCode) {
    if (lotId == null) {
      return orderableId.toString() + SEPARATOR + locationCode;
    }
    return orderableId.toString() + SEPARATOR + lotId + SEPARATOR + locationCode;
  }

}
