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

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.siglus.siglusapi.constant.FieldConstants.ADJUSTMENT;
import static org.siglus.siglusapi.constant.FieldConstants.ISSUE;
import static org.siglus.siglusapi.constant.FieldConstants.RECEIVE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_QUANTITY_NOT_MATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_TRADE_ITEM_IS_EMPTY;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.BaseEntity;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.repository.StockEventsRepository;
import org.openlmis.stockmanagement.service.StockEventProcessor;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotSearchParams;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.StockEventForMultiUserDto;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.StockEventsStockManagementService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
//@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods"})
public class SiglusStockEventsService {

  @Autowired
  private StockEventsStockManagementService stockEventsStockManagementService;
  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;
  @Autowired
  private SiglusLotReferenceDataService lotReferenceDataService;
  @Autowired
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;
  @Autowired
  private SiglusStockManagementDraftService stockManagementDraftService;
  @Autowired
  private StockCardRepository stockCardRepository;
  @Autowired
  private StockCardExtensionRepository stockCardExtensionRepository;
  @Autowired
  private StockCardLineItemRepository stockCardLineItemRepository;
  @Autowired
  private StockEventsRepository stockEventsRepository;
  @Autowired
  private StockEventProcessor stockEventProcessor;
  @Autowired
  private SiglusArchiveProductService archiveProductService;
  @Autowired
  private SiglusDateHelper dateHelper;
  @Autowired
  private LotConflictService lotConflictService;
  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;
  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;
  @Autowired
  private ActiveDraftValidator draftValidator;
  @Autowired
  private StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;

  @Value("${stockmanagement.kit.unpack.reasonId}")
  private UUID unpackReasonId;

  @Value("${stockmanagement.kit.unpack.destination.nodeId}")
  private UUID unpackDestinationNodeId;
  @Autowired
  private SiglusStockEventsService siglusStockEventsService;

  @Transactional
  public UUID createStockEvent(StockEventDto eventDto) {
    UUID userId = getUserId(eventDto);
    if (ALL_PRODUCTS_PROGRAM_ID.equals(eventDto.getProgramId())) {
      Map<UUID, UUID> programIdToEventId = createStockEventForPrograms(eventDto, userId);
      if (!programIdToEventId.isEmpty()) {
        return programIdToEventId.values().stream().findFirst().orElse(null);
      }
      return null;
    }
    return createStockEventForOneProgram(eventDto, userId);

  }

  @Transactional
  public UUID createStockEventForOneProgram(StockEventDto eventDto, UUID userId) {
    eventDto.setUserId(userId);
    siglusStockEventsService.createAndFillLotId(eventDto);
    if (eventDto.isPhysicalInventory()) {
      UUID programId = eventDto.getProgramId();
      List<StockEventDto> stockEventDtos;
      List<PhysicalInventoryDto> physicalInventoryDtoList = siglusPhysicalInventoryService
          .getPhysicalInventoryDtosDirectly(programId, eventDto.getFacilityId(), Boolean.TRUE);

      if (CollectionUtils.isEmpty(physicalInventoryDtoList)) {
        throw new ValidationMessageException("stockmanagement.error.physicalInventory.isSubmitted");
      }
      stockEventDtos = physicalInventoryDtoList.stream()
          .map(StockEventDto::fromPhysicalInventoryDto)
          .collect(Collectors.toList());

      Map<UUID, UUID> programIdToEventId = new HashMap<>();
      stockEventDtos.forEach(stockEventDto -> {
        stockEventDto.setFacilityId(eventDto.getFacilityId());
        stockEventDto.setSignature(eventDto.getSignature());
        stockEventDto.setDocumentNumber(eventDto.getDocumentNumber());
        stockEventDto.setUserId(eventDto.getUserId());
        stockEventDto.setType(eventDto.getType());
        stockEventDto.setLineItems(eventDto.getLineItems().stream()
            .filter(lineItem -> lineItem.getProgramId() != null)
            .filter(lineItem -> lineItem.getProgramId().equals(stockEventDto.getProgramId()))
            .collect(Collectors.toList()));
        programIdToEventId.put(stockEventDto.getProgramId(), siglusCreateStockEvent(stockEventDto));
      });

      if (!programIdToEventId.isEmpty() && eventDto.isPhysicalInventory()) {
        siglusPhysicalInventoryService
            .deletePhysicalInventoryForProductInOneProgramDirectly(eventDto.getFacilityId(),
                programId);
      }
      return programIdToEventId.get(programId);
    } else {
      UUID stockEventId = stockEventsStockManagementService.createStockEvent(eventDto);
      enhanceStockCard(eventDto, stockEventId);
      return stockEventId;
    }
  }

  private Map<UUID, UUID> createStockEventForPrograms(StockEventDto eventDto, UUID userId) {
    eventDto.setUserId(userId);
    siglusStockEventsService.createAndFillLotId(eventDto);
    Set<UUID> programIds = eventDto.getLineItems().stream()
        .map(StockEventLineItemDto::getProgramId)
        .collect(Collectors.toSet());
    List<StockEventDto> stockEventDtos;
    if (eventDto.isPhysicalInventory()) {
      List<PhysicalInventoryDto> inventories = programIds.stream()
          .map(programId -> siglusPhysicalInventoryService
              .getPhysicalInventoryDtosDirectly(programId, eventDto.getFacilityId(), Boolean.TRUE))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
      if (CollectionUtils.isEmpty(inventories)) {
        throw new ValidationMessageException("stockmanagement.error.physicalInventory.isSubmitted");
      }
      stockEventDtos = inventories.stream()
          .map(StockEventDto::fromPhysicalInventoryDto)
          .collect(Collectors.toList());
    } else {
      if (isNotUnpack(eventDto)) {
        List<StockManagementDraftDto> stockManagementDraftDtos = stockManagementDraftService
            .findStockManagementDraft(ALL_PRODUCTS_PROGRAM_ID, getDraftType(eventDto), true);
        if (CollectionUtils.isEmpty(stockManagementDraftDtos)) {
          throw new ValidationMessageException(ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED);
        }
      }
      stockEventDtos = programIds.stream()
          .map(StockEventDto::fromProgramId)
          .collect(Collectors.toList());
    }
    Map<UUID, UUID> programIdToEventId = new HashMap<>();
    stockEventDtos.forEach(stockEventDto -> {
      stockEventDto.setFacilityId(eventDto.getFacilityId());
      stockEventDto.setSignature(eventDto.getSignature());
      stockEventDto.setDocumentNumber(eventDto.getDocumentNumber());
      stockEventDto.setUserId(eventDto.getUserId());
      stockEventDto.setType(eventDto.getType());
      stockEventDto.setLineItems(eventDto.getLineItems().stream()
          .filter(lineItem -> lineItem.getProgramId() != null)
          .filter(lineItem -> lineItem.getProgramId().equals(stockEventDto.getProgramId()))
          .collect(Collectors.toList()));
      programIdToEventId.put(stockEventDto.getProgramId(), siglusCreateStockEvent(stockEventDto));
    });
    if (!programIdToEventId.isEmpty()) {
      if (eventDto.isPhysicalInventory()) {
        siglusPhysicalInventoryService
            .deletePhysicalInventoryForAllProductsDirectly(eventDto.getFacilityId());
      } else if (isNotUnpack(eventDto)) {
        String type = getDraftType(eventDto);
        eventDto.setType(type);
        stockManagementDraftService.deleteStockManagementDraft(eventDto);
      }
    }
    return programIdToEventId;
  }

  /**
   * reason for create a new transaction:
   * Running this method in the super transaction will cause 'stockmanagement.error.event.lot.not.exist' execption.
   * detail steps：
   * 1. method createStockEventForOneProgram do something
   * 2. method createAndFillLotId insert a new lot, with new uuid(This method)
   * 3. method createStockEventForOneProgram call siglusCreateStockEvent and in stockEventProcessor.process build
   * context. When building context, it start a http request /api/lots/ to getLotsByIds(which beyond the super
   * transaction scope, so the http must see the change in step 2)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createAndFillLotId(StockEventDto eventDto) {
    final List<StockEventLineItemDto> lineItems = eventDto.getLineItems();
    Set<UUID> orderableIds = lineItems.stream().map(StockEventLineItemDto::getOrderableId)
        .collect(Collectors.toSet());
    Map<UUID, OrderableDto> orderableDtos = orderableReferenceDataService.findByIds(orderableIds)
        .stream()
        .collect(Collectors.toMap(OrderableDto::getId, orderableDto -> orderableDto));
    for (StockEventLineItemDto eventLineItem : lineItems) {
      UUID orderableId = eventLineItem.getOrderableId();
      OrderableDto orderable = orderableDtos.get(orderableId);
      if (orderable.getIsKit()) {
        validateLotMustBeNull(eventLineItem);
        continue;
      }
      UUID facilityId = getFacilityId(eventDto);
      fillLotIdIfNull(facilityId, orderable, eventLineItem);
    }
  }

  private UUID siglusCreateStockEvent(StockEventDto eventDto) {
    // do the creation
    List<StockEventLineItemDto> lineItems = eventDto.getLineItems();
    lineItems.forEach(lineItem -> lineItem.setId(UUID.randomUUID()));
    eventDto.setLineItems(lineItems);
    UUID stockEventId = stockEventProcessor.process(eventDto);
    enhanceStockCard(eventDto, stockEventId);
    return stockEventId;
  }

  private void enhanceStockCard(StockEventDto eventDto, UUID stockEventId) {
    addStockCardLineItemLocation(eventDto);
    addStockCardCreateTime(eventDto);
    addStockCardLineItemDocumentNumber(eventDto, stockEventId);
    Set<UUID> orderableIds = eventDto.getLineItems().stream()
        .map(StockEventLineItemDto::getOrderableId)
        .collect(Collectors.toSet());
    archiveProductService.activateProducts(eventDto.getFacilityId(), orderableIds);
  }

  private void fillLotIdIfNull(UUID facilityId, OrderableDto orderable,
      StockEventLineItemDto eventLineItem) {
    if (eventLineItem.getLotId() != null || isBlank(eventLineItem.getLotCode())) {
      // already done or nothing we can do since lot info is missing
      return;
    }
    UUID lotId = createNewLotOrReturnExisted(facilityId, orderable, eventLineItem.getLotCode(),
        eventLineItem.getExpirationDate()).getId();
    eventLineItem.setLotId(lotId);
  }

  private void validateLotMustBeNull(StockEventLineItemDto stockEventLineItem) {
    if (StringUtils.isNotBlank(stockEventLineItem.getLotCode())
        || stockEventLineItem.getLotId() != null) {
      throw new ValidationMessageException(new Message(ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY));
    }
  }

  public LotDto createNewLotOrReturnExisted(UUID facilityId, OrderableDto orderable, String lotCode,
      LocalDate expirationDate) {
    String tradeItemId = orderable.getTradeItemIdentifier();
    if (null == tradeItemId) {
      throw new ValidationMessageException(new Message(ERROR_TRADE_ITEM_IS_EMPTY));
    }
    LotDto existedLot = findExistedLot(lotCode, tradeItemId);
    if (existedLot == null) {
      LotDto lotDto = new LotDto();
      lotDto.setId(Lot.of(lotCode, expirationDate).getUUid());
      lotDto.setTradeItemId(UUID.fromString(tradeItemId));
      lotDto.setManufactureDate(dateHelper.getCurrentDate());
      lotDto.setExpirationDate(expirationDate);
      lotDto.setActive(true);
      lotDto.setLotCode(lotCode);
      return lotReferenceDataService.saveLot(lotDto);
    }
    lotConflictService
        .handleLotConflict(facilityId, lotCode, existedLot.getId(), expirationDate,
            existedLot.getExpirationDate());
    return existedLot;
  }

  private UUID getUserId(StockEventDto eventDto) {
    if (eventDto.getUserId() != null) {
      return eventDto.getUserId();
    }
    return authenticationHelper.getCurrentUser().getId();
  }

  private UUID getFacilityId(StockEventDto eventDto) {
    if (eventDto.getFacilityId() != null) {
      return eventDto.getFacilityId();
    }
    return authenticationHelper.getCurrentUser().getHomeFacilityId();
  }

  private LotDto findExistedLot(String lotCode, String tradeItemId) {
    LotSearchParams lotSearchParams = new LotSearchParams();
    lotSearchParams.setLotCode(lotCode);
    lotSearchParams.setTradeItemId(singletonList(UUID.fromString(tradeItemId)));
    List<LotDto> existedLots = lotReferenceDataService.getLots(lotSearchParams);
    return existedLots.stream().filter(lotDto -> lotDto.getLotCode().equals(lotCode)).findFirst()
        .orElse(null);
  }

  private void addStockCardLineItemLocation(StockEventDto eventDto) {
    List<StockEventLineItemDto> lineItems = eventDto.getLineItems();
    lineItems.stream().filter(lineItem -> lineItem.getLocationCode() != null && lineItem.getArea() != null)
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
    Map<UUID, StockCardExtension> stockCardIdToExtensionMap = Maps
        .uniqueIndex(stockCardExtensionRepository.findByStockCardIdIn(stockCardIds),
            StockCardExtension::getStockCardId);
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
    Map<String, String> documentationNoMap = eventDto.getLineItems()
        .stream()
        .collect(Collectors.toMap(
            stockEventLineItemDto -> stockEventLineItemDto.getOrderableId().toString()
                + stockEventLineItemDto.getLotId()
                + stockEventLineItemDto.getReasonId()
                + stockEventLineItemDto.getSourceId()
                + stockEventLineItemDto.getDestinationId(),
            stockEventLineItemDto -> Optional.ofNullable(stockEventLineItemDto.getDocumentationNo())
                .orElse(""),
            (v1, v2) -> v1));
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findByOriginEvent(stockEventsRepository.findOne(stockEventId));
    stockCardLineItems.forEach(stockCardLineItem -> {
      UUID orderableId = stockCardLineItem.getStockCard().getOrderableId();
      stockCardLineItem.setDocumentNumber(documentationNoMap.get(orderableId.toString()
          + stockCardLineItem.getStockCard().getLotId()
          + Optional.ofNullable(stockCardLineItem.getReason()).map(StockCardLineItemReason::getId)
          .orElse(null)
          + Optional.ofNullable(stockCardLineItem.getSource()).map(Node::getId)
          .orElse(null)
          + Optional.ofNullable(stockCardLineItem.getDestination()).map(Node::getId)
          .orElse(null)
      ));
    });
    stockCardLineItemRepository.save(stockCardLineItems);
  }

  private boolean isNotUnpack(StockEventDto eventDto) {
    return !(eventDto.hasLineItems() && eventDto.getLineItems().stream().anyMatch((lineItem) ->
        unpackDestinationNodeId.equals(lineItem.getDestinationId())));
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

  @Transactional
  public UUID createStockEventForMultiUser(StockEventForMultiUserDto stockEventForMultiUserDto) {
    List<UUID> subDraftIds = stockEventForMultiUserDto.getSubDrafts();
    validatePreSubmitSubDraft(subDraftIds);
    return createStockEvent(stockEventForMultiUserDto.getStockEvent());
  }

  private void validatePreSubmitSubDraft(List<UUID> subDraftIds) {
    if (subDraftIds.isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY),
          "subDrafts empty");
    }
    StockManagementDraft subDraft = stockManagementDraftRepository.findOne(subDraftIds.get(0));
    draftValidator.validateSubDraft(subDraft);
    int subDraftsQuantity = stockManagementDraftRepository
        .countByInitialDraftId(subDraft.getInitialDraftId());
    if (subDraftIds.size() != subDraftsQuantity) {
      throw new BusinessDataException(
          new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_QUANTITY_NOT_MATCH),
          "subDrafts quantity not match");
    }
  }
}
