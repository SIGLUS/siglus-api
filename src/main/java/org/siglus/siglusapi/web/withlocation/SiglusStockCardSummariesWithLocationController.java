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

import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.UUID;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.dto.LocationMovementDto;
import org.siglus.siglusapi.dto.ProductMovementDto;
import org.siglus.siglusapi.dto.StockCardSummaryWithLocationDto;
import org.siglus.siglusapi.service.SiglusStockCardLocationMovementService;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/stockCardSummariesWithLocation")
public class SiglusStockCardSummariesWithLocationController {

  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;
  @Autowired
  private SiglusStockCardService siglusStockCardService;
  @Autowired
  private SiglusStockCardLocationMovementService siglusStockCardLocationMovementService;

  @GetMapping
  public Page<StockCardSummaryV2Dto> searchStockCardSummaries(
      @RequestParam MultiValueMap<String, String> parameters,
      @RequestParam(required = false) List<UUID> subDraftIds,
      @RequestParam(required = false) UUID draftId,
      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
    return siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(parameters, subDraftIds, draftId, pageable, true);
  }

  @GetMapping("/integration/summary")
  public List<StockCardSummaryWithLocationDto> getStockCardSummaryDtos(
      @RequestParam MultiValueMap<String, String> parameters,
      @RequestParam(required = false) UUID draftId,
      @RequestParam(required = false) UUID orderId,
      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
    return siglusStockCardSummariesService.getStockCardSummaryWithLocationDtos(parameters, draftId, pageable, orderId);
  }

  @GetMapping("/stockCard/{stockCardId}")
  public ResponseEntity<StockCardDto> searchStockCardById(
      @PathVariable("stockCardId") UUID stockCardId) {
    StockCardDto stockCardWithLocationById = siglusStockCardService.findStockCardWithLocationById(
        stockCardId);
    return new ResponseEntity<>(stockCardWithLocationById, OK);
  }

  @GetMapping("/byProduct/{orderableId}")
  public ProductMovementDto getProductMovement(@PathVariable UUID orderableId, @RequestParam UUID facilityId) {
    return siglusStockCardService.getMovementByProduct(facilityId, orderableId);
  }

  @GetMapping("/byLocation")
  public LocationMovementDto getLocationMovement(@RequestParam UUID stockCardId,
      @RequestParam String locationCode) {
    return siglusStockCardLocationMovementService.getLocationMovementDto(stockCardId, locationCode);
  }
}
