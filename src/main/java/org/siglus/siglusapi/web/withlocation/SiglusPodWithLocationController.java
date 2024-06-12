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

import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.domain.PodSubDraftLineItem;
import org.siglus.siglusapi.dto.PodWithLocationDto;
import org.siglus.siglusapi.service.SiglusPodService;
import org.siglus.siglusapi.util.MovementDateValidator;
import org.siglus.siglusapi.web.request.CreatePodSubDraftLineItemRequest;
import org.siglus.siglusapi.web.request.PodWithLocationRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.ProofOfDeliveryWithLocationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/proofsOfDeliveryWithLocation")
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SiglusPodWithLocationController {
  private final SiglusPodService siglusPodService;
  private final MovementDateValidator movementDateValidator;

  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/{id}/subDrafts/{subDraftId}")
  public PodWithLocationDto getPodSubDraftWithLocation(@PathVariable("id") UUID podId,
      @PathVariable("subDraftId") UUID subDraftId) {
    return siglusPodService.getPodSubDraftWithLocation(podId, subDraftId);
  }

  @DeleteMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDraftWithLocation(@PathVariable("id") UUID podId, @PathVariable("subDraftId") UUID subDraftId) {
    siglusPodService.deleteSubDraftWithLocation(podId, subDraftId);
  }

  @PutMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void updateSubDraftWithLocation(@PathVariable("id") UUID podId, @PathVariable("subDraftId") UUID subDraftId,
      @Valid @RequestBody UpdatePodSubDraftRequest request) {
    siglusPodService.updateSubDraftWithLocation(request, subDraftId);
  }

  @DeleteMapping("/{id}/subDrafts")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDraftsWithLocation(@PathVariable("id") UUID podId) {
    siglusPodService.deleteSubDraftsWithLocation(podId);
  }

  @GetMapping("/{id}/subDrafts/merge")
  public ProofOfDeliveryWithLocationResponse getMergedSubDraftWithLocation(@PathVariable("id") UUID podId) {
    return siglusPodService.getMergedSubDraftWithLocation(podId);
  }

  @PutMapping("/{id}")
  public void submitSubDraftsWithLocation(@PathVariable("id") UUID podId, @RequestBody PodWithLocationRequest request,
      OAuth2Authentication authentication) {
    movementDateValidator.validateMovementDate(request.getPodDto().getReceivedDate(),
        request.getPodDto().getShipment().getOrder().getReceivingFacility().getId());
    siglusPodService.submitSubDraftsWithLocation(podId, request, authentication);
  }

  @GetMapping("/{id}")
  public ProofOfDeliveryWithLocationResponse viewPodWithLocation(@PathVariable("id") UUID podId) {
    return siglusPodService.getPodExtensionResponseWithLocation(podId);
  }

  @PostMapping("/{id}/subDrafts/{subDraftId}/lineItems")
  public PodSubDraftLineItem createPodSubDraftLineItem(@PathVariable("id") UUID podId,
      @PathVariable("subDraftId") UUID subDraftId,
      @Validated @RequestBody CreatePodSubDraftLineItemRequest request) {
    return siglusPodService.createPodSubDraftLineItem(podId, subDraftId, request.getPodLineItemId());
  }

  @DeleteMapping("/{id}/subDrafts/{subDraftId}/lineItems/{lineItemId}")
  @ResponseStatus(NO_CONTENT)
  public void deletePodSubDraftLineItem(@PathVariable("id") UUID podId,
                                        @PathVariable("subDraftId") UUID subDraftId,
                                        @PathVariable("lineItemId") UUID lineItemId) {
    siglusPodService.deletePodSubDraftLineItem(podId, subDraftId, lineItemId);
  }
}
