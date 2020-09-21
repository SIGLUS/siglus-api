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

package org.siglus.siglusapi.service.fc;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.fc.ProgramDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.util.FcUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcProgramService {

  @Autowired
  private ProgramRealProgramRepository programRealProgramRepository;

  public boolean processPrograms(List<ProgramDto> dtos) {
    if (isEmpty(dtos)) {
      return false;
    }
    try {
      Map<String, ProgramRealProgram> programMap = programRealProgramRepository.findAll()
          .stream().collect(Collectors.toMap(ProgramRealProgram::getRealProgramCode, p -> p));

      Set<ProgramRealProgram> programsToUpdate = new HashSet<>();
      dtos.forEach(dto -> {
        ProgramRealProgram program = programMap.get(dto.getCode());
        if (program != null) {
          ProgramRealProgram updateProgram = compareAndUpdateProgramData(program, dto);
          if (updateProgram != null) {
            programsToUpdate.add(updateProgram);
          }
          return;
        }
        programsToUpdate.add(ProgramRealProgram.from(dto));
      });

      programRealProgramRepository.save(programsToUpdate);
      log.info("save fc program successfully, size: {}", dtos.size());
      return true;
    } catch (Exception e) {
      log.error("process fc program data error", e);
      return false;
    }
  }

  private ProgramRealProgram compareAndUpdateProgramData(ProgramRealProgram program,
      ProgramDto dto) {
    boolean isEqual = true;
    if (!program.getRealProgramName().equals(dto.getDescription())) {
      program.setRealProgramName(dto.getDescription());
      isEqual = false;
    }

    if (!program.getActive().equals(FcUtilService.isActive(dto.getStatus()))) {
      program.setActive(FcUtilService.isActive(dto.getStatus()));
      isEqual = false;
    }

    if (isEqual) {
      return null;
    }
    return program;
  }
}
