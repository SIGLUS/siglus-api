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
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftDto;
import org.siglus.siglusapi.service.SiglusStockCardLocationMovementDraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/locationMovementDrafts")
public class SiglusProductLocationMovementDraftController {

  @Autowired
 private SiglusStockCardLocationMovementDraftService productLocationMovementDraftService;

  @PostMapping
  @ResponseStatus(CREATED)
  public StockCardLocationMovementDraftDto createEmptyProductLocationMovementDraft(
      @RequestBody StockCardLocationMovementDraftDto dto) {
    return productLocationMovementDraftService.createEmptyMovementDraft(dto);
  }

  @GetMapping
  public List<StockCardLocationMovementDraftDto> searchMovementDrafts(@RequestParam UUID programId) {
    return productLocationMovementDraftService.searchMovementDrafts(programId);
  }

  @GetMapping("/{id}")
  public StockCardLocationMovementDraftDto searchMovementDraft(@PathVariable UUID id) {
    return productLocationMovementDraftService.searchMovementDraft(id);
  }

  @PutMapping("/{id}")
  @ResponseStatus(OK)
  public StockCardLocationMovementDraftDto updateDraft(@PathVariable UUID id,
      @RequestBody StockCardLocationMovementDraftDto dto) {
    return productLocationMovementDraftService.updateMovementDraft(dto, id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(NO_CONTENT)
  public void deleteDraft(@PathVariable UUID id) {
    productLocationMovementDraftService.deleteMovementDraft(id);
  }

  @GetMapping("/virtualLocationDrafts")
  public StockCardLocationMovementDraftDto searchVirtualLocationMovementDraft() {
    return productLocationMovementDraftService.searchVirtualLocationMovementDraft();
  }

}
