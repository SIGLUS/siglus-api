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

import static org.siglus.siglusapi.constant.FcConstants.DEFAULT_REGIMEN_CATEGORY_CODE;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenCategory;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.RegimenDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.RegimenCategoryRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcRegimenService implements ProcessDataService {

  private int maxRegimenCategoryDisplayOrder;

  private RegimenRepository regimenRepository;
  private ProgramRealProgramRepository programRealProgramRepository;
  private ProgramReferenceDataService programRefDataService;
  private RegimenCategoryRepository regimenCategoryRepository;
  private FcIntegrationResultService fcIntegrationResultService;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> regimens, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC regimen] sync count: {}", regimens.size());
    if (regimens.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    AtomicInteger createCounter = new AtomicInteger();
    AtomicInteger updateCounter = new AtomicInteger();
    AtomicInteger sameCounter = new AtomicInteger();
    try {
      Map<String, ProgramRealProgram> realProgramCodeToProgram = programRealProgramRepository.findAll()
          .stream().collect(Collectors.toMap(ProgramRealProgram::getRealProgramCode, Function.identity()));
      Map<String, UUID> programCodeToId = programRefDataService.findAll()
          .stream().collect(Collectors.toMap(ProgramDto::getCode, ProgramDto::getId));
      List<Regimen> allRegimens = regimenRepository.findAll();
      Map<String, Regimen> regimenCodeToRegimen = allRegimens
          .stream().collect(Collectors.toMap(Regimen::getCode, Function.identity()));
      AtomicInteger maxRegimenDisplayOrder = new AtomicInteger(allRegimens.stream()
          .mapToInt(Regimen::getDisplayOrder).max().orElse(0));
      List<RegimenCategory> allCategories = regimenCategoryRepository.findAll();
      Map<String, RegimenCategory> regimenCategoryCodeToCategory = allCategories.stream()
          .collect(Collectors.toMap(RegimenCategory::getCode, Function.identity()));
      maxRegimenCategoryDisplayOrder = allCategories.stream()
          .mapToInt(RegimenCategory::getDisplayOrder).max().orElse(0);
      Set<Regimen> regimensToUpdate = new HashSet<>();
      regimens.forEach(item -> {
        RegimenDto current = (RegimenDto) item;
        ProgramRealProgram realProgram = realProgramCodeToProgram.get(current.getAreaCode());
        if (realProgram == null) {
          log.error("[FC regimen] unknown real program code: {}", current.getAreaCode());
          return;
        }
        UUID realProgramId = realProgram.getId();
        UUID programId = programCodeToId.get(realProgram.getProgramCode());
        if (programId == null) {
          log.error("[FC regimen] unknown program code: {}", current.getAreaCode());
          return;
        }
        Regimen existed = regimenCodeToRegimen.get(current.getCode());
        if (existed == null) {
          log.info("[FC regimen] create regimen: {}", current);
          regimensToUpdate.add(Regimen.from(current, realProgramId, programId,
              getRegimenCategory(current, regimenCategoryCodeToCategory), maxRegimenDisplayOrder.incrementAndGet()));
          createCounter.getAndIncrement();
        } else if (isDifferent(existed, current, realProgram.getId())) {
          log.info("[FC regimen] update regimen, existed: {}, current: {}", existed, current);
          regimensToUpdate.add(merge(existed, current, realProgramId, programId, regimenCategoryCodeToCategory));
          updateCounter.getAndIncrement();
        } else {
          sameCounter.getAndIncrement();
        }
      });
      regimenRepository.save(regimensToUpdate);
    } catch (Exception e) {
      log.error("[FC regimen] sync data error", e);
      finalSuccess = false;
    }
    log.info("[FC regimen] process data create: {}, update: {}, same: {}",
        createCounter.get(), updateCounter.get(), sameCounter.get());
    return buildResult(regimens, startDate, previousLastUpdatedAt, finalSuccess, createCounter.get(),
        updateCounter.get());
  }

  private boolean isDifferent(Regimen existed, RegimenDto current, UUID realProgramId) {
    return !existed.getName().equals(current.getDescription())
        || !existed.getRealProgramId().equals(realProgramId)
        || !isCategoryEquivalent(existed, current);
  }

  private Regimen merge(Regimen existed, RegimenDto current, UUID realProgramId, UUID programId,
      Map<String, RegimenCategory> codeToCategoryMap) {
    existed.setName(current.getDescription());
    existed.setRealProgramId(realProgramId);
    existed.setProgramId(programId);
    existed.setRegimenCategory(getRegimenCategory(current, codeToCategoryMap));
    return existed;
  }

  private RegimenCategory getRegimenCategory(RegimenDto dto,
      Map<String, RegimenCategory> codeToCategoryMap) {
    if (dto.getCategoryCode() == null) {
      // return default category
      return codeToCategoryMap.get(DEFAULT_REGIMEN_CATEGORY_CODE);
    }
    RegimenCategory dbCategory = codeToCategoryMap.get(dto.getCategoryCode());
    if (dbCategory == null) {
      return RegimenCategory
          .builder()
          .code(dto.getCategoryCode())
          .name(dto.getCategoryDescription())
          .displayOrder(++maxRegimenCategoryDisplayOrder)
          .build();
    }
    dbCategory.setName(dto.getCategoryDescription());
    return dbCategory;
  }


  private boolean isCategoryEquivalent(Regimen regimen, RegimenDto dto) {
    RegimenCategory category = regimen.getRegimenCategory();
    if ((category == null || DEFAULT_REGIMEN_CATEGORY_CODE.equals(category.getCode()))
        && dto.getCategoryCode() == null && dto.getCategoryDescription() == null) {
      return true;
    }

    return category != null
        && category.getCode().equals(dto.getCategoryCode())
        && category.getName().equals(dto.getCategoryDescription());
  }

}
