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

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.siglus.siglusapi.repository.dto.PodLineItemDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence repository for saving/finding {@link ProofOfDeliveryLineItem}.
 */
public interface PodLineItemsRepository extends JpaRepository<ProofOfDeliveryLineItem, UUID> {

  @Query(name = "PodLineItem.findLineItemDtos", nativeQuery = true)
  List<PodLineItemDto> lineItemDtos(@Param("podId") UUID podId, @Param("orderId") UUID orderId,
      @Param("requisitionId") UUID requisitionId);

  @Query(value = "select\n"
      + "  *\n"
      + "from\n"
      + "  fulfillment.proof_of_delivery_line_items podli\n"
      + "join siglusintegration.pod_line_items_extension plie on plie.podlineitemid = podli.id \n"
      + "where\n"
      + "  podli.orderableid in :orderableIds\n"
      + "  and podli.proofofdeliveryid = :podId\n"
      + "  and plie.subdraftid <> :subDraftId", nativeQuery = true)
  List<ProofOfDeliveryLineItem> findDuplicatedOrderableLineItem(@Param("orderableIds") Collection<UUID> orderableIds,
      @Param("podId") UUID podId, @Param("subDraftId") UUID subDraftId);

  @Query(value = "select\n"
      + "\torderableid\n"
      + "from\n"
      + "\tfulfillment.proof_of_delivery_line_items\n"
      + "where\n"
      + "\tproofofdeliveryid =:podId\n"
      + "group by\n"
      + "\torderableid", nativeQuery = true)
  List<UUID> findUsedOrderableByPodId(@Param("podId") UUID podId);
}
