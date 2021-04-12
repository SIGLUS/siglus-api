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
import static org.siglus.siglusapi.constant.FcConstants.DEFAULT_REGIMEN_CATEGORY_CODE;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenCategory;
import org.siglus.siglusapi.dto.fc.RegimenDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.RegimenCategoryRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcRegimenService {

  @Autowired
  private RegimenRepository regimenRepository;

  @Autowired
  private ProgramRealProgramRepository programRealProgramRepository;

  @Autowired
  private ProgramReferenceDataService programRefDataService;

  @Autowired
  private RegimenCategoryRepository regimenCategoryRepository;

  private int maxRegimenCategoryDisplayOrder;

  public boolean processRegimens(List<RegimenDto> dtos) {
    if (isEmpty(dtos)) {
      return false;
    }
    try {
      Map<String, ProgramRealProgram> codeToProgramMap = programRealProgramRepository.findAll()
          .stream()
          .collect(Collectors.toMap(ProgramRealProgram::getRealProgramCode, Function.identity()));
      Map<String, UUID> codeToProgramIdMap = programRefDataService.findAll()
          .stream()
          .collect(Collectors.toMap(ProgramDto::getCode, ProgramDto::getId));
      List<Regimen> allRegimens = regimenRepository.findAll();
      Map<String, Regimen> codeToRegimenMap = allRegimens
          .stream().collect(Collectors.toMap(Regimen::getCode, Function.identity()));
      AtomicInteger maxRegimenDisplayOrder = new AtomicInteger(allRegimens.stream()
          .mapToInt(Regimen::getDisplayOrder).max().orElse(0));
      List<RegimenCategory> allCategories = regimenCategoryRepository.findAll();
      Map<String, RegimenCategory> codeToCategoryMap = allCategories.stream()
          .collect(Collectors.toMap(RegimenCategory::getCode, Function.identity()));
      maxRegimenCategoryDisplayOrder = allCategories.stream()
          .mapToInt(RegimenCategory::getDisplayOrder).max().orElse(0);

      Set<Regimen> regimensToUpdate = new HashSet<>();
      dtos.stream()
          .filter(RegimenDto::isActive)
          .forEach(dto -> {
            UUID dtoProgramId = codeToProgramIdMap
                .get(codeToProgramMap.get(dto.getAreaCode()).getProgramCode());
            if (dtoProgramId == null) {
              log.error("[FC] unknown program code: {}", dto.getAreaCode());
              return;
            }
            Regimen regimen = codeToRegimenMap.get(dto.getCode());
            if (regimen != null) {
              if (isDifferent(regimen, dto, dtoProgramId)) {
                regimensToUpdate.add(merge(regimen, dto, dtoProgramId, codeToCategoryMap));
              }
              return;
            }

            regimensToUpdate.add(Regimen.from(dto, dtoProgramId,
                getRegimenCategory(dto, codeToCategoryMap),
                maxRegimenDisplayOrder.incrementAndGet()));
          });
      log.info("[FC] update regimens: {}", regimensToUpdate);
      regimenRepository.save(regimensToUpdate);
      log.info("[FC] save fc regimen successfully, size: {}", dtos.size());
      return true;
    } catch (Exception e) {
      log.error("[FC] process fc regimen data error", e);
      return false;
    }
  }

  private boolean isDifferent(Regimen regimen, RegimenDto dto, UUID dtoProgramId) {
    return !regimen.getName().equals(dto.getDescription())
        || !regimen.getProgramId().equals(dtoProgramId) || !isCategoryEquivalent(regimen, dto);
  }

  private Regimen merge(Regimen regimen, RegimenDto dto, UUID dtoProgramId,
      Map<String, RegimenCategory> codeToCategoryMap) {
    regimen.setName(dto.getDescription());
    regimen.setProgramId(dtoProgramId);
    regimen.setRegimenCategory(getRegimenCategory(dto, codeToCategoryMap));
    return regimen;
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
