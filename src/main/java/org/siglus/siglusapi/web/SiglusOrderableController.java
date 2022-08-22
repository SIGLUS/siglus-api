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

package org.siglus.siglusapi.web;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.referencedata.dto.OrderableDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.repository.dto.ProgramOrderableDto;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/orderables")
@RequiredArgsConstructor
public class SiglusOrderableController {

  private final SiglusOrderableService orderableService;
  private final SiglusAuthenticationHelper authenticationHelper;

  @GetMapping
  public Page<OrderableDto> searchOrderables(
      @RequestParam MultiValueMap<String, Object> queryParams, Pageable pageable,
      @RequestParam(required = false) UUID draftId) {
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(queryParams);
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    if (draftId != null) {
      return orderableService
          .searchDeduplicatedOrderables(draftId, searchParams, pageable, facilityId);
    }
    return orderableService.searchOrderables(searchParams, pageable, facilityId);
  }

  @GetMapping("/additionaltoadd")
  public Page<OrderableDto> searchOrderables(@RequestParam UUID programId,
      @RequestParam MultiValueMap<String, Object> queryParams, Pageable pageable) {
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(queryParams);
    return orderableService.additionalToAdd(programId, searchParams, pageable);
  }

  @GetMapping("/price")
  public Map<UUID, BigDecimal> searchOrderablesPrice() {
    return orderableService.getAllProgramOrderableDtos().stream()
        .filter(e -> Objects.nonNull(e.getPrice()))
        .collect(Collectors.toMap(ProgramOrderableDto::getOrderableId, ProgramOrderableDto::getPrice));
  }
}
