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

import static org.siglus.common.repository.RepositoryConstants.FROM_ORDERABLES_CLAUSE;
import static org.siglus.common.repository.RepositoryConstants.ORDER_BY_PAGEABLE;
import static org.siglus.common.repository.RepositoryConstants.SELECT_ORDERABLE;
import static org.siglus.common.repository.RepositoryConstants.WHERE_LATEST_ORDERABLE;

import java.util.UUID;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.common.domain.referencedata.VersionIdentity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence repository for saving/finding {@link Orderable}.
 */
public interface OrderableRepository extends JpaRepository<Orderable, VersionIdentity> {
  
  @Query(value = SELECT_ORDERABLE
      + FROM_ORDERABLES_CLAUSE
      + WHERE_LATEST_ORDERABLE
      + " AND o.identity.id IN :ids"
      + ORDER_BY_PAGEABLE,
      countQuery = "SELECT COUNT(1)"
      + FROM_ORDERABLES_CLAUSE
      + WHERE_LATEST_ORDERABLE
      + " AND o.identity.id IN :ids"
      + ORDER_BY_PAGEABLE
  )
  Page<Orderable> findAllLatestByIds(@Param("ids") Iterable<UUID> ids, Pageable pageable);

}
