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

import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.siglusapi.dto.SiglusProgramDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProgramExtensionService {

  @Autowired
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private ProgramReferenceDataService programRefDataService;

  public ProgramExtension findByProgramId(UUID programId) {
    return programExtensionRepository.findByProgramId(programId);
  }

  public List<SiglusProgramDto> getPrograms(/*@Nullable*/ String code) {
    if (ALL_PRODUCTS_PROGRAM_CODE.equals(code)) {
      return Collections.singletonList(getAllProgramDto());
    }
    List<SiglusProgramDto> siglusProgramDtos = programRefDataService.findAll().stream()
        .filter(programDto -> code == null || code.equals(programDto.getCode()))
        .map(SiglusProgramDto::from)
        .collect(Collectors.toList());
    Map<UUID, ProgramExtension> programExtensions =
        programExtensionRepository.findAll().stream().collect(Collectors.toMap(
            ProgramExtension::getProgramId,
            programExtension -> programExtension));
    siglusProgramDtos.forEach(siglusProgramDto -> {
      ProgramExtension programExtension = programExtensions.get(siglusProgramDto.getId());
      if (programExtension != null) {
        siglusProgramDto.setIsVirtual(programExtension.getIsVirtual());
        siglusProgramDto.setParentId(programExtension.getParentId());
        siglusProgramDto.setIsSupportEmergency(programExtension.getIsSupportEmergency());
      }
    });
    return siglusProgramDtos;
  }

  public SiglusProgramDto getProgram(UUID programId) {
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      return getAllProgramDto();
    }
    SiglusProgramDto siglusProgramDto = SiglusProgramDto
        .from(programRefDataService.findOne(programId));
    ProgramExtension programExtension = programExtensionRepository.findByProgramId(programId);
    siglusProgramDto.setIsVirtual(programExtension.getIsVirtual());
    siglusProgramDto.setParentId(programExtension.getParentId());
    siglusProgramDto.setIsSupportEmergency(programExtension.getIsSupportEmergency());
    return siglusProgramDto;
  }

  private SiglusProgramDto getAllProgramDto() {
    SiglusProgramDto siglusProgramDto = new SiglusProgramDto();
    siglusProgramDto.setId(ALL_PRODUCTS_PROGRAM_ID);
    siglusProgramDto.setCode(ALL_PRODUCTS_PROGRAM_CODE);
    siglusProgramDto.setName(ALL_PRODUCTS_PROGRAM_NAME);
    return siglusProgramDto;
  }
}
