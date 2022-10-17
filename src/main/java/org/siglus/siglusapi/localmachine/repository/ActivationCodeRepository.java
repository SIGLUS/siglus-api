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

package org.siglus.siglusapi.localmachine.repository;

import java.util.Optional;
import java.util.UUID;
import org.siglus.siglusapi.localmachine.domain.ActivationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivationCodeRepository extends JpaRepository<ActivationCode, UUID> {

  Optional<ActivationCode> findFirstByFacilityCodeAndActivationCode(String facilityCode, String activationCode);

  @Query(value = "select activationcode "
      + "from localmachine.activation_codes "
      + "where facilitycode=:facilityCode "
      + "and used=false", nativeQuery = true)
  String findUsableAcivationCode(@Param("facilityCode") String facilityCode);
}
