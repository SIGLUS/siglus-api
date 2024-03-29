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

import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.FcIntegrationChanges;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ProgramDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcProgramService implements ProcessDataService {

  private final ProgramRealProgramRepository programRealProgramRepository;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> programs, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC program] sync count: {}", programs.size());
    if (programs.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    AtomicInteger createCounter = new AtomicInteger();
    AtomicInteger updateCounter = new AtomicInteger();
    AtomicInteger sameCounter = new AtomicInteger();
    List<FcIntegrationChanges> fcIntegrationChangesList = new ArrayList<>();
    try {
      Map<String, ProgramRealProgram> programCodeToProgram = programRealProgramRepository.findAll()
          .stream().collect(Collectors.toMap(ProgramRealProgram::getRealProgramCode, p -> p));
      Set<ProgramRealProgram> programsToUpdate = new HashSet<>();
      programs.forEach(item -> {
        ProgramDto current = (ProgramDto) item;
        ProgramRealProgram existed = programCodeToProgram.get(current.getCode());
        if (existed == null) {
          log.info("[FC program] create: {}", current);
          programsToUpdate.add(ProgramRealProgram.from(current));
          createCounter.getAndIncrement();
          FcIntegrationChanges createChanges = FcUtil
              .buildCreateFcIntegrationChanges(PROGRAM_API, current.getCode(), current.toString());
          fcIntegrationChangesList.add(createChanges);
        } else {
          FcIntegrationChanges updateChanges = getUpdatedProgram(existed, current);
          if (updateChanges != null) {
            log.info("[FC program] update, existed: {}, current: {}", existed, current);
            programsToUpdate.add(getUpdateProgram(existed, current));
            updateCounter.getAndIncrement();
            fcIntegrationChangesList.add(updateChanges);
          } else {
            sameCounter.getAndIncrement();
          }
        }
      });
      programRealProgramRepository.save(programsToUpdate);
    } catch (Exception e) {
      log.error("[FC program] process data error", e);
      finalSuccess = false;
    }
    log.info("[FC program] process data create: {}, update: {}, same: {}",
        createCounter.get(), updateCounter.get(), sameCounter.get());
    return buildResult(
        new FcIntegrationResultBuildDto(PROGRAM_API, programs, startDate, previousLastUpdatedAt, finalSuccess,
            createCounter.get(), updateCounter.get(), null, fcIntegrationChangesList));
  }

  private FcIntegrationChanges getUpdatedProgram(ProgramRealProgram existed, ProgramDto current) {
    boolean isSame = true;
    StringBuilder updateContent = new StringBuilder();
    StringBuilder originContent = new StringBuilder();
    if (!existed.getRealProgramName().equals(current.getDescription())) {
      log.info("[FC program] name different, existed: {}, current: {}", existed.getRealProgramName(),
          current.getDescription());
      updateContent.append("name=").append(current.getDescription()).append('\n');
      originContent.append("name=").append(existed.getRealProgramName()).append('\n');
      isSame = false;
    }
    if (!existed.getActive().equals(FcUtil.isActive(current.getStatus()))) {
      existed.setActive(FcUtil.isActive(current.getStatus()));
      log.info("[FC program] status different, existed: {}, current: {}", existed.getActive(), current.getStatus());
      updateContent.append("status=").append(FcUtil.isActive(current.getStatus())).append('\n');
      originContent.append("status=").append(existed.getActive()).append('\n');
      isSame = false;
    }
    if (isSame) {
      return null;
    }
    return FcUtil.buildUpdateFcIntegrationChanges(PROGRAM_API, current.getCode(), updateContent.toString(),
        originContent.toString());
  }

  private ProgramRealProgram getUpdateProgram(ProgramRealProgram program, ProgramDto dto) {
    if (!program.getRealProgramName().equals(dto.getDescription())) {
      program.setRealProgramName(dto.getDescription());
    }
    if (!program.getActive().equals(FcUtil.isActive(dto.getStatus()))) {
      program.setActive(FcUtil.isActive(dto.getStatus()));
    }
    return program;
  }
}
