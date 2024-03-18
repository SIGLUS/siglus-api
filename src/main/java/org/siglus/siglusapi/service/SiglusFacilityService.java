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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.siglus.siglusapi.dto.FacilityRemovedLotDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.RemovedLotDto;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@AllArgsConstructor
@NoArgsConstructor
public class SiglusFacilityService {
  @Autowired
  private RequisitionGroupMembersRepository requisitionGroupMembersRepository;
  @Autowired
  private SiglusFacilityRepository siglusFacilityRepository;
  @Autowired
  private FacilityConfigHelper facilityConfigHelper;
  @Autowired
  private SiglusLotService siglusLotService;
  @Autowired
  private SiglusStockCardService siglusStockCardService;

  public List<RequisitionGroupMembersDto> searchFacilityRequisitionGroup(UUID id, Set<UUID> programIds) {
    return requisitionGroupMembersRepository.findParentFacilityByRequisitionGroup(id, programIds);
  }

  public Map<UUID, String> getFacilityIdToName(Set<UUID> facilityIds) {
    return siglusFacilityRepository.findFacilityBasicInfoByIds(facilityIds).stream()
        .collect(Collectors.toMap(Facility::getId, Facility::getName));
  }

  public void removeExpiredLots(UUID facilityId, List<FacilityRemovedLotDto> lots) {
    if (ObjectUtils.isEmpty(lots)) {
      return;
    }
    boolean hasLocation = facilityConfigHelper.isLocationManagementEnabled(facilityId);
    if (hasLocation && lots.stream().anyMatch(FacilityRemovedLotDto::hasLocation)) {
      throw new BusinessDataException(Message.createFromMessageKeyStr("Missing Location"));
    }
    List<UUID> stockCardIds = lots.stream().map(FacilityRemovedLotDto::getStockCardId).collect(Collectors.toList());
    Map<UUID, StockCard> stockCardMap = siglusStockCardService.findStockCardByIds(stockCardIds)
         .stream().collect(Collectors.toMap(StockCard::getId, stockCard -> stockCard));
    List<RemovedLotDto> removedLotDtos = lots.stream().map(lot -> {
      StockCard stockCard = stockCardMap.getOrDefault(lot.getStockCardId(), null);
      if (stockCard == null) {
        throw new BusinessDataException(Message.createFromMessageKeyStr(
                "stock card id doesn't exist " + lot.getStockCardId()));
      }
      if (stockCard.getFacilityId() != facilityId) {
        throw new BusinessDataException(Message.createFromMessageKeyStr(
                "stock card id:" + lot.getStockCardId() + " doesn't belong to facility:" + facilityId));
      }
      return RemovedLotDto.builder().facilityId(stockCard.getFacilityId())
              .programId(stockCard.getProgramId())
              .orderableId(stockCard.getOrderableId())
              .lotId(stockCard.getLotId())
              .area(lot.getArea())
              .locationCode(lot.getLocationCode())
              .quantity(lot.getQuantity())
              .build();
    }).collect(Collectors.toList());
    siglusLotService.removeExpiredLots(removedLotDtos, hasLocation);
  }
}
