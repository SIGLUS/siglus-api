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

import static com.google.common.collect.Lists.newArrayList;
import static org.siglus.common.i18n.MessageKeys.ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY;
import static org.siglus.common.i18n.MessageKeys.ERROR_TRADE_ITEM_IS_EMPTY;
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;

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
import org.siglus.common.domain.StockCardExtension;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.LotSearchParams;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.repository.StockCardExtensionRepository;
import org.siglus.common.util.Message;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.domain.LotConflict;
import org.siglus.siglusapi.repository.LotConflictRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.StockEventsStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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
  private LotConflictRepository lotConflictRepository;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private SiglusStockEventsService stockEventsService;

  @Value("${stockmanagement.kit.unpack.reasonId}")
  private UUID unpackReasonId;

  @Transactional
  public UUID createStockEvent(StockEventDto eventDto) {
    UUID stockEventId = stockEventsStockManagementService.createStockEvent(eventDto);
    enhanceStockCard(eventDto, stockEventId);
    return stockEventId;
  }

  @Transactional
  public UUID createStockEventForAllProducts(StockEventDto eventDto) {
    Set<UUID> programIds = eventDto.getLineItems().stream()
        .map(StockEventLineItemDto::getProgramId).collect(Collectors.toSet());
    List<StockEventDto> stockEventDtos;
    if (eventDto.isPhysicalInventory()) {
      List<PhysicalInventoryDto> inventories = programIds.stream()
          .map(programId -> siglusPhysicalInventoryService
              .getPhysicalInventoryDtos(programId, eventDto.getFacilityId(), Boolean.TRUE))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
      stockEventDtos = inventories.stream()
          .map(StockEventDto::fromPhysicalInventoryDto)
          .collect(Collectors.toList());
    } else {
      stockEventDtos = programIds.stream()
          .map(StockEventDto::fromProgramId)
          .collect(Collectors.toList());
    }
    Map<UUID, UUID> programIdToEventId = callV3StockEventForAllProducts(eventDto, stockEventDtos);
    if (programIdToEventId.isEmpty()) {
      if (eventDto.isPhysicalInventory()) {
        siglusPhysicalInventoryService
            .deletePhysicalInventoryForAllProducts(eventDto.getFacilityId());
      } else if (!eventDto.hasReason(unpackReasonId)) {
        setType(eventDto);
        stockManagementDraftService.deleteStockManagementDraft(eventDto);
      }
      return programIdToEventId.values().stream().findFirst().orElse(null);
    }
    return null;
  }

  @Transactional
  public Map<UUID, UUID> createStockEventForNoDraftAllProducts(StockEventDto eventDto) {
    UserDto userDto = authenticationHelper.getCurrentUser();
    eventDto.setUserId(userDto.getId());
    stockEventsService.createAndFillLotId(eventDto, false, userDto);
    Set<UUID> programIds = eventDto.getLineItems().stream()
        .map(StockEventLineItemDto::getProgramId).collect(Collectors.toSet());
    List<StockEventDto> stockEventDtos = programIds.stream()
        .map(StockEventDto::fromProgramId)
        .collect(Collectors.toList());
    return callV3StockEventForAllProducts(eventDto, stockEventDtos);
  }

  private Map<UUID, UUID> callV3StockEventForAllProducts(StockEventDto eventDto,
      List<StockEventDto> stockEventDtos) {
    Map<UUID, UUID> programIdToEventId = new HashMap<>();
    stockEventDtos.stream().forEach(stockEventDto -> {
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
    return programIdToEventId;
  }

  @Transactional
  public void createAndFillLotId(StockEventDto eventDto, boolean updateExpirationDate, UserDto userDto) {
    final List<StockEventLineItemDto> lineItems = eventDto.getLineItems();
    Map<UUID, OrderableDto> orderableDtos = orderableReferenceDataService.findByIds(
        lineItems.stream().map(StockEventLineItemDto::getOrderableId).collect(Collectors.toSet()))
        .stream()
        .collect(Collectors.toMap(OrderableDto::getId, orderableDto -> orderableDto));
    for (StockEventLineItemDto stockEventLineItem : lineItems) {
      UUID orderableId = stockEventLineItem.getOrderableId();
      boolean isKit = !orderableDtos.get(orderableId).getChildren().isEmpty();
      if (isKit) {
        verifyKitInfo(stockEventLineItem);
      } else if (stockEventLineItem.getLotId() == null && StringUtils.isNotBlank(stockEventLineItem.getLotCode())) {
        String tradeItemId = orderableDtos.get(stockEventLineItem.getOrderableId()).getIdentifiers().get(TRADE_ITEM);
        fillLotIdForNormalOrderable(stockEventLineItem, tradeItemId, updateExpirationDate, userDto);
      }
    }
  }

  private UUID siglusCreateStockEvent(StockEventDto eventDto) {
    UUID stockEventId = stockEventProcessor.process(eventDto);
    enhanceStockCard(eventDto, stockEventId);
    return stockEventId;
  }

  private void enhanceStockCard(StockEventDto eventDto, UUID stockEventId) {
    addStockCardCreateTime(eventDto);
    addStockCardLineItemDocumentNumber(eventDto, stockEventId);
    Set<UUID> orderableIds = eventDto.getLineItems().stream()
        .map(StockEventLineItemDto::getOrderableId)
        .collect(Collectors.toSet());
    archiveProductService.activateProducts(eventDto.getFacilityId(), orderableIds);
  }

  private void fillLotIdForNormalOrderable(StockEventLineItemDto stockEventLineItem, String tradeItemId,
      boolean updateExpirationDate, UserDto userDto) {
    UUID lotId = createNewLotOrReturnExisted(stockEventLineItem.getLotCode(),
        stockEventLineItem.getExpirationDate(), tradeItemId, updateExpirationDate, userDto).getId();
    stockEventLineItem.setLotId(lotId);
  }

  private void verifyKitInfo(StockEventLineItemDto stockEventLineItem) {
    if (StringUtils.isNotBlank(stockEventLineItem.getLotCode()) || stockEventLineItem.getLotId() != null) {
      throw new ValidationMessageException(new Message(ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY));
    }
  }

  private LotDto createNewLotOrReturnExisted(String lotCode, LocalDate expirationDate,
      String tradeItemId, Boolean updateExpirationDate, UserDto userDto) {
    if (null == tradeItemId) {
      throw new ValidationMessageException(new Message(ERROR_TRADE_ITEM_IS_EMPTY));
    }
    LotSearchParams lotSearchParams = new LotSearchParams();
    lotSearchParams.setLotCode(lotCode);
    lotSearchParams.setTradeItemId(newArrayList(UUID.fromString(tradeItemId)));
    List<LotDto> existedLots = lotReferenceDataService.getLots(lotSearchParams);
    if (CollectionUtils.isNotEmpty(existedLots)) {
      LotDto existedLot = existedLots.get(0);
      if (Boolean.TRUE.equals(updateExpirationDate) && !existedLot.getExpirationDate().isEqual(expirationDate)) {
        LotConflict conflict = lotConflictRepository
            .findLotConflictByFacilityIdAndLotId(userDto.getHomeFacilityId(), existedLot.getId());
        if (conflict == null) {
          LotConflict lotConflict = LotConflict.builder()
              .expirationDate(expirationDate)
              .lotId(existedLot.getId())
              .lotCode(existedLot.getLotCode())
              .facilityId(userDto.getHomeFacilityId())
              .build();
          log.info("save lot Conflict: {}", lotConflict);
          lotConflictRepository.save(lotConflict);
        }
        log.info("lot existed date is different: {}", lotCode);
      }
      return existedLot;
    }
    LotDto lotDto = new LotDto();
    lotDto.setTradeItemId(UUID.fromString(tradeItemId));
    lotDto.setManufactureDate(dateHelper.getCurrentDate());
    lotDto.setExpirationDate(expirationDate);
    lotDto.setActive(true);
    lotDto.setLotCode(lotCode);
    return lotReferenceDataService.saveLot(lotDto);
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
            stockEventLineItemDto -> Optional.ofNullable(stockEventLineItemDto.getDocumentationNo()).orElse(""),
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

  private void setType(StockEventDto eventDto) {
    if (eventDto.hasSource()) {
      eventDto.setType("receive");
    } else if (eventDto.hasDestination()) {
      eventDto.setType("issue");
    } else {
      eventDto.setType("adjustment");
    }
  }

}
