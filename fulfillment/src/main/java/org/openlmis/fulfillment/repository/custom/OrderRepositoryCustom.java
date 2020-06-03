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

package org.openlmis.fulfillment.repository.custom;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

  Page<Order> searchOrders(OrderSearchParams params, Set<UUID> processingPeriodIds,
      Pageable pageable, Set<UUID> availableSupplyingFacilities,
      Set<UUID> availableRequestingFacilities);

  Page<Order> searchOrders(OrderSearchParams params, Set<UUID> processingPeriodIds,
      Pageable pageable);

  List<UUID> getRequestingFacilities(List<UUID> supplyingFacilityIds);
}
