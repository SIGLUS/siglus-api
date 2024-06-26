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

import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_UUID;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.siglus.siglusapi.dto.DraftListDto;
import org.siglus.siglusapi.dto.PhysicalInventoryValidationDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryDto;
import org.siglus.siglusapi.repository.dto.SiglusPhysicalInventoryBriefDto;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryHistoryService;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/siglusapi/physicalInventories")
@RequiredArgsConstructor
public class SiglusPhysicalInventoryController {

  private final SiglusPhysicalInventoryService siglusPhysicalInventoryService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final SiglusPhysicalInventoryHistoryService siglusPhysicalInventoryHistoryService;

  @GetMapping
  public List<PhysicalInventoryDto> searchPhysicalInventories(
      @RequestParam UUID program, @RequestParam UUID facility,
      @RequestParam(required = false) Boolean isDraft) {
    return siglusPhysicalInventoryService.getPhysicalInventoryBriefDtos(facility, program, isDraft)
        .stream()
        .map(SiglusPhysicalInventoryBriefDto::toSiglusPhysicalInventoryDto)
        .collect(Collectors.toList());
  }

  @GetMapping("/{id}")
  public PhysicalInventoryDto searchPhysicalInventory(@PathVariable UUID id) {
    if (ALL_PRODUCTS_UUID.equals(id)) {
      UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
      return siglusPhysicalInventoryService.getPhysicalInventoryForAllPrograms(facilityId);
    }
    return siglusPhysicalInventoryService.getPhysicalInventory(id);
  }

  @PutMapping("/{id}")
  public PhysicalInventoryDto updatePhysicalInventory(@PathVariable UUID id, @RequestBody PhysicalInventoryDto dto) {
    if (ALL_PRODUCTS_UUID.equals(id)) {
      siglusPhysicalInventoryService.checkDraftIsExist(dto.getFacilityId());
      return siglusPhysicalInventoryService.saveDraftForAllPrograms(dto);
    }
    return siglusPhysicalInventoryService.saveDraftForProductsForOneProgram(dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(NO_CONTENT)
  public void deletePhysicalInventory(@PathVariable UUID id) {
    if (ALL_PRODUCTS_UUID.equals(id)) {
      UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
      siglusPhysicalInventoryService.deletePhysicalInventoryDraftForAllPrograms(facilityId);
    } else {
      siglusPhysicalInventoryService.deletePhysicalInventoryDraft(id);
    }
  }

  @GetMapping("/dates")
  public Set<String> searchPhysicalInventoryDates(
      @RequestParam UUID programId, @RequestParam UUID facilityId,
      @RequestParam String startDate, @RequestParam String endDate) {
    return siglusPhysicalInventoryService.findPhysicalInventoryDates(programId, facilityId, startDate, endDate);
  }

  @GetMapping("/latest")
  public PhysicalInventoryDto searchLatestPhysicalInventoryOccurDate(
      @RequestParam UUID facilityId, @RequestParam UUID programId) {
    return siglusPhysicalInventoryService.findLatestPhysicalInventory(facilityId, programId);
  }

  @GetMapping("/conflict")
  public PhysicalInventoryValidationDto checkPhysicalInventoryConflict(@RequestParam UUID program,
      @RequestParam UUID facility, @RequestParam(required = false) UUID draft) {
    if (ALL_PRODUCTS_UUID.equals(program)) {
      return siglusPhysicalInventoryService.checkConflictForAllPrograms(facility, draft);
    }
    return siglusPhysicalInventoryService.checkConflictForOneProgram(facility, program, draft);
  }

  @GetMapping("/draftList")
  public DraftListDto searchSubDraftList(@RequestParam UUID program,
      @RequestParam UUID facility,
      @RequestParam(required = false) Boolean isDraft) {
    if (ALL_PRODUCTS_UUID.equals(program)) {
      return siglusPhysicalInventoryService.getSubDraftListForAllPrograms(facility, isDraft);
    }
    return siglusPhysicalInventoryService.getSubDraftListForOneProgram(program, facility, isDraft);
  }

  @GetMapping("/histories")
  public List<SiglusPhysicalInventoryHistoryDto> searchPhysicalInventoryHistories() {
    return siglusPhysicalInventoryHistoryService.searchPhysicalInventoryHistories();
  }

  @GetMapping("/histories/{id}")
  public String searchPhysicalInventoryHistoryData(@PathVariable("id") UUID id) {
    return siglusPhysicalInventoryHistoryService.searchPhysicalInventoryHistoryData(id);
  }
}
