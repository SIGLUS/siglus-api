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

import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY;
import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY_REPORT_ONLY;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY_REPORT_ONLY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.CalculatedStocksOnHand;
import org.siglus.siglusapi.domain.CalculatedStocksOnHandLocations;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.dto.DisplayedLotDto;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.LotLocationDto;
import org.siglus.siglusapi.dto.LotLocationPair;
import org.siglus.siglusapi.dto.LotsDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.CalculatedStocksOnHandLocationsRepository;
import org.siglus.siglusapi.repository.CalculatedStocksOnHandRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
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
  private CalculatedStocksOnHandLocationsRepository calculatedStocksOnHandLocationsRepository;

  @Autowired
  private FacilityNativeRepository facilityNativeRepository;

  @Autowired
  private CalculatedStocksOnHandRepository calculatedStocksOnHandRepository;

  public List<LotLocationDto> searchLotLocaiton(List<UUID> orderableIds, boolean extraData) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();

    if (!extraData) {
      List<LotLocationDto> lotLocationDtos = new LinkedList<>();
      List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
      locations.forEach(location -> {
        lotLocationDtos.add(LotLocationDto
            .builder()
            .locationCode(location.getLocationCode())
            .area(location.getArea())
            .build());
      });
      return lotLocationDtos;
    }

    List<LotLocationDto> lotLocationDtos = new LinkedList<>();
    orderableIds.forEach(orderableId -> {
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
            .area(locationPairs.get(0).getCalculatedStocksOnHandLocations().getArea())
            .lots(lotDtoList)
            .build());
      });

    });

    return lotLocationDtos;
  }

  public List<DisplayedLotDto> searchDisplayedLots(List<UUID> orderableIds) {

    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();

    Map<UUID, FacilityProgramPeriodScheduleDto> programIdToSchedulesCode = Maps.uniqueIndex(
        facilityNativeRepository.findFacilityProgramPeriodScheduleByFacilityId(
            facilityId), FacilityProgramPeriodScheduleDto::getProgramId);

    List<DisplayedLotDto> displayedLotDtos = new LinkedList<>();
    orderableIds.forEach(orderableId -> {
      List<UUID> lotIds = new LinkedList<>();
      List<StockCard> stockCardList = stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId);
      stockCardList.forEach(stockCard -> {
        String schedulesCode = programIdToSchedulesCode.get(stockCard.getProgramId()).getSchedulesCode();
        List<CalculatedStocksOnHand> calculatedStocksOnHands = calculatedStocksOnHandRepository.findByStockCardId(
            stockCard.getId());

        Optional<CalculatedStocksOnHand> max = calculatedStocksOnHands.stream()
            .max(Comparator.comparing(CalculatedStocksOnHand::getOccurreddate));
        if (!max.isPresent()) {
          return;
        }
        Integer stockonhand = max.get().getStockonhand();
        if (stockonhand > 0) {
          lotIds.add(stockCard.getLotId());
        } else {
          if ((schedulesCode.equals(MONTHLY) || schedulesCode.equals(MONTHLY_REPORT_ONLY))
              && hasSohInMouthRange(calculatedStocksOnHands, 3)) {
            lotIds.add(stockCard.getLotId());
          }
          if ((schedulesCode.equals(QUARTERLY) || schedulesCode.equals(QUARTERLY_REPORT_ONLY))
              && hasSohInMouthRange(calculatedStocksOnHands, 6)) {
            lotIds.add(stockCard.getLotId());
          }
        }

      });
      displayedLotDtos.add(
          DisplayedLotDto.builder().orderableId(orderableId).lotIds(lotIds).build()
      );
    });

    return displayedLotDtos;
  }

  private boolean hasSohInMouthRange(List<CalculatedStocksOnHand> calculatedStocksOnHands, int monthRange) {
    return calculatedStocksOnHands.stream().filter(o -> o.getOccurreddate().after(
        Date.from(LocalDate.now().minusMonths(monthRange).atStartOfDay(ZoneId.systemDefault()).toInstant())))
        .anyMatch(o -> o.getStockonhand() != 0);
  }

  public List<FacilityLocationsDto> searchLocationsByFacility() {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    if (locations.isEmpty()) {
      throw new NotFoundException(ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND);
    }
    return convertToDto(locations);
  }

  private List<FacilityLocationsDto> convertToDto(List<FacilityLocations> locations) {
    return locations.stream().map(location -> {
      return FacilityLocationsDto.builder()
          .facilityId(location.getFacilityId())
          .area(location.getArea())
          .locationCode(location.getLocationCode())
          .build();
    }).collect(Collectors.toList());
  }
}
