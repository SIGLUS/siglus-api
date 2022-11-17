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
import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.dto.HfCmmCountDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacilityCmmsRepository extends JpaRepository<HfCmm, UUID> {

  HfCmm findByFacilityCodeAndProductCodeAndPeriodBeginAndPeriodEnd(String facilityCode,
      String productCode, LocalDate periodBegin, LocalDate periodEnd);

  @Modifying
  @Query(value = "delete from siglusintegration.hf_cmms "
      + "where id in ( "
      + "select hf_cmm.id from siglusintegration.hf_cmms hf_cmm "
      + "left join referencedata.facilities facilitie on facilitie.code = hf_cmm.facilitycode "
      + "left join referencedata.orderables orderable on orderable.code = hf_cmm.productcode "
      + "where facilitie.id = :facilityId and orderable.id in (:orderableIds) "
      + ")", nativeQuery = true)
  void deleteHfCmmsByFacilityIdAndProductCode(@Param("facilityId") UUID facilityId,
      @Param("orderableIds") Iterable<UUID> orderableIds);

  @Query(name = "HfCmm.findHfCmmCountDto", nativeQuery = true)
  List<HfCmmCountDto> findAllFacilityCmmCountDtos(@Param("periodStartDates") List<LocalDate> periodStartDates);

  @Query(name = "HfCmm.findOneHfCmmCountDto", nativeQuery = true)
  List<HfCmmCountDto> findOneFacilityCmmCountDtos(@Param("periodStartDates") List<LocalDate> periodStartDates,
      @Param("facilityCode") String facilityCode);
}
