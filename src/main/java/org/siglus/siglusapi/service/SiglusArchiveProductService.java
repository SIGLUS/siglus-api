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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.common.domain.StockCardExtension;
import org.siglus.common.repository.StockCardExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SiglusArchiveProductService {

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private StockCardExtensionRepository stockCardExtensionRepository;

  public void activateArchivedProducts(Collection<UUID> orderableIds, UUID facilityId) {
    Set<UUID> stockCardIds = stockCardRepository
        .findByOrderableIdInAndFacilityId(orderableIds, facilityId)
        .stream()
        .map(StockCard::getId)
        .collect(Collectors.toSet());
    List<StockCardExtension> stockCardExtensions = stockCardExtensionRepository
        .findByStockCardIdIn(stockCardIds);
    stockCardExtensions.forEach(stockCardExtension -> stockCardExtension.setArchived(false));
    stockCardExtensionRepository.save(stockCardExtensions);
  }

  public boolean isArchived(UUID stockCardId) {
    return stockCardExtensionRepository.findByStockCardId(stockCardId).isArchived();
  }

  public boolean isArchived(StockCard stockCard) {
    return isArchived(stockCard.getId());
  }

  public boolean isNotArchived(StockCard stockCard) {
    return !isArchived(stockCard);
  }

  public Set<String> searchArchivedProducts(UUID facilityId) {
    return stockCardExtensionRepository.findArchivedProducts(facilityId);
  }
}
