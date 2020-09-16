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

import static org.siglus.siglusapi.constant.FcConstants.DUMMY;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.referencedata.Code;
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

  public boolean processRegimenData(List<RegimenDto> dtos) {
    try {
      Map<String, ProgramRealProgram> realProgramMap = programRealProgramRepository.findAll()
          .stream().collect(Collectors.toMap(ProgramRealProgram::getRealProgramCode, p -> p));
      Map<String, UUID> programIdMap = programRefDataService.findAll()
          .stream()
          .collect(Collectors.toMap(ProgramDto::getCode, ProgramDto::getId));
      Map<String, Regimen> regimenMap = regimenRepository.findAll()
          .stream().collect(Collectors.toMap(r -> r.getCode().toString(), r -> r));

      Map<String, RegimenCategory> categoryMap = regimenCategoryRepository.findAll().stream()
          .collect(Collectors.toMap(c -> c.getCode().toString(), c -> c));

      Set<Regimen> regimensToUpdate = new HashSet<>();
      dtos.stream()
          .filter(RegimenDto::isActive)
          .filter(dto -> !DUMMY.equals(dto.getCode()))
          .forEach(dto -> {
            UUID dtoProgramId = programIdMap.get(realProgramMap.get(dto.getAreaCode())
                .getProgramCode());
            if (dtoProgramId == null) {
              log.error("unknown program code: {}", dto.getAreaCode());
            }
            Regimen regimen = regimenMap.get(dto.getCode());
            if (regimen != null) {
              Regimen updateRegimen =
                  compareAndUpdateRegimenData(regimen, dto, dtoProgramId, categoryMap);
              if (updateRegimen != null) {
                regimensToUpdate.add(updateRegimen);
              }
              return;
            }
            regimensToUpdate.add(Regimen.from(dto, dtoProgramId,
                getRegimenCategoryFromDto(dto, categoryMap)));
          });

      regimenRepository.save(regimensToUpdate);
      log.info("save fc regimen successfully, size: {}", dtos.size());
      return true;
    } catch (Exception e) {
      log.error("process fc program data error", e);
      return false;
    }
  }

  private Regimen compareAndUpdateRegimenData(Regimen regimen, RegimenDto dto,
      UUID dtoProgramId,
      Map<String, RegimenCategory> categoryMap) {
    boolean isEqual = true;
    if (!regimen.getName().equals(dto.getDescription())) {
      regimen.setName(dto.getDescription());
      isEqual = false;
    }
    if (!regimen.getProgramId().equals(dtoProgramId)) {
      regimen.setProgramId(dtoProgramId);
      isEqual = false;
    }

    // compare category code
    boolean isCategoryEqual = isEqual(regimen.getRegimenCategory(), dto.getCategoryCode(),
        dto.getCategoryDescription());
    if (!isCategoryEqual && dto.getCategoryCode() != null) {
      // ignore dto category code null
      regimen.setRegimenCategory(
          getRegimenCategoryFromDto(dto, categoryMap));
    }

    if (isEqual && isCategoryEqual) {
      return null;
    }
    return regimen;
  }

  private RegimenCategory getRegimenCategoryFromDto(RegimenDto dto,
      Map<String, RegimenCategory> categoryMap) {
    if (dto.getCategoryCode() == null) {
      return null;
    }
    RegimenCategory dbCategory = categoryMap.get(dto.getCategoryCode());
    if (dbCategory == null) {
      return RegimenCategory
          .builder()
          .code(Code.code(dto.getCategoryCode()))
          .name(dto.getCategoryDescription())
          .build();
    }
    dbCategory.setName(dto.getCategoryDescription());
    return dbCategory;
  }

  private boolean isEqual(RegimenCategory category, String categoryCode,
      String categoryDescription) {
    if (category == null && categoryCode == null && categoryDescription == null) {
      return true;
    } else if (category != null
        && category.getCode().equals(categoryCode)
        && category.getName().equals(categoryDescription)) {
      return true;
    }
    return false;
  }
}
