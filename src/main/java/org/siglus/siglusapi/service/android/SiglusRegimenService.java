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

package org.siglus.siglusapi.service.android;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.siglusapi.dto.android.response.RegimenResponse;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.service.android.mapper.RegimentMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class SiglusRegimenService {

  private final RegimenRepository repo;
  private final RegimentMapper mapper;
  private final ProgramReferenceDataService programDataService;

  public List<RegimenResponse> getRegimens() {
    Map<UUID, ProgramDto> allPrograms = programDataService.findAll().stream()
        .collect(toMap(BaseDto::getId, Function.identity()));
    return repo.findAll().stream()
        .map(regimen -> mapper.toResponse(regimen, allPrograms))
        .collect(Collectors.toList());
  }

}
