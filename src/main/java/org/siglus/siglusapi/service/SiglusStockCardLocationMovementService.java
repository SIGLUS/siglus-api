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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.siglus.siglusapi.constant.FieldConstants.CAPITAL_ADJUSTMENT;
import static org.siglus.siglusapi.constant.FieldConstants.CAPITAL_ISSUE;
import static org.siglus.siglusapi.constant.FieldConstants.CAPITAL_RECEIVE;
import static org.siglus.siglusapi.constant.FieldConstants.INVENTORY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_LINE_ITEMS_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_LESS_THAN_STOCK_ON_HAND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_CARD_NOT_FOUND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.openlmis.requisition.dto.ReasonType;
import org.openlmis.stockmanagement.domain.BaseEntity;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.OrderableReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.constant.LocationConstants;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.siglus.siglusapi.dto.InitialMoveProductFieldDto;
import org.siglus.siglusapi.dto.LocationMovementDto;
import org.siglus.siglusapi.dto.LocationMovementLineItemDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.StockCardLocationMovementDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementLineItemDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusStockCardLocationMovementService {

  private final StockCardLocationMovementLineItemRepository movementLineItemRepository;
  private final StockCardLocationMovementDraftRepository movementDraftRepository;
  private final SiglusStockCardRepository siglusStockCardRepository;
  private final StockCardRepository stockCardRepository;
  private final CalculatedStocksOnHandByLocationService calculatedStocksOnHandByLocationService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  private final SiglusStockCardService siglusStockCardService;
  private final SiglusAdministrationsService administrationsService;

  private final FacilityReferenceDataService facilityReferenceDataService;

  private final OrderableReferenceDataService orderableReferenceDataService;

  private final ProgramReferenceDataService programReferenceDataService;

  private final LotReferenceDataService lotReferenceDataService;

  public InitialMoveProductFieldDto canInitialMoveProduct(UUID facilityId) {

    if (BooleanUtils.isFalse(administrationsService.getFacility(facilityId).getEnableLocationManagement())) {
      return new InitialMoveProductFieldDto(false);
    }
    List<UUID> stockCardIds = stockCardRepository.findByFacilityIdIn(facilityId)
        .stream().map(StockCard::getId).collect(Collectors.toList());
    List<CalculatedStockOnHandByLocation> calculatedStockOnHandByLocationList =
        findLatestCalculatedSohByLocationVirtualLocationRecordByStockCardId(stockCardIds);

    return new InitialMoveProductFieldDto(!calculatedStockOnHandByLocationList.isEmpty());
  }

  @Transactional
  public void createMovementLineItems(StockCardLocationMovementDto movementDto) {
    validateMovementLineItems(movementDto);
    checkStockOnHand(movementDto);
    List<StockCardLocationMovementLineItem> movementLineItems = convertMovementDtoToMovementItems(movementDto);
    movementLineItemRepository.save(movementLineItems);
    deleteMovementDraft(movementDto);
    calculatedStocksOnHandByLocationService.calculateStockOnHandByLocationForMovement(movementLineItems);
    deleteEmptySohVirtualLocation(movementDto.getFacilityId());
  }

  void deleteEmptySohVirtualLocation(UUID facilityId) {
    List<UUID> stockCardIds = stockCardRepository.findByFacilityIdIn(facilityId).stream()
        .map(BaseEntity::getId).collect(Collectors.toList());
    List<CalculatedStockOnHandByLocation> allSohByLocations =
        calculatedStockOnHandByLocationRepository.findAllByStockCardIds(stockCardIds);
    Set<UUID> emptyVirtualStockCardIds = allSohByLocations.stream().filter(
        e -> e.getStockOnHand() == 0 && Objects.equals(e.getLocationCode(), LocationConstants.VIRTUAL_LOCATION_CODE))
        .map(CalculatedStockOnHandByLocation::getStockCardId).collect(Collectors.toSet());

    List<UUID> toBeDeletedCalculatedSohByLocationIds = allSohByLocations.stream()
        .filter(
            e -> emptyVirtualStockCardIds.contains(e.getStockCardId())
                && Objects.equals(e.getLocationCode(), LocationConstants.VIRTUAL_LOCATION_CODE))
        .map(org.siglus.common.domain.BaseEntity::getId).collect(Collectors.toList());
    if (!toBeDeletedCalculatedSohByLocationIds.isEmpty()) {
      calculatedStockOnHandByLocationRepository.deleteAllByIdIn(toBeDeletedCalculatedSohByLocationIds);
      log.info("virtual location calculation has been deleted  size : {} ",
          toBeDeletedCalculatedSohByLocationIds.size());
    }

  }

  private void deleteMovementDraft(StockCardLocationMovementDto movementDto) {
    List<StockCardLocationMovementDraft> movementDrafts = movementDraftRepository
        .findByProgramIdAndFacilityId(movementDto.getProgramId(), movementDto.getFacilityId());
    if (movementDrafts.isEmpty()) {
      throw new NotFoundException(ERROR_MOVEMENT_DRAFT_NOT_FOUND);
    }
    movementDraftRepository.delete(movementDrafts.get(0));
  }

  public LocationMovementDto getLocationMovementDto(UUID stockCardId, String locationCode) {
    List<LocationMovementLineItemDto> productLocationMovement =
        calculatedStockOnHandByLocationRepository.getProductLocationMovement(stockCardId, locationCode);
    List<LocationMovementLineItemDto> stockMovementWithLocation =
        calculatedStockOnHandByLocationRepository.getStockMovementWithLocation(stockCardId, locationCode);
    if (CollectionUtils.isNotEmpty(stockMovementWithLocation)) {
      productLocationMovement.addAll(stockMovementWithLocation);
    }
    List<LocationMovementLineItemDto> locationMovementLineItemDtos = productLocationMovement.stream()
        .sorted(Comparator.comparing(LocationMovementLineItemDto::getProcessedDate)).collect(
            Collectors.toList());
    Integer soh = 0;
    for (LocationMovementLineItemDto locationMovementLineItemDto : locationMovementLineItemDtos) {
      Integer quantity = locationMovementLineItemDto.getQuantity();
      locationMovementLineItemDto.setQuantity(Math.abs(locationMovementLineItemDto.getQuantity()));
      switch (locationMovementLineItemDto.getReasonCategory()) {
        case INVENTORY:
          soh += quantity;
          break;
        case CAPITAL_ISSUE:
          soh -= quantity;
          break;
        case CAPITAL_RECEIVE:
          soh += quantity;
          break;
        case CAPITAL_ADJUSTMENT:
          if (locationMovementLineItemDto.getReasonType().equals(ReasonType.DEBIT.name())) {
            soh -= quantity;
          }
          if (locationMovementLineItemDto.getReasonType().equals(ReasonType.CREDIT.name())) {
            soh += quantity;
          }
          break;
        default:
          break;
      }
      locationMovementLineItemDto.setSoh(soh);
    }
    Collections.reverse(locationMovementLineItemDtos);
    resetFirstMovementQuantity(locationMovementLineItemDtos);
    Integer latestSoh = calculatedStockOnHandByLocationRepository.findRecentlySohByStockCardIdAndLocationCode(
            stockCardId, locationCode).orElse(0);
    return createLocationMovmentDto(locationMovementLineItemDtos, stockCardId, latestSoh, locationCode);
  }

  private void resetFirstMovementQuantity(List<LocationMovementLineItemDto> locationMovementLineItemDtos) {
    LocationMovementLineItemDto first = locationMovementLineItemDtos.get(locationMovementLineItemDtos.size() - 1);
    first.setQuantity(first.getSoh());
  }

  private List<CalculatedStockOnHandByLocation> findLatestCalculatedSohByLocationVirtualLocationRecordByStockCardId(
      List<UUID> stockCardIds) {
    return !stockCardIds.isEmpty()
        ? calculatedStockOnHandByLocationRepository.findLatestLocationSohByStockCardIds(stockCardIds)
        .stream().filter(calculatedByLocation -> calculatedByLocation.getStockOnHand() > 0
            && LocationConstants.VIRTUAL_LOCATION_CODE.equals(calculatedByLocation.getLocationCode()))
        .collect(Collectors.toList())
        : Collections.emptyList();
  }

  private LocationMovementDto createLocationMovmentDto(List<LocationMovementLineItemDto> locationMovementLineItemDtos,
      UUID stockCardId, int soh, String locationCode) {
    LocationMovementLineItemDto firstLineItem =
        locationMovementLineItemDtos.get(locationMovementLineItemDtos.size() - 1);

    LocationMovementLineItemDto initialLineItemDto = new LocationMovementLineItemDto();
    initialLineItemDto.setSoh(0);
    initialLineItemDto.setAdjustment("Inventário");
    initialLineItemDto.setOccurredDate(firstLineItem.getOccurredDate());
    locationMovementLineItemDtos.add(initialLineItemDto);

    StockCard stockCard = stockCardRepository.getOne(stockCardId);
    FacilityDto facility = facilityReferenceDataService.findOne(stockCard.getFacilityId());
    OrderableDto orderable = orderableReferenceDataService.findOne(stockCard.getOrderableId());
    ProgramDto program = programReferenceDataService.findOne(stockCard.getProgramId());
    LotDto lot = lotReferenceDataService.findOne(stockCard.getLotId());
    return LocationMovementDto.builder()
        .facilityName(facility.getName())
        .productName(orderable.getFullProductName())
        .orderableId(orderable.getId())
        .productCode(orderable.getProductCode())
        .displayUnit(orderable.getDispensable().getDisplayUnit())
        .lotCode(lot == null ? null : lot.getLotCode())
        .program(program.getName())
        .programId(program.getId())
        .locationCode(locationCode)
        .lineItems(locationMovementLineItemDtos)
        .stockOnHand(soh)
        .expiryDate(lot == null ? null : lot.getExpirationDate())
        .build();
  }

  private List<StockCardLocationMovementLineItem> convertMovementDtoToMovementItems(
      StockCardLocationMovementDto movementDto) {
    List<StockCardLocationMovementLineItem> movementLineItems = new ArrayList<>();
    movementDto.getMovementLineItems().forEach(lineItemDto -> {
      List<StockCard> stockCards = Boolean.TRUE.equals(lineItemDto.getIsKit()) ? siglusStockCardRepository
          .findByFacilityIdAndOrderableId(movementDto.getFacilityId(), lineItemDto.getOrderableId())
          : siglusStockCardRepository.findByFacilityIdAndOrderableIdAndLotId(movementDto.getFacilityId(),
          lineItemDto.getOrderableId(), lineItemDto.getLotId());
      if (stockCards.isEmpty()) {
        throw new NotFoundException(ERROR_STOCK_CARD_NOT_FOUND);
      }
      StockCardLocationMovementLineItem movementLineItem = StockCardLocationMovementLineItem.builder()
          .stockCardId(stockCards.get(0).getId())
          .srcArea(lineItemDto.getSrcArea())
          .srcLocationCode(lineItemDto.getSrcLocationCode())
          .destArea(lineItemDto.getDestArea())
          .destLocationCode(lineItemDto.getDestLocationCode())
          .quantity(lineItemDto.getQuantity())
          .occurredDate(movementDto.getOccurredDate())
          .userId(movementDto.getUserId())
          .signature(movementDto.getSignature())
          .build();
      movementLineItems.add(movementLineItem);
    });
    return movementLineItems;
  }

  private void checkStockOnHand(StockCardLocationMovementDto movementDto) {
    List<StockCardLocationMovementLineItemDto> lineItems = movementDto.getMovementLineItems();
    boolean isInitialMoveProduct = canInitialMoveProduct(
        authenticationHelper.getCurrentUser().getHomeFacilityId()).isNeedInitiallyMoveProduct();
    Set<Entry<String, List<StockCardLocationMovementLineItemDto>>> groupByOrderableIdLotIdSrcAreaAndSrcLocationCode =
        lineItems.stream().collect(Collectors.groupingBy(this::fetchGroupKey)).entrySet();
    groupByOrderableIdLotIdSrcAreaAndSrcLocationCode.forEach(entry -> {
      int totalMovementQuantity = entry.getValue()
          .stream()
          .mapToInt(StockCardLocationMovementLineItemDto::getQuantity)
          .sum();
      Integer stockOnHand = entry.getValue().get(0).getStockOnHand();
      if (totalMovementQuantity > stockOnHand) {
        throw new BusinessDataException(new Message(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND),
            "movement quantity more than stock on hand");
      }
      if (isInitialMoveProduct && totalMovementQuantity < stockOnHand) {
        throw new BusinessDataException(new Message(ERROR_MOVEMENT_QUANTITY_LESS_THAN_STOCK_ON_HAND));
      }
    });
  }

  private String fetchGroupKey(StockCardLocationMovementLineItemDto lineItemDto) {
    return Boolean.TRUE.equals(lineItemDto.getIsKit()) ? lineItemDto.getOrderableId().toString()
        + lineItemDto.getSrcArea()
        + lineItemDto.getSrcLocationCode() : lineItemDto.getOrderableId().toString()
        + lineItemDto.getLotId().toString()
        + lineItemDto.getSrcArea()
        + lineItemDto.getSrcLocationCode();
  }

  private void validateMovementLineItems(StockCardLocationMovementDto movementDto) {
    List<StockCardLocationMovementLineItemDto> movementLineItems = movementDto.getMovementLineItems();
    validateLineItems(movementLineItems);
  }

  private void validateLineItems(List<StockCardLocationMovementLineItemDto> lineItems) {
    if (isEmpty(lineItems)) {
      throw new ValidationMessageException(ERROR_MOVEMENT_LINE_ITEMS_MISSING);
    }
  }
}
