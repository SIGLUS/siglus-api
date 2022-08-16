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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.CalculatedStocksOnHandLocations;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.dto.LotLocationDto;
import org.siglus.siglusapi.dto.LotLocationPair;
import org.siglus.siglusapi.dto.LotsDto;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.SiglusCalculatedStocksOnHandLocationsRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SiglusLotLocationService {

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private FacilityLocationsRepository facilityLocationsRepository;

  @Autowired
  private LotRepository lotRepository;

  @Autowired
  private SiglusCalculatedStocksOnHandLocationsRepository calculatedStocksOnHandLocationsRepository;

  public List<LotLocationDto> searchLotLocaiton(List<UUID> orderbalesId, boolean extraData) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();

    if (!extraData) {
      List<LotLocationDto> lotLocationDtos = new LinkedList<>();
      List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
      locations.forEach(location -> {
        lotLocationDtos.add(LotLocationDto
            .builder()
            .locationCode(location.getLocationCode())
            .build());
      });
      return lotLocationDtos;
    }

    List<LotLocationDto> lotLocationDtos = new LinkedList<>();
    orderbalesId.forEach(orderableId -> {
      List<StockCard> stockCardList = stockCardRepository.findByFacilityIdAndOrderableId(facilityId,
          orderableId);

      Map<UUID, List<CalculatedStocksOnHandLocations>> lotIdToLocationIdMap = new HashMap<>();
      stockCardList.forEach(stockCard -> {
        List<CalculatedStocksOnHandLocations> locationStockOnHandList = calculatedStocksOnHandLocationsRepository
            .findByStockCardId(stockCard.getId());
        lotIdToLocationIdMap.put(stockCard.getLotId(), locationStockOnHandList);
      });

      List<LotLocationPair> lotLocationPairs = new LinkedList<>();

      lotIdToLocationIdMap.forEach((lotId, locationStockOnHandList) -> {
        locationStockOnHandList.forEach(locationsStockOnHand -> {
          lotLocationPairs.add(LotLocationPair.builder().lotId(lotId)
              .calculatedStocksOnHandLocations(locationsStockOnHand)
              .build());
        });
      });

      Map<String, List<LotLocationPair>> locationCodeToPairs = lotLocationPairs.stream()
          .collect(Collectors.groupingBy(e -> e.getCalculatedStocksOnHandLocations().getLocationCode()));

      locationCodeToPairs.forEach((locationCode, locationPairs) -> {
        List<LotsDto> lotDtoList = new LinkedList<>();

        locationPairs.forEach(locationPair -> {
          UUID lotId = locationPair.getLotId();
          Lot lot = lotRepository.findOne(lotId);
          lotDtoList.add(LotsDto
              .builder()
              .lotId(lotId).orderablesId(orderableId).lotCode(lot.getLotCode())
              .stockOnHand(locationPair
                  .getCalculatedStocksOnHandLocations()
                  .getStockonhand())
              .expirationDate(lot.getExpirationDate())
              .build());
        });
        lotLocationDtos.add(LotLocationDto
            .builder()
            .locationCode(locationCode)
            .lots(lotDtoList)
            .build());
      });

    });

    return lotLocationDtos;
  }
}
