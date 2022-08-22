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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.LotLocationDto;
import org.siglus.siglusapi.dto.LotLocationPair;
import org.siglus.siglusapi.dto.LotsDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
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

  public List<LotLocationDto> searchLotLocationDtos(List<UUID> orderableIds, boolean extraData) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    if (!extraData) {
      return getLocationsByFacilityId(facilityId);
    }
    return getLotLocationDtos(orderableIds, facilityId);
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

  private List<LotLocationDto> getLotLocationDtos(List<UUID> orderableIds, UUID facilityId) {
    List<LotLocationDto> lotLocationDtos = new LinkedList<>();
    orderableIds.forEach(orderableId -> {

      Map<String, List<LotLocationPair>> locationCodeToLotLocationPairs = getLocationCodeToLotLocationPairListMap(
          facilityId, orderableId);

      locationCodeToLotLocationPairs.forEach((locationCode, locationPairs) -> {
        List<LotsDto> lotDtoList = getLotsDtos(orderableId, locationPairs);
        lotLocationDtos.add(LotLocationDto
            .builder()
            .locationCode(locationCode)
            .area(locationPairs.get(0).getCalculatedStockOnHandByLocation().getArea())
            .lots(lotDtoList)
            .build());
      });

    });
    return lotLocationDtos;
  }


  private Map<String, List<LotLocationPair>> getLocationCodeToLotLocationPairListMap(UUID facilityId,
      UUID orderableId) {
    List<StockCard> stockCardList = stockCardRepository.findByFacilityIdAndOrderableId(facilityId,
        orderableId);

    Map<UUID, List<CalculatedStockOnHandByLocation>> lotIdToLocationSohListMap = new HashMap<>();
    stockCardList.forEach(stockCard -> {
      List<CalculatedStockOnHandByLocation> recentlyLocationSohList =
          calculatedStockOnHandByLocationRepository.findRecentlyLocationSohByStockCardId(stockCard.getId());
      if (!recentlyLocationSohList.isEmpty()) {
        lotIdToLocationSohListMap.put(stockCard.getLotId(), recentlyLocationSohList);
      }
    });

    return reverseMappingRelationship(lotIdToLocationSohListMap);
  }

  private Map<String, List<LotLocationPair>> reverseMappingRelationship(
      Map<UUID, List<CalculatedStockOnHandByLocation>> lotIdToLocationIdMap) {
    List<LotLocationPair> lotLocationPairs = new LinkedList<>();

    lotIdToLocationIdMap.forEach((lotId, locationStockOnHandList)
        -> locationStockOnHandList.forEach(
          locationsStockOnHand -> lotLocationPairs.add(LotLocationPair.builder().lotId(lotId)
            .calculatedStockOnHandByLocation(locationsStockOnHand)
            .build())));

    return lotLocationPairs.stream()
        .collect(Collectors.groupingBy(e -> e.getCalculatedStockOnHandByLocation().getLocationCode()));
  }

  private List<LotsDto> getLotsDtos(UUID orderableId, List<LotLocationPair> locationPairs) {
    List<LotsDto> lotDtoList = new LinkedList<>();

    locationPairs.forEach(locationPair -> {
      UUID lotId = locationPair.getLotId();
      Lot lot = lotRepository.findOne(lotId);
      lotDtoList.add(LotsDto
          .builder()
          .lotId(lotId).orderablesId(orderableId).lotCode(lot.getLotCode())
          .stockOnHand(locationPair
              .getCalculatedStockOnHandByLocation()
              .getStockonhand())
          .expirationDate(lot.getExpirationDate())
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
