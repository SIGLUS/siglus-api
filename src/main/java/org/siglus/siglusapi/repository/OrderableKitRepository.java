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
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.VersionIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderableKitRepository extends
    JpaRepository<Orderable, VersionIdentity> {

  @Query(value = "SELECT o.* "
      + "         FROM referencedata.orderables o"
      + " WHERE EXISTS ("
      + "    SELECT 1 FROM referencedata.orderable_children children "
      + "             WHERE o.id = children.parentid "
      + "             AND o.versionnumber = children.orderableversionnumber )",
      nativeQuery = true
  )
  List<Orderable> findAllKitProduct();

}
