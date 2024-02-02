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

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.fulfillment.domain.ShipmentDraftLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentDraftLineItemsRepository extends JpaRepository<ShipmentDraftLineItem, UUID> {

  List<ShipmentDraftLineItem> findByOrderableIdIn(List<UUID> orderableId);

  @Query(value = "SELECT CAST(t.lotid AS varchar), SUM(t.quantityshipped) "
          + "FROM fulfillment.shipment_draft_line_items t"
          + "WHERE t.lotid IN :lotIds GROUP BY t.lotid",
          nativeQuery = true)
  List<Map.Entry<UUID, BigInteger>> reservedCount(@Param("lotIds") List<UUID> lotIds);
}
