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

import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.Set;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.service.SiglusPodService;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftWithLocationRequest;
import org.siglus.siglusapi.web.response.ProofOfDeliveryWithLocationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/proofsOfDeliveryWithLocation")
@RequiredArgsConstructor
public class SiglusPodWithLocationController {
  private final SiglusPodService siglusPodService;

  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/{id}")
  public ProofOfDeliveryWithLocationResponse getProofOfDeliveryWithLocation(@PathVariable("id") UUID podId,
      @RequestParam(required = false) Set<String> expand) {
    return siglusPodService.getPodWithLocation(podId, expand);
  }

  @DeleteMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDraftWithLocation(@PathVariable("id") UUID podId, @PathVariable("subDraftId") UUID subDraftId) {
    siglusPodService.deleteSubDraftWithLocation(podId, subDraftId);
  }

  @PutMapping("/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void updateSubDraftWithLocation(@PathVariable("subDraftId") UUID subDraftId,
      @Valid @RequestBody UpdatePodSubDraftWithLocationRequest request) {
    siglusPodService.updateSubDraftWithLocation(request, subDraftId);
  }
}