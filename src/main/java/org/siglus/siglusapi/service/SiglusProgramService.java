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

import static org.siglus.siglusapi.constant.CacheConstants.CACHE_KEY_GENERATOR;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_PROGRAM;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_PROGRAMS;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_PROGRAM_BY_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiglusProgramService {

  private final ProgramReferenceDataService programRefDataService;

  @Cacheable(value = SIGLUS_PROGRAMS, keyGenerator = CACHE_KEY_GENERATOR)
  public List<ProgramDto> getPrograms(String code) {
    if (ALL_PRODUCTS_PROGRAM_CODE.equals(code)) {
      return Collections.singletonList(getAllProgramDto());
    }
    return programRefDataService.findAll().stream()
        .filter(programDto -> code == null || code.equals(programDto.getCode()))
        .collect(Collectors.toList());
  }

  @Cacheable(value = SIGLUS_PROGRAM, keyGenerator = CACHE_KEY_GENERATOR)
  public ProgramDto getProgram(UUID programId) {
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      return getAllProgramDto();
    }
    return programRefDataService.findOne(programId);
  }

  @Cacheable(value = SIGLUS_PROGRAM_BY_CODE, keyGenerator = CACHE_KEY_GENERATOR)
  public Optional<ProgramDto> getProgramByCode(String code) {
    return programRefDataService.findAll().stream().filter(programDto -> programDto.getCode().equals(code)).findAny();
  }

  private ProgramDto getAllProgramDto() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(ALL_PRODUCTS_PROGRAM_ID);
    programDto.setCode(ALL_PRODUCTS_PROGRAM_CODE);
    programDto.setName(ALL_PRODUCTS_PROGRAM_NAME);
    return programDto;
  }
}
