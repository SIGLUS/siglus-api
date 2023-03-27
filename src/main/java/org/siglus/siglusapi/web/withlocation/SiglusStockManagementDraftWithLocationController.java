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

package org.siglus.siglusapi.web.withlocation;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.MergedLineItemWithLocationDto;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftWithLocationDto;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.siglus.siglusapi.service.SiglusStockManagementDraftService;
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
@RequestMapping("/api/siglusapi/draftsWithLocation")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@RequiredArgsConstructor
public class SiglusStockManagementDraftWithLocationController {

  private final SiglusStockManagementDraftService stockManagementDraftService;

  @GetMapping
  public List<StockManagementDraftWithLocationDto> searchDrafts(@RequestParam UUID program,
      @RequestParam UUID userId, @RequestParam String draftType,
      @RequestParam(required = false) Boolean isDraft) {
    return stockManagementDraftService.findStockManagementDraftWithLocation(program, draftType, isDraft);
  }

  @GetMapping("/{id}/subDraft/{subDraftId}")
  public StockManagementDraftWithLocationDto searchDraftWithMutilUser(@PathVariable UUID subDraftId) {
    return stockManagementDraftService.searchDraftWithLocation(subDraftId);
  }

  @DeleteMapping("/{id}/subDraft/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void deleteDraftWithMutilUser(@PathVariable UUID subDraftId) {
    stockManagementDraftService.deleteStockManagementDraft(subDraftId);
  }

  @PutMapping("/{id}/subDraft/{subDraftId}")
  @ResponseStatus(OK)
  public StockManagementDraftWithLocationDto updateDraftWithMutilUser(@PathVariable UUID subDraftId,
      @RequestBody StockManagementDraftWithLocationDto dto) {
    return stockManagementDraftService.updateDraftWithLocation(dto, subDraftId);
  }

  @GetMapping("/{id}")
  public StockManagementDraftWithLocationDto searchDraft(@PathVariable UUID id) {
    return stockManagementDraftService.searchDraftWithLocation(id);
  }

  @PutMapping("/{id}")
  @ResponseStatus(OK)
  public StockManagementDraftWithLocationDto updateDraft(@PathVariable UUID id,
      @RequestBody StockManagementDraftWithLocationDto dto) {
    return stockManagementDraftService.updateDraftWithLocation(dto, id);
  }

  @PutMapping("/{id}/subDraft/{subDraftId}/info")
  @ResponseStatus(OK)
  public StockManagementDraftDto restoreSubDraftWhenDoDelete(
      @RequestBody StockManagementDraftDto dto) {
    return stockManagementDraftService.restoreSubDraftWhenDoDelete(dto);
  }

  @PutMapping("/{id}/subDraft/{subDraftId}/submit")
  @ResponseStatus(OK)
  public StockManagementDraftWithLocationDto updateStatusAfterSubmitWithLocation(
      @RequestBody StockManagementDraftWithLocationDto draftDto) {
    return stockManagementDraftService.updateStatusAfterSubmitWithLocation(draftDto);
  }

  @GetMapping("/{id}/subDraft/merge")
  public List<MergedLineItemWithLocationDto> mergeSubDraftsWithLocation(@PathVariable UUID id) {
    return stockManagementDraftService.mergeSubDraftsWithLocation(id);
  }


  @DeleteMapping("/{id}")
  @ResponseStatus(NO_CONTENT)
  public void deleteInitialDraft(@PathVariable UUID id) {
    stockManagementDraftService.deleteInitialDraft(id);
  }

  @GetMapping("/initialDraft")
  public List<StockManagementInitialDraftDto> searchInitialDrafts(
      @RequestParam UUID programId,
      @RequestParam String draftType) {
    return stockManagementDraftService.findStockManagementInitialDraft(programId, draftType);
  }

  @PostMapping("/initialDraft")
  @ResponseStatus(CREATED)
  public StockManagementInitialDraftDto initialDraft(
      @RequestBody StockManagementInitialDraftDto dto) {
    return stockManagementDraftService.createInitialDraft(dto);
  }

  @GetMapping("/subDrafts")
  public List<StockManagementDraftWithLocationDto> searchSubDrafts(@RequestParam UUID initialDraftId) {
    return stockManagementDraftService.findSubDraftsWithLocation(initialDraftId);
  }

  @PostMapping
  @ResponseStatus(CREATED)
  public StockManagementDraftWithLocationDto createEmptyStockManagementDraftWithLocation(
      @RequestBody StockManagementDraftWithLocationDto dto) {
    return stockManagementDraftService.createEmptyStockManagementDraftWithLocation(dto);
  }

  @PutMapping("/update")
  @ResponseStatus(OK)
  public StockManagementDraftDto updateOperatorAndStatus(
      @RequestBody StockManagementDraftDto dto) {
    return stockManagementDraftService.updateOperatorAndStatus(dto);
  }
}

