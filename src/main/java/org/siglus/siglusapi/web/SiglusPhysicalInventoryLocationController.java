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

import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryDto;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/siglusapi/location/physicalInventories")
public class SiglusPhysicalInventoryLocationController {

  @Autowired
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @PostMapping()
  @ResponseStatus(CREATED)
  public PhysicalInventoryDto createEmptyPhysicalInventoryWithLocationOption(
      @RequestBody PhysicalInventoryDto dto,
      @RequestParam Integer splitNum,
      @RequestParam(required = false) boolean initialPhysicalInventory,
      @RequestParam(name = "locationManagementOption") String optionString) {
    if (ALL_PRODUCTS_PROGRAM_ID.equals(dto.getProgramId())) {
      return siglusPhysicalInventoryService.createAndSplitNewDraftForAllProduct(dto, splitNum,
          initialPhysicalInventory, optionString);
    }
    return siglusPhysicalInventoryService.createAndSpiltNewDraftForOneProgram(dto, splitNum,
        optionString);
  }

  @GetMapping("/subDraft")
  public SiglusPhysicalInventoryDto searchSubDraftPhysicalInventory(@RequestParam List<UUID> subDraftIds) {
    return siglusPhysicalInventoryService.getSubLocationPhysicalInventoryDtoBySubDraftId(subDraftIds);
  }
}

