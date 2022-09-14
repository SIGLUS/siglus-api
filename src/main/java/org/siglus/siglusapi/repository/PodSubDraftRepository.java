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
import java.util.UUID;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PodSubDraftRepository extends JpaRepository<PodSubDraft, UUID> {

  @Modifying
  @Query(value = "delete from siglusintegration.pod_sub_draft where id in (:ids);",
      nativeQuery = true)
  void deleteAllByIds(@Param("ids") Iterable<UUID> ids);

  @Query(value =
      "select CAST(o.id AS VARCHAR) as orderId "
          + "from fulfillment.orders o "
          + "inner join fulfillment.shipments s on (o.id = s.orderid) and o.id in (:orderIds) "
          + "inner join fulfillment.proofs_of_delivery p on (s.id = p.shipmentid) "
          + "inner join siglusintegration.pod_sub_draft psd on (p.id = psd.proofofdeliveryid)",
      nativeQuery = true)
  List<String> findOrderIdsWithSubDraft(@Param("orderIds") Iterable<UUID> orderIds);

  void deleteAllByPodId(UUID id);

  int countAllByPodId(UUID localIssueVoucherId);

  List<PodSubDraft> findAllByPodId(UUID podId);
}
