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

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiglusProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, UUID> {

  @Query(value = "select * from fulfillment.proofs_of_delivery p "
      + "join fulfillment.shipments s2 on p.shipmentid = s2.id "
      + "join fulfillment.orders o2 on s2.orderid = o2.id "
      + "where p.status = 'CONFIRMED' "
      + "and p.receiveddate >= :date "
      + "and p.receiveddate < :today "
      + "and p.shipmentid in ("
      + "     select s.id from fulfillment.shipments s "
      + "     join fulfillment.orders o "
      + "     on s.orderid = o.id "
      + "     where o.requestingfacilityid in :requestingFacilityIds) "
      + "order by ?#{#pageable}",
      nativeQuery = true)
  Page<ProofOfDelivery> search(@Param("date") LocalDate date, @Param("today") String today,
      @Param("requestingFacilityIds") Set<UUID> requestingFacilityIds,
      Pageable pageable);
}
