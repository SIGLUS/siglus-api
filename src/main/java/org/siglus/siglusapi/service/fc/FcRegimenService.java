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

import static com.google.common.collect.Maps.newHashMap;
import static org.siglus.siglusapi.constant.FcConstants.DEFAULT_REGIMEN_CATEGORY_NAME;
import static org.siglus.siglusapi.constant.FcConstants.REGIMEN_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.domain.CustomProductsRegimens;
import org.siglus.siglusapi.domain.FcIntegrationChanges;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenCategory;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.RegimenDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.CustomProductsRegimensRepository;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.RegimenCategoryRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcRegimenService implements ProcessDataService {

  private int maxRegimenCategoryDisplayOrder;

  private final RegimenRepository regimenRepository;
  private final ProgramRealProgramRepository programRealProgramRepository;
  private final ProgramReferenceDataService programRefDataService;
  private final RegimenCategoryRepository regimenCategoryRepository;

  private final CustomProductsRegimensRepository customProductsRegimensRepository;

  private final Map<String, CustomProductsRegimens> codeToCustomProductsRegimens = newHashMap();

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
    List<FcIntegrationChanges> fcIntegrationChangesList = new ArrayList<>();
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
      Map<String, RegimenCategory> nameToRegimenCategory = allCategories.stream()
          .collect(Collectors.toMap(RegimenCategory::getName, Function.identity()));
      maxRegimenCategoryDisplayOrder = allCategories.stream()
          .mapToInt(RegimenCategory::getDisplayOrder).max().orElse(0);
      Set<Regimen> regimensToUpdate = new HashSet<>();

      List<CustomProductsRegimens> customProductsRegimens = customProductsRegimensRepository.findAll();
      customProductsRegimens.forEach(item -> codeToCustomProductsRegimens.put(item.getCode(), item));

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
              getRegimenCategoryFromCustomProductsRegimensRepository(current,
                  nameToRegimenCategory, codeToCustomProductsRegimens),
              maxRegimenDisplayOrder.incrementAndGet()));
          createCounter.getAndIncrement();
          FcIntegrationChanges createChanges = FcUtil
              .buildCreateFcIntegrationChanges(REGIMEN_API, current.getCode(), current.toString());
          fcIntegrationChangesList.add(createChanges);
        } else {
          FcIntegrationChanges updateChanges = getUpdatedRegimen(existed, current, realProgram.getId(), programId);
          if (updateChanges != null) {
            log.info("[FC regimen] update regimen, existed: {}, current: {}", existed, current);
            regimensToUpdate.add(merge(existed, current, realProgramId, programId, nameToRegimenCategory));
            updateCounter.getAndIncrement();
            fcIntegrationChangesList.add(updateChanges);
          } else {
            sameCounter.getAndIncrement();
          }
        }
      });
      regimenRepository.save(regimensToUpdate);
    } catch (Exception e) {
      log.error("[FC regimen] process data error", e);
      finalSuccess = false;
    }
    log.info("[FC regimen] process data create: {}, update: {}, same: {}",
        createCounter.get(), updateCounter.get(), sameCounter.get());
    return buildResult(
        new FcIntegrationResultBuildDto(REGIMEN_API, regimens, startDate, previousLastUpdatedAt, finalSuccess,
            createCounter.get(), updateCounter.get(), null, fcIntegrationChangesList));
  }

  private FcIntegrationChanges getUpdatedRegimen(Regimen existed, RegimenDto current, UUID realProgramId,
      UUID programId) {
    boolean isSame = true;
    StringBuilder updateContent = new StringBuilder();
    StringBuilder originContent = new StringBuilder();
    if (!existed.getName().equals(current.getDescription())) {
      updateContent.append("name=").append(current.getDescription()).append("; ");
      originContent.append("name=").append(existed.getName()).append("; ");
      isSame = false;
    }
    if (!existed.getProgramId().equals(programId)) {
      updateContent.append("programId=").append(programId).append("; ");
      originContent.append("programId=").append(existed.getProgramId()).append("; ");
      isSame = false;
    }
    if (!existed.getRealProgramId().equals(realProgramId)) {
      updateContent.append("realProgramId=").append(realProgramId).append("; ");
      originContent.append("realProgramId=").append(existed.getRealProgramId()).append("; ");
      isSame = false;
    }
    if (!isCategoryEquivalent(existed, current)) {
      updateContent.append("category=").append(getCurrentRegimenCategoryCode(current)).append("; ");
      originContent.append("category=")
          .append(existed.getRegimenCategory() == null ? null : existed.getRegimenCategory().getCode()).append("; ");
      isSame = false;
    }

    if (isSame) {
      return null;
    }
    return FcUtil.buildUpdateFcIntegrationChanges(REGIMEN_API, current.getCode(), updateContent.toString(),
        originContent.toString());
  }

  private String getCurrentRegimenCategoryCode(RegimenDto current) {
    CustomProductsRegimens customProductsRegimens = codeToCustomProductsRegimens.get(current.getCode());
    if (null != customProductsRegimens) {
      return Objects.equals(customProductsRegimens.getCategoryType(), "Adult")
          ? "ADULTS" : customProductsRegimens.getCategoryType();
    }
    return current.getCategoryCode() == null ? "DEFAULT" : current.getCategoryCode();
  }

  @SuppressWarnings("java:S125")
  private Regimen merge(Regimen existed, RegimenDto current, UUID realProgramId, UUID programId,
      Map<String, RegimenCategory> codeToCategoryMap) {
    existed.setName(current.getDescription());
    existed.setRealProgramId(realProgramId);
    existed.setProgramId(programId);
    // do not update regimen category as FC don't provide the value, we fill the value by script
    // existed.setRegimenCategory(getRegimenCategory(current, codeToCategoryMap));
    existed.setRegimenCategory(getRegimenCategoryFromCustomProductsRegimensRepository(current,
        codeToCategoryMap, codeToCustomProductsRegimens));
    return existed;
  }

  private RegimenCategory getRegimenCategoryFromCustomProductsRegimensRepository(RegimenDto dto,
      Map<String, RegimenCategory> nameToRegimenCategory,
      Map<String, CustomProductsRegimens> codeToCustomProductsRegimens) {
    CustomProductsRegimens customProductsRegimensByCode = codeToCustomProductsRegimens.get(dto.getCode());
    if (null != customProductsRegimensByCode) {
      return nameToRegimenCategory.get(customProductsRegimensByCode.getCategoryType());
    }
    return getRegimenCategory(dto, nameToRegimenCategory);
  }

  private RegimenCategory getRegimenCategory(RegimenDto dto,
      Map<String, RegimenCategory> nameToRegimenCategory) {
    if (dto.getCategoryCode() == null) {
      // return default category
      return nameToRegimenCategory.get(DEFAULT_REGIMEN_CATEGORY_NAME);
    }
    RegimenCategory dbCategory = nameToRegimenCategory.get(dto.getCategoryDescription());
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
    if (category == null
        && dto.getCategoryCode() == null && dto.getCategoryDescription() == null) {
      return true;
    }

    return category != null && category.getCode().equalsIgnoreCase(getCurrentRegimenCategoryCode(dto));
  }

}
