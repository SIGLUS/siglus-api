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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.siglus.siglusapi.dto.DraftListDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryDto;
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
@RequiredArgsConstructor
@RequestMapping("/api/siglusapi/location/physicalInventories")
public class SiglusPhysicalInventoryLocationController {

  private final SiglusPhysicalInventoryService siglusPhysicalInventoryService;
  private final SiglusPhysicalInventorySubDraftService siglusPhysicalInventorySubDraftService;


  @PostMapping()
  @ResponseStatus(CREATED)
  public PhysicalInventoryDto createEmptyPhysicalInventoryWithLocationOption(
      @RequestBody PhysicalInventoryDto dto,
      @RequestParam Integer splitNum,
      @RequestParam(required = false) boolean initialPhysicalInventory,
      @RequestParam(name = "locationManagementOption") String optionString,
      @RequestParam(required = false) boolean isByLocation) {
    return siglusPhysicalInventoryService.createAndSplitNewDraftForAllProduct(dto, splitNum,
        initialPhysicalInventory, optionString, isByLocation);
  }

  @GetMapping
  public List<SiglusPhysicalInventoryDto> searchPhysicalInventories(
      @RequestParam UUID program,
      @RequestParam UUID facility,
      @RequestParam(required = false) Boolean isDraft,
      @RequestParam(required = false) boolean isByLocation) {
    return siglusPhysicalInventoryService
        .getLocationPhysicalInventoryDtosForAllProducts(facility, isDraft, isByLocation);
  }

  @GetMapping("/subDraft")
  public SiglusPhysicalInventoryDto searchSubDraftPhysicalInventory(@RequestParam List<UUID> subDraftIds,
      @RequestParam(required = false) boolean isByLocation) {
    return siglusPhysicalInventoryService.getSubLocationPhysicalInventoryDtoBySubDraftId(subDraftIds, isByLocation);
  }


  @PutMapping("/subDraft")
  public void updateSubDrafts(@RequestBody PhysicalInventorySubDraftDto dto,
      @RequestParam(required = false) boolean isByLocation) {
    siglusPhysicalInventorySubDraftService.updateSubDrafts(dto.getSubDraftIds(), dto,
        PhysicalInventorySubDraftEnum.DRAFT, isByLocation);
  }

  @DeleteMapping("/subDraft")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDrafts(
      @RequestParam(required = false) boolean initialPhysicalInventory,
      @RequestBody List<UUID> subDraftIds,
      @RequestParam(required = false) boolean isByLocation) {
    siglusPhysicalInventorySubDraftService.deleteSubDrafts(subDraftIds, initialPhysicalInventory, isByLocation);
  }

  @PostMapping("/subDraftSubmit")
  @ResponseStatus(NO_CONTENT)
  public void submitSubDrafts(@RequestBody PhysicalInventorySubDraftDto dto,
      @RequestParam(required = false) boolean isByLocation) {
    siglusPhysicalInventorySubDraftService.updateSubDrafts(dto.getSubDraftIds(), dto,
        PhysicalInventorySubDraftEnum.SUBMITTED, isByLocation);

  }

  @GetMapping("/draftList")
  public DraftListDto searchSubDraftList(@RequestParam UUID program,
      @RequestParam UUID facility,
      @RequestParam(required = false) Boolean isDraft) {
    return siglusPhysicalInventoryService.getSubDraftListForAllProduct(facility, isDraft);
  }

}

