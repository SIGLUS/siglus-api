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
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.siglus.siglusapi.service.SiglusPodService;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/proofsOfDelivery")
@AllArgsConstructor
public class SiglusPodController {
  private final SiglusPodService siglusPodService;

  @GetMapping
  public Page<ProofOfDeliveryDto> getAllProofsOfDelivery(
      @RequestParam(required = false) UUID orderId,
      @RequestParam(required = false) UUID shipmentId,
      Pageable pageable) {
    return siglusPodService.getAllProofsOfDelivery(orderId, shipmentId, pageable);
  }

  @GetMapping("/{id}/printInfo")
  public PodPrintInfoResponse getPrintInfo(String orderId, @PathVariable("id") UUID podId) {
    return siglusPodService.getPrintInfo(UUID.fromString(orderId), podId);
  }

  @PostMapping("/{id}/subDrafts")
  @ResponseStatus(HttpStatus.CREATED)
  public void createSubDrafts(@PathVariable("id") UUID podId,
      @Valid @RequestBody CreatePodSubDraftRequest request) {
    siglusPodService.createSubDrafts(podId, request);
  }

  @GetMapping("/{id}/subDrafts/summary")
  public PodSubDraftsSummaryResponse getSubDraftSummary(@PathVariable("id") UUID podId) {
    return siglusPodService.getSubDraftSummary(podId);
  }


}
