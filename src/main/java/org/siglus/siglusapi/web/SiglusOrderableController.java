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

import java.util.UUID;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.QueryOrderableSearchParams;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/orderables")
public class SiglusOrderableController {

  @Autowired
  private SiglusOrderableService orderableService;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @GetMapping
  public Page<OrderableDto> searchOrderables(
      @RequestParam MultiValueMap<String, Object> queryParams, Pageable pageable) {
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(queryParams);
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    return orderableService.searchOrderables(searchParams, pageable, facilityId);
  }

  @GetMapping("/additionaltoadd")
  public Page<OrderableDto> searchOrderables(@RequestParam UUID programId,
      @RequestParam MultiValueMap<String, Object> queryParams, Pageable pageable) {
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(queryParams);
    return orderableService.additionalToAdd(programId, searchParams, pageable);
  }
}
