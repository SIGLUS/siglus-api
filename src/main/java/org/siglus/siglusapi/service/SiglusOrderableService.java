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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.web.OrderableSearchParams;
import org.openlmis.referencedata.web.QueryOrderableSearchParams;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusOrderableService {

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private StockCardExtensionRepository stockCardExtensionRepository;

  public Page<OrderableDto> searchOrderables(QueryOrderableSearchParams searchParams,
      Pageable pageable) {
    Map<UUID, ProgramExtension> programExtensions =
        programExtensionRepository.findAll().stream().collect(Collectors.toMap(
            ProgramExtension::getProgramId,
            programExtension -> programExtension));
    Page<OrderableDto> orderableDtoPage = orderableReferenceDataService
        .searchOrderables(searchParams, pageable);
    orderableDtoPage.getContent().forEach(orderableDto -> orderableDto.getPrograms().forEach(
        programOrderableDto -> programOrderableDto
            .setParentId(programExtensions.get(programOrderableDto.getProgramId()).getParentId())));
    return orderableDtoPage;
  }

  public Page<OrderableDto> searchOrderables(OrderableSearchParams searchParams, UUID facilityId) {
    Page<OrderableDto> orderableDtoPage = orderableReferenceDataService
        .searchOrderables(searchParams);
    Set<UUID> stockCardIds = stockCardRepository
        .findByFacilityId(facilityId)
        .stream()
        .map(StockCard::getId)
        .collect(Collectors.toSet());
    Map<UUID, StockCardExtension> stockCardIdStockCardExtensionMap = stockCardExtensionRepository
        .findByStockCardIdIn(stockCardIds)
        .stream()
        .collect(Collectors.toMap(StockCardExtension::getStockCardId,
            stockCardExtension -> stockCardExtension));
    Map<UUID, UUID> orderableIdStockCardIdMap = stockCardRepository
        .findByFacilityId(facilityId)
        .stream()
        .collect(Collectors.toMap(StockCard::getOrderableId, StockCard::getId, (v1, v2) -> v1));
    orderableDtoPage.getContent().forEach(orderableDto -> {
      UUID stockCardId = orderableIdStockCardIdMap.get(orderableDto.getId());
      StockCardExtension stockCardExtension = stockCardIdStockCardExtensionMap.get(stockCardId);
      if (null == stockCardExtension) {
        orderableDto.setArchived(false);
        return;
      }
      orderableDto.setArchived(stockCardExtension.isArchived());
    });
    return orderableDtoPage;
  }
}
