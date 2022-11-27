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

import java.util.Set;
import java.util.UUID;
import org.siglus.siglusapi.domain.RegimenOrderable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegimenOrderableRepository extends JpaRepository<RegimenOrderable, UUID> {

  Set<RegimenOrderable> findByRegimenCodeIn(Set<String> regimenCodes);

  @Query(value = "select * from siglusintegration.regimen_orderables "
      + "where mappingkey in :mappingKeys ", nativeQuery = true)
  Set<RegimenOrderable> findByMappingKeyIn(@Param("mappingKeys") Set<String> mappingKeys);

  @Query(value = "select distinct ro.mappingKey "
      + "from siglusintegration.regimen_orderables ro "
      + "where ro.mappingKey is not null ", nativeQuery = true)
  Set<String> findByExistedMappingKey();
}
