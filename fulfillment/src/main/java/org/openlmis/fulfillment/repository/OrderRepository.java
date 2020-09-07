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

package org.openlmis.fulfillment.repository;

import java.util.List;
import java.util.UUID;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.custom.OrderRepositoryCustom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends PagingAndSortingRepository<Order, UUID>,
    OrderRepositoryCustom {

  Order findByOrderCode(@Param("orderCode") String orderNumber);

  Order findByExternalId(@Param("externalId") UUID externalId);

  // [SIGLUS change start]
  // [change reason]: siglus find can Fulfill Order.
  @Query(value = "select * "
      + "from fulfillment.orders "
      + "where status in ('FULFILLING','PARTIALLY_FULFILLED', 'ORDERED')", nativeQuery = true)
  List<Order> findCanFulfillOrder();

  @Query(value = "select * "
      + "from fulfillment.orders "
      + "where status in ('FULFILLING','PARTIALLY_FULFILLED', 'ORDERED')"
      + "and externalId in :externalIds", nativeQuery = true)
  Order findCanFulfillOrderAndInExternalId(@Param("externalIds") List<UUID> externalIds);
  // [SIGLUS change end]
}
