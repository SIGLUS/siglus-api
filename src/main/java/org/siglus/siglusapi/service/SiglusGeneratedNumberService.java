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

package org.siglus.siglusapi.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.GeneratedNumber;
import org.siglus.siglusapi.repository.GeneratedNumberRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusGeneratedNumberService {

  private final GeneratedNumberRepository generatedNumberRepository;

  public synchronized Integer getGeneratedNumber(UUID facilityId, UUID programId, int year, boolean emergency) {
    GeneratedNumber generatedNumber = generatedNumberRepository.findByFacilityIdAndProgramIdAndYearAndEmergency(
        facilityId, programId, year, emergency);
    if (generatedNumber == null) {
      generatedNumber = GeneratedNumber.builder()
          .facilityId(facilityId)
          .programId(programId)
          .year(year)
          .emergency(emergency)
          .number(1)
          .build();
    } else {
      generatedNumber.setNumber(generatedNumber.getNumber() + 1);
    }
    log.info("save generated number: {}", generatedNumber);
    return generatedNumberRepository.save(generatedNumber).getNumber();
  }

  public synchronized void revertGeneratedNumber(UUID facilityId, UUID programId, int year, boolean emergency) {
    GeneratedNumber generatedNumber = generatedNumberRepository.findByFacilityIdAndProgramIdAndYearAndEmergency(
        facilityId, programId, year, emergency);
    if (generatedNumber == null) {
      return;
    }
    generatedNumber.setNumber(generatedNumber.getNumber() - 1);
    log.info("revert generated number: {}", generatedNumber);
    generatedNumberRepository.save(generatedNumber);
  }
}
