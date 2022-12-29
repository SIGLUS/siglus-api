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

import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY_SUBMIT_PRODUCT_ZERO_INVENTORY_DAY_RANGE;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY_SUBMIT_PRODUCT_ZERO_INVENTORY_DAY_RANGE;
import static org.siglus.siglusapi.constant.FieldConstants.SEPARATOR;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.dto.LotDto;
import org.openlmis.referencedata.service.LotSearchParams;
import org.openlmis.referencedata.web.LotController;
import org.openlmis.stockmanagement.domain.BaseEntity;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.constant.LocationConstants;
import org.siglus.siglusapi.constant.PeriodConstants;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.OrderableIdentifiers;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.LocationLotsDto;
import org.siglus.siglusapi.dto.LotsDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.OrderableIdentifiersRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusLotLocationService {

  private final SiglusAuthenticationHelper authenticationHelper;
  private final StockCardRepository stockCardRepository;
  private final FacilityLocationsRepository facilityLocationsRepository;
  private final CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  private final FacilityNativeRepository facilityNativeRepository;
  private final CalculatedStockOnHandRepository calculatedStockOnHandRepository;
  private final SiglusStockCardLocationMovementService stockCardLocationMovementService;
  private final LotController lotController;
  private final SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;
  private final SiglusStockCardSummariesService siglusStockCardSummariesService;
  private final OrderableIdentifiersRepository orderableIdentifiersRepository;

  public List<LocationLotsDto> searchLotLocationDtos(List<UUID> orderableIds, boolean extraData, boolean isAdjustment,
      boolean returnNoMovementLots) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    if (!extraData) {
      return getLocationsByFacilityId(facilityId);
    }

    List<LocationLotsDto> locationLotsDtos = new ArrayList<>(
        getLotLocationDtos(orderableIds, facilityId, isAdjustment));

    if (returnNoMovementLots) {
      List<UUID> lotIds = locationLotsDtos
          .stream()
          .flatMap(e -> e.getLots()
              .stream()
              .map(LotsDto::getLotId))
          .collect(Collectors.toList());
      locationLotsDtos.add(getNoMovementLotLocationDto(orderableIds, lotIds));
    }

    return locationLotsDtos;
  }

  public List<FacilityLocationsDto> searchLocationsByFacility(Boolean isEmpty) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    if (locations.isEmpty()) {
      throw new NotFoundException(ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND);
    }
    List<FacilityLocationsDto> dtos = convertToDto(locations);
    if (Boolean.TRUE.equals(isEmpty)) {
      List<StockCard> stockCards = stockCardRepository.findByFacilityIdIn(facilityId);
      List<UUID> stockCardIds = stockCards
          .stream()
          .map(StockCard::getId)
          .collect(Collectors.toList());
      List<CalculatedStockOnHandByLocation> sohByLocations = calculatedStockOnHandByLocationRepository
          .findPreviousLocationStockOnHandsGreaterThan0(stockCardIds, LocalDate.now());
      Set<String> noEmptyLocationKeys = sohByLocations
          .stream()
          .map(this::getLocationKey)
          .collect(Collectors.toSet());
      dtos.forEach(dto -> dto.setIsEmpty(!noEmptyLocationKeys.contains(getLocationKey(dto))));
    }
    return dtos;
  }

  private LocationLotsDto getNoMovementLotLocationDto(List<UUID> orderableIds, List<UUID> hasMovementLotIds) {
    List<LotDto> lotDtos = siglusStockCardSummariesService.getLotsDataByOrderableIds(orderableIds);
    List<String> tradeLineItemIds = lotDtos
        .stream()
        .map(e -> e.getTradeItemId().toString())
        .collect(Collectors.toList());
    Map<String, List<OrderableIdentifiers>> tradeItemIdToOrderableIdentifiersMap = orderableIdentifiersRepository
        .findByKeyAndValueIn(FieldConstants.TRADE_ITEM, tradeLineItemIds)
        .stream()
        .collect(Collectors.groupingBy(OrderableIdentifiers::getValue));

    List<UUID> lotIds = lotDtos
        .stream()
        .map(LotDto::getId)
        .collect(Collectors.toList());
    LotSearchParams requestParams = new LotSearchParams(null, null, null, lotIds);
    Map<String, List<LotDto>> lotCodeToLotDtoMap = lotController.getLots(requestParams, null).getContent()
        .stream()
        .collect(Collectors.groupingBy(LotDto::getLotCode));

    List<LotsDto> allLotDtos = lotDtos
        .stream()
        .map(e -> LotsDto.builder()
            .orderableId(tradeItemIdToOrderableIdentifiersMap.get(
                e.getTradeItemId().toString()).get(0).getOrderableId())
            .lotId(lotCodeToLotDtoMap.get(e.getLotCode()).get(0).getId())
            .expirationDate(e.getExpirationDate())
            .lotCode(e.getLotCode())
            .build())
        .collect(Collectors.toList());

    List<LotsDto> noMovementLotDtos = allLotDtos.stream().filter(e -> !hasMovementLotIds.contains(e.getLotId()))
        .collect(Collectors.toList());
    return LocationLotsDto.builder()
        .lots(noMovementLotDtos)
        .build();
  }

  private String getLocationKey(CalculatedStockOnHandByLocation stockOnHandByLocation) {
    return stockOnHandByLocation.getArea() + SEPARATOR + stockOnHandByLocation.getLocationCode();
  }

  private String getLocationKey(FacilityLocationsDto facilityLocationsDto) {
    return facilityLocationsDto.getArea() + SEPARATOR + facilityLocationsDto.getLocationCode();
  }

  private List<LocationLotsDto> getLocationsByFacilityId(UUID facilityId) {
    List<LocationLotsDto> locationLotsDtos = new LinkedList<>();
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    locations.forEach(location -> locationLotsDtos.add(LocationLotsDto.builder()
        .locationCode(location.getLocationCode())
        .area(location.getArea())
        .build()));
    return locationLotsDtos;
  }

  private List<LocationLotsDto> getLotLocationDtos(List<UUID> orderableIds, UUID facilityId, boolean isAdjustment) {
    List<LocationLotsDto> locationLotsDtos = new LinkedList<>();
    List<StockCard> stockCardList;
    if (isAdjustment) {
      stockCardList = getUnrestrictedAdjustmentStockCards(facilityId, orderableIds);
    } else {
      stockCardList = stockCardRepository.findByOrderableIdInAndFacilityId(orderableIds, facilityId);
    }
    if (stockCardList.isEmpty()) {
      return Collections.emptyList();
    }
    Map<UUID, StockCard> stockCardIdToStockCard = Maps.uniqueIndex(stockCardList, StockCard::getId);
    boolean needInitiallyMoveProduct = stockCardLocationMovementService.canInitialMoveProduct(facilityId)
        .isNeedInitiallyMoveProduct();
    Map<String, List<Pair<UUID, CalculatedStockOnHandByLocation>>> locationCodeToLotLocationPairs =
        getLocationCodeToLotLocationPairListMap(stockCardList);

    locationCodeToLotLocationPairs.forEach((locationCode, locationPairs) -> {
      if (locationCode.equals(LocationConstants.VIRTUAL_LOCATION_CODE) && !needInitiallyMoveProduct) {
        return;
      }
      List<LotsDto> lotDtoList = getLotsDtos(locationPairs, stockCardIdToStockCard, needInitiallyMoveProduct);
      locationLotsDtos.add(LocationLotsDto.builder()
          .locationCode(locationCode)
          .area(locationPairs.get(0).getSecond().getArea())
          .lots(lotDtoList)
          .build());
    });
    return locationLotsDtos;
  }


  private Map<String, List<Pair<UUID, CalculatedStockOnHandByLocation>>> getLocationCodeToLotLocationPairListMap(
      List<StockCard> stockCardList) {
    Set<UUID> stockCardIds = stockCardList.stream().map(StockCard::getId).collect(Collectors.toSet());
    List<CalculatedStockOnHandByLocation> recentlyLocationSohLists = calculatedStockOnHandByLocationRepository
        .findRecentlyLocationSohByStockCardIds(stockCardIds);
    Map<UUID, List<CalculatedStockOnHandByLocation>> stockCardIdToLocationSohListMap = recentlyLocationSohLists
        .stream()
        .collect(Collectors.groupingBy(CalculatedStockOnHandByLocation::getStockCardId));

    return reverseMappingRelationship(stockCardIdToLocationSohListMap);
  }

  private List<StockCard> getUnrestrictedAdjustmentStockCards(UUID facilityId, List<UUID> orderableIds) {
    List<StockCard> stockCardList = stockCardRepository.findByOrderableIdInAndFacilityId(orderableIds, facilityId);
    if (stockCardList.isEmpty()) {
      return Collections.emptyList();
    }
    Map<UUID, FacilityProgramPeriodScheduleDto> programIdToSchedulesCode = Maps.uniqueIndex(facilityNativeRepository
        .findFacilityProgramPeriodScheduleByFacilityId(facilityId), FacilityProgramPeriodScheduleDto::getProgramId);

    Map<UUID, List<StockCardLineItem>> stockCardIdToStockCardLineItemsMap = siglusStockCardLineItemRepository
        .findAllByStockCardIn(stockCardList)
        .stream()
        .collect(Collectors.groupingBy(e -> e.getStockCard().getId()));

    List<UUID> stockCardIds = stockCardList.stream().map(BaseEntity::getId).collect(Collectors.toList());
    Map<UUID, List<CalculatedStockOnHand>> stockCardIdToLatestSohMap =
        calculatedStockOnHandRepository.findLatestStockOnHands(stockCardIds, ZonedDateTime.now()).stream()
            .collect(Collectors.groupingBy(CalculatedStockOnHand::getStockCardId));

    List<StockCard> stockCards = new LinkedList<>();
    stockCardList.forEach(stockCard -> {
      Code schedulesCode = getShedulesCode(programIdToSchedulesCode, stockCard);

      List<CalculatedStockOnHand> latestStockOnHands = stockCardIdToLatestSohMap.get(stockCard.getId());
      if (Objects.isNull(latestStockOnHands) || latestStockOnHands.isEmpty()) {
        return;
      }
      Integer stockOnHand = latestStockOnHands.get(0).getStockOnHand();

      if (stockOnHand > 0) {
        stockCards.add(stockCard);
      } else {
        List<StockCardLineItem> stockCardLineItems = stockCardIdToStockCardLineItemsMap.get(stockCard.getId());
        if (hasSohChangeWithMonthlyReport(schedulesCode, stockCardLineItems)) {
          stockCards.add(stockCard);
        }
        if (hasSohChangeWithQuarterlyReport(schedulesCode, stockCardLineItems)) {
          stockCards.add(stockCard);
        }
      }
    });
    return stockCards;
  }

  private boolean hasSohChangeWithQuarterlyReport(Code schedulesCode, List<StockCardLineItem> stockCardLineItems) {
    return (PeriodConstants.QUARTERLY_SCHEDULE_CODE.equals(schedulesCode)
        || PeriodConstants.REPORT_QUARTERLY_SCHEDULE_CODE.equals(schedulesCode))
        && hasActualSohChangesInTimeRange(stockCardLineItems, QUARTERLY_SUBMIT_PRODUCT_ZERO_INVENTORY_DAY_RANGE);
  }

  private boolean hasSohChangeWithMonthlyReport(Code schedulesCode, List<StockCardLineItem> stockCardLineItems) {
    return (PeriodConstants.MONTH_SCHEDULE_CODE.equals(schedulesCode)
        || PeriodConstants.REPORT_MONTH_SCHEDULE_CODE.equals(schedulesCode)
        || Objects.isNull(schedulesCode))
        && hasActualSohChangesInTimeRange(stockCardLineItems, MONTHLY_SUBMIT_PRODUCT_ZERO_INVENTORY_DAY_RANGE);
  }

  private Code getShedulesCode(Map<UUID, FacilityProgramPeriodScheduleDto> programIdToSchedulesCode,
      StockCard stockCard) {
    FacilityProgramPeriodScheduleDto facilityProgramPeriodScheduleDto = programIdToSchedulesCode.get(
        stockCard.getProgramId());
    return Objects.isNull(facilityProgramPeriodScheduleDto)
        ? null
        : Code.code(facilityProgramPeriodScheduleDto.getSchedulesCode());
  }

  private boolean hasActualSohChangesInTimeRange(List<StockCardLineItem> stockCardLineItems, int days) {
    return
        !Objects.isNull(stockCardLineItems)
            && stockCardLineItems.stream()
            .filter(o -> o.getOccurredDate().isAfter(LocalDate.now().minusDays(days + 1L)))
            .anyMatch(o -> o.getQuantity() != 0);
  }

  private Map<String, List<Pair<UUID, CalculatedStockOnHandByLocation>>> reverseMappingRelationship(
      Map<UUID, List<CalculatedStockOnHandByLocation>> stockCardIdToLocationIdMap) {

    List<Pair<UUID, CalculatedStockOnHandByLocation>> stockCardLocationPairs = new LinkedList<>();
    stockCardIdToLocationIdMap.forEach((stockCardId, locationStockOnHandList)
        -> locationStockOnHandList.forEach(locationsStockOnHand -> stockCardLocationPairs.add(Pair.of(
        stockCardId, locationsStockOnHand))));

    return stockCardLocationPairs
        .stream()
        .collect(Collectors.groupingBy(e -> e.getSecond().getLocationCode()));
  }

  private List<LotsDto> getLotsDtos(List<Pair<UUID, CalculatedStockOnHandByLocation>> locationPairs,
      Map<UUID, StockCard> stockCardIdToStockCard, boolean needInitiallyMoveProduct) {
    List<UUID> lotIds = locationPairs.stream()
        .map(Pair::getFirst)
        .map(stockCardIdToStockCard::get)
        .map(StockCard::getLotId)
        .collect(Collectors.toList());
    LotSearchParams requestParams = new LotSearchParams(null, null, null, lotIds);
    List<LotDto> lots = lotController.getLots(requestParams, null).getContent();
    Map<UUID, LotDto> idToLot = lots.stream().collect(Collectors.toMap(LotDto::getId, Function.identity()));

    List<LotsDto> lotDtoList = new LinkedList<>();
    locationPairs.forEach(locationPair -> {
      StockCard stockCard = stockCardIdToStockCard.get(locationPair.getFirst());
      UUID lotId = stockCard.getLotId();
      LotDto lot = null;
      if (null != lotId) {
        lot = idToLot.get(lotId);
      }
      if (lot != null && !lot.isActive() && !needInitiallyMoveProduct) {
        return;
      }
      lotDtoList.add(LotsDto.builder()
          .lotId(lotId)
          .orderableId(stockCard.getOrderableId())
          .lotCode(lot == null ? null : lot.getLotCode())
          .stockOnHand(locationPair.getSecond().getStockOnHand())
          .expirationDate(lot == null ? null : lot.getExpirationDate())
          .build());
    });
    return lotDtoList;
  }

  private List<FacilityLocationsDto> convertToDto(List<FacilityLocations> locations) {
    return locations.stream().map(location -> FacilityLocationsDto.builder()
        .facilityId(location.getFacilityId())
        .area(location.getArea())
        .locationCode(location.getLocationCode())
        .build()).collect(Collectors.toList());
  }
}
