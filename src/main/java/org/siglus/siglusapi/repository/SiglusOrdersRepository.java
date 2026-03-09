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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.siglus.siglusapi.dto.FacilitySupplyingProjection;
import org.siglus.siglusapi.repository.dto.OrderDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence repository for saving/finding {@link Order}.
 */
public interface SiglusOrdersRepository extends JpaRepository<Order, UUID> {

  @Query(name = "Order.findOrderDto", nativeQuery = true)
  OrderDto findOrderDtoById(@Param("orderId") UUID orderId);

  Order findByOrderCode(String orderCode);

  List<Order> findAllByExternalIdIn(Set<UUID> externalIds);

  List<Order> findBySupplyingFacilityIdAndProgramIdAndStatusIn(UUID supplyingFacilityId, UUID programId,
      List<OrderStatus> statuses);

  @Query(value = "SELECT DISTINCT CAST(o.supplyingfacilityid AS varchar) AS id, f.name AS name "
      + "FROM fulfillment.orders o "
      + "LEFT JOIN referencedata.facilities f  "
      + "       ON o.supplyingfacilityid = f.id "
      + "WHERE o.requestingfacilityid = :requestingFacilityId "
      + "AND (CAST(:programId AS text) IS NULL OR CAST(o.programid AS text) = CAST(:programId AS text)) ",
      nativeQuery = true)
  List<FacilitySupplyingProjection> findSupplyingFacilities(
      @Param("requestingFacilityId") UUID requestingFacilityId,
      @Param("programId") String programId);
}