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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.dto.ProgramAdditionalOrderableDto;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SiglusProgramAdditionalOrderableService {

  @Autowired
  private ProgramAdditionalOrderableRepository programAdditionalOrderableRepository;

  public Page<ProgramAdditionalOrderableDto> searchAdditionalOrderables(UUID programId, String code,
      String name, UUID orderableOriginProgramId, Pageable pageable) {
    code = '%' + defaultIfBlank(code, EMPTY).toUpperCase() + '%';
    name = '%' + defaultIfBlank(name, EMPTY).toUpperCase() + '%';
    if (null == orderableOriginProgramId) {
      return programAdditionalOrderableRepository.search(programId, code, name, pageable);
    }
    return programAdditionalOrderableRepository.search(programId, code, name,
        orderableOriginProgramId, pageable);
  }

  @Transactional
  public void deleteAdditionalOrderable(UUID id) {
    ProgramAdditionalOrderable additionalOrderable = programAdditionalOrderableRepository
        .findOne(id);
    if (null == additionalOrderable) {
      return;
    }
    log.info("delete additional orderable by id: {}", id);
    programAdditionalOrderableRepository.delete(additionalOrderable);
  }

  @Transactional
  public void createAdditionalOrderables(List<ProgramAdditionalOrderableDto> dtos) {
    log.info("save additional orderables, size: {}", dtos.size());
    programAdditionalOrderableRepository.save(ProgramAdditionalOrderable.from(dtos));
  }
}
