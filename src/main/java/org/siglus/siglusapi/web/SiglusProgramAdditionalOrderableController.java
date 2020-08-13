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

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.dto.ProgramAdditionalOrderableDto;
import org.siglus.siglusapi.service.SiglusProgramAdditionalOrderableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/programadditionalorderables")
public class SiglusProgramAdditionalOrderableController {

  @Autowired
  private SiglusProgramAdditionalOrderableService siglusProgramAdditionalOrderableService;

  @GetMapping
  public Page<ProgramAdditionalOrderableDto> searchAdditionalOrderables(
      @RequestParam UUID programId, @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) UUID orderableOriginProgramId, Pageable pageable) {
    return siglusProgramAdditionalOrderableService.searchAdditionalOrderables(programId, code, name,
        orderableOriginProgramId, pageable);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(NO_CONTENT)
  public void deleteAdditionalOrderable(@PathVariable UUID id) {
    siglusProgramAdditionalOrderableService.deleteAdditionalOrderable(id);
  }

  @PostMapping
  @ResponseStatus(CREATED)
  public void createAdditionalOrderables(@RequestBody List<ProgramAdditionalOrderableDto> dtos) {
    siglusProgramAdditionalOrderableService.createAdditionalOrderables(dtos);
  }
}
