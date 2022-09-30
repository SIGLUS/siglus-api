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

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.referencedata.dto.LotDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.dto.StockCardDetailsDto;
import org.siglus.siglusapi.dto.StockCardSummaryDto;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/stockCardSummaries")
@RequiredArgsConstructor
public class SiglusStockCardSummariesController {

  private final SiglusStockCardSummariesService stockCardSummariesSiglusService;

  @GetMapping
  public Page<StockCardSummaryV2Dto> searchStockCardSummaries(
      @RequestParam MultiValueMap<String, String> parameters,
      @RequestParam(required = false) List<UUID> subDraftIds,
      @RequestParam(required = false) UUID draftId,
      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {

    return stockCardSummariesSiglusService
        .searchStockCardSummaryV2Dtos(parameters, subDraftIds, draftId, pageable);
  }

  @GetMapping("/integration/detail")
  public List<List<StockCardDetailsDto>> getStockCardDetailsDtoByGroup(
      @RequestParam MultiValueMap<String, String> parameters,
      @RequestParam(required = false) List<UUID> subDraftIds,
      @RequestParam(required = false) UUID draftId,
      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
    return stockCardSummariesSiglusService.getStockCardDetailsDtoByGroup(parameters, subDraftIds, draftId, pageable);
  }

  @GetMapping("/integration/summary")
  public List<StockCardSummaryDto> getStockCardSummaryDtos(
      @RequestParam MultiValueMap<String, String> parameters,
      @RequestParam(required = false) List<UUID> subDraftIds,
      @RequestParam(required = false) UUID draftId,
      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
    return stockCardSummariesSiglusService.getStockCardSummaryDtos(parameters, subDraftIds, draftId, pageable);
  }

  @PostMapping("/lots")
  public List<LotDto> getLosts(@RequestBody List<UUID> orderableIds) {
    return stockCardSummariesSiglusService.getLotsDataByOrderableIds(orderableIds);
  }

}
