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

import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.constant.PeriodConstants;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.LotLocationDto;
import org.siglus.siglusapi.dto.LotsDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusLotLocationService {

  private final SiglusAuthenticationHelper authenticationHelper;
  private final StockCardRepository stockCardRepository;
  private final FacilityLocationsRepository facilityLocationsRepository;
  private final LotRepository lotRepository;
  private final CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  private final FacilityNativeRepository facilityNativeRepository;
  private final CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  public List<LotLocationDto> searchLotLocationDtos(List<UUID> orderableIds, boolean extraData, boolean isAdjustment) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    if (!extraData) {
      return getLocationsByFacilityId(facilityId);
    }
    return getLotLocationDtos(orderableIds, facilityId, isAdjustment);
  }

  public List<FacilityLocationsDto> searchLocationsByFacility() {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    if (locations.isEmpty()) {
      throw new NotFoundException(ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND);
    }
    return convertToDto(locations);
  }

  private List<LotLocationDto> getLocationsByFacilityId(UUID facilityId) {
    List<LotLocationDto> lotLocationDtos = new LinkedList<>();
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    locations.forEach(location -> lotLocationDtos.add(LotLocationDto
        .builder()
        .locationCode(location.getLocationCode())
        .area(location.getArea())
        .build()));
    return lotLocationDtos;
  }

  private List<LotLocationDto> getLotLocationDtos(List<UUID> orderableIds, UUID facilityId, boolean isAdjustment) {
    List<LotLocationDto> lotLocationDtos = new LinkedList<>();
    orderableIds.forEach(orderableId -> {
      List<StockCard> stockCardList;
      if (isAdjustment) {
        stockCardList = getUnrestrictedAdjustmentStockCards(facilityId, orderableId);
      } else {
        stockCardList = stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId);
      }

      Map<String, List<Pair<UUID, CalculatedStockOnHandByLocation>>> locationCodeToLotLocationPairs =
          getLocationCodeToLotLocationPairListMap(stockCardList);

      Map<UUID, StockCard> stockCardIdToStockCard = Maps.uniqueIndex(stockCardList, StockCard::getId);
      locationCodeToLotLocationPairs.forEach((locationCode, locationPairs) -> {
        List<LotsDto> lotDtoList = getLotsDtos(orderableId, locationPairs, stockCardIdToStockCard);
        lotLocationDtos.add(LotLocationDto
            .builder()
            .locationCode(locationCode)
            .area(locationPairs.get(0).getValue().getArea())
            .lots(lotDtoList)
            .build());
      });

    });
    return lotLocationDtos;
  }


  private Map<String, List<Pair<UUID, CalculatedStockOnHandByLocation>>> getLocationCodeToLotLocationPairListMap(
      List<StockCard> stockCardList) {
    Map<UUID, List<CalculatedStockOnHandByLocation>> stockCardIdToLocationSohListMap = new HashMap<>();
    stockCardList.forEach(stockCard -> {
      List<CalculatedStockOnHandByLocation> recentlyLocationSohList =
          calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardId(stockCard.getId());
      if (!recentlyLocationSohList.isEmpty()) {
        stockCardIdToLocationSohListMap.put(stockCard.getId(), recentlyLocationSohList);
      }
    });

    return reverseMappingRelationship(stockCardIdToLocationSohListMap);
  }

  private List<StockCard> getUnrestrictedAdjustmentStockCards(UUID facilityId, UUID orderableId) {
    List<StockCard> stockCardList = stockCardRepository.findByFacilityIdAndOrderableId(facilityId,
        orderableId);

    Map<UUID, FacilityProgramPeriodScheduleDto> programIdToSchedulesCode = Maps.uniqueIndex(facilityNativeRepository
            .findFacilityProgramPeriodScheduleByFacilityId(facilityId), FacilityProgramPeriodScheduleDto::getProgramId);

    List<StockCard> stockCards = new LinkedList<>();
    stockCardList.forEach(stockCard -> {
      Code schedulesCode = Code.code(programIdToSchedulesCode.get(stockCard.getProgramId()).getSchedulesCode());
      List<CalculatedStockOnHand> calculatedStockOnHands = calculatedStockOnHandRepository
          .findByStockCardIdInAndOccurredDateLessThanEqual(
              Collections.singletonList(stockCard.getId()), LocalDate.now());

      Optional<CalculatedStockOnHand> recentlyCalculatedStockOnHand = calculatedStockOnHands.stream()
          .max(Comparator.comparing(CalculatedStockOnHand::getOccurredDate));
      if (!recentlyCalculatedStockOnHand.isPresent()) {
        return;
      }
      Integer stockOnHand = recentlyCalculatedStockOnHand.get().getStockOnHand();
      if (stockOnHand > 0) {
        stockCards.add(stockCard);
      } else {
        if ((schedulesCode.equals(PeriodConstants.MONTH_SCHEDULE_CODE)
            || schedulesCode.equals(PeriodConstants.REPORT_MONTH_SCHEDULE_CODE))
            && hasSohInMonthRange(calculatedStockOnHands, MONTHLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE)) {
          stockCards.add(stockCard);
        }
        if ((schedulesCode.equals(PeriodConstants.QUARTERLY_SCHEDULE_CODE)
            || schedulesCode.equals(PeriodConstants.REPORT_QUARTERLY_SCHEDULE_CODE))
            && hasSohInMonthRange(calculatedStockOnHands, QUARTERLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE)) {
          stockCards.add(stockCard);
        }
      }

    });
    return stockCards;
  }

  private boolean hasSohInMonthRange(List<CalculatedStockOnHand> calculatedStockOnHandList, int monthRange) {
    return calculatedStockOnHandList.stream()
        .filter(o -> o.getOccurredDate().isAfter(LocalDate.now().minusMonths(monthRange)))
        .anyMatch(o -> o.getStockOnHand() != 0);
  }

  private Map<String, List<Pair<UUID, CalculatedStockOnHandByLocation>>> reverseMappingRelationship(
      Map<UUID, List<CalculatedStockOnHandByLocation>> stockCardIdToLocationIdMap) {

    List<Pair<UUID, CalculatedStockOnHandByLocation>> stockCardLocationPairs = new LinkedList<>();
    stockCardIdToLocationIdMap.forEach((stockCardId, locationStockOnHandList)
        -> locationStockOnHandList.forEach(locationsStockOnHand -> stockCardLocationPairs.add(new Pair<>(
            stockCardId, locationsStockOnHand))));

    return stockCardLocationPairs.stream().collect(Collectors.groupingBy(e -> e.getValue().getLocationCode()));
  }

  private List<LotsDto> getLotsDtos(UUID orderableId, List<Pair<UUID, CalculatedStockOnHandByLocation>> locationPairs,
      Map<UUID, StockCard> stockCardIdToStockCard) {
    List<LotsDto> lotDtoList = new LinkedList<>();
    locationPairs.forEach(locationPair -> {
      UUID lotId = stockCardIdToStockCard.get(locationPair.getKey()).getLotId();
      Lot lot = null;
      if (null != lotId) {
        lot = lotRepository.findOne(lotId);
      }
      lotDtoList.add(LotsDto
          .builder()
          .lotId(lotId).orderableId(orderableId).lotCode(lot == null ? null : lot.getLotCode())
          .stockOnHand(locationPair.getValue().getStockOnHand())
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
