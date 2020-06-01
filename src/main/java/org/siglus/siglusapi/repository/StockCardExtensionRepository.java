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

package org.siglus.siglusapi.repository;

import java.util.Set;
import java.util.UUID;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockCardExtensionRepository extends JpaRepository<StockCardExtension, UUID> {

  StockCardExtension findByStockCardId(UUID stockCardId);

  @Query(value = "select cast(orderableId as varchar) orderableId "
      + "from stockmanagement.stock_cards sc, "
      + "siglusintegration.stock_card_extension sce "
      + "where sc.id = sce.stockcardid "
      + "and sc.facilityId = :facilityId "
      + "and sce.archived = true", nativeQuery = true)
  Set<String> findArchivedProducts(@Param("facilityId") UUID facilityId);
}
