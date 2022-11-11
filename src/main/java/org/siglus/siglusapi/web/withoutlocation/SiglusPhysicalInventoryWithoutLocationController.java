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

package org.siglus.siglusapi.web.withoutlocation;

import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.siglus.siglusapi.service.SiglusPhysicalInventorySubDraftService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
public class SiglusPhysicalInventoryWithoutLocationController {

  private final SiglusPhysicalInventoryService siglusPhysicalInventoryService;
  private final SiglusPhysicalInventorySubDraftService siglusPhysicalInventorySubDraftService;

  @PostMapping
  @ResponseStatus(CREATED)
  public PhysicalInventoryDto createEmptyPhysicalInventory(
      @RequestBody PhysicalInventoryDto dto,
      @RequestParam Integer splitNum,
      @RequestParam(required = false) boolean initialPhysicalInventory) {
    if (ALL_PRODUCTS_PROGRAM_ID.equals(dto.getProgramId())) {
      // TODO refactor the last "null" param by override method
      return siglusPhysicalInventoryService.createAndSplitNewDraftForAllPrograms(dto, splitNum,
          initialPhysicalInventory, null, false);
    }
    return siglusPhysicalInventoryService.createAndSpiltNewDraftForOneProgram(dto, splitNum, null, false);
  }

  @GetMapping("/subDraft")
  public PhysicalInventoryDto searchSubDraftPhysicalInventory(@RequestParam List<UUID> subDraftIds) {
    return siglusPhysicalInventoryService.getSubPhysicalInventoryDtoBySubDraftId(subDraftIds);
  }

  @PutMapping("/subDraft")
  public void updateSubDrafts(@RequestBody PhysicalInventorySubDraftDto dto) {
    siglusPhysicalInventorySubDraftService.updateSubDrafts(dto.getSubDraftIds(), dto,
        PhysicalInventorySubDraftEnum.DRAFT, false);
  }

  @DeleteMapping("/subDraft")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDrafts(
      @RequestParam(required = false) boolean initialPhysicalInventory,
      @RequestBody List<UUID> subDraftIds) {
    siglusPhysicalInventorySubDraftService.deleteSubDrafts(subDraftIds, initialPhysicalInventory, false);
  }

  @PostMapping("/subDraftSubmit")
  @ResponseStatus(NO_CONTENT)
  public void submitSubDrafts(@RequestBody PhysicalInventorySubDraftDto dto) {
    siglusPhysicalInventorySubDraftService.updateSubDrafts(dto.getSubDraftIds(), dto,
        PhysicalInventorySubDraftEnum.SUBMITTED, false);
  }

}
