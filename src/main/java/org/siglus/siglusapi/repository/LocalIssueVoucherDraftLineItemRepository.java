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
import org.siglus.siglusapi.domain.LocalIssueVoucherDraftLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalIssueVoucherDraftLineItemRepository extends JpaRepository<LocalIssueVoucherDraftLineItem, UUID> {
  @Query(value = "select\n"
      + "  *\n"
      + "from\n"
      + "  siglusintegration.local_issue_voucher_draft_line_items livdli \n"
      + "where\n"
      + "  orderableid in :orderableIds\n"
      + "  and localissuevoucherid  = :podId\n"
      + "  and localissuevouchersubdraftid  <> :subDraftId", nativeQuery = true)
  List<ProofOfDeliveryLineItem> findDuplicatedOrderableLineItem(@Param("orderableIds") Collection<UUID> orderableIds,
      @Param("podId") UUID podId, @Param("subDraftId") UUID subDraftId);

  @Query(value = "select\n"
      + "  orderableid\n"
      + "from\n"
      + "  siglusintegration.local_issue_voucher_draft_line_items livdli\n"
      + "where\n"
      + "  localissuevoucherid =:podId\n"
      + "group by\n"
      + "  orderableid", nativeQuery = true)
  List<UUID> findUsedOrderableByPodId(@Param("podId") UUID podId);

  void deleteByLocalIssueVoucherSubDraftId(UUID subDraftId);
}
