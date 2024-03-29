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

import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.Set;
import java.util.UUID;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.siglus.siglusapi.service.SiglusPodService;
import org.siglus.siglusapi.util.MovementDateValidator;
import org.siglus.siglusapi.web.request.PodExtensionRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodExtensionResponse;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
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
@RequestMapping("/api/siglusapi/proofsOfDelivery")
@AllArgsConstructor
public class SiglusPodWithoutLocationController {
  private final SiglusPodService siglusPodService;
  private final MovementDateValidator movementDateValidator;

  @DeleteMapping("/{id}/subDrafts")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDrafts(@PathVariable("id") UUID podId) {
    siglusPodService.deleteSubDrafts(podId);
  }

  @PostMapping("/{id}/subDrafts/merge")
  public PodExtensionResponse mergeSubDrafts(@PathVariable("id") UUID podId,
      @RequestParam Set<String> expand) {
    return siglusPodService.mergeSubDrafts(podId, expand);
  }

  @PostMapping("/{id}/subDrafts/submit")
  public ProofOfDeliveryDto submitSubDrafts(@PathVariable("id") UUID podId,
      @RequestBody PodExtensionRequest request,
      OAuth2Authentication authentication) {
    movementDateValidator.validateMovementDate(request.getPodDto().getReceivedDate(),
        request.getPodDto().getShipment().getOrder().getReceivingFacility().getId());
    return siglusPodService.submitSubDrafts(podId, request, authentication);
  }

  @GetMapping("/{id}/subDrafts/{subDraftId}")
  public ProofOfDeliveryDto getSubDraftDetail(@PathVariable("id") UUID podId,
      @PathVariable("subDraftId") UUID subDraftId,
      @RequestParam Set<String> expand) {
    return siglusPodService.getSubDraftDetail(podId, subDraftId, expand);
  }

  @PutMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void updateSubDraft(@PathVariable("id") UUID podId, @PathVariable("subDraftId") UUID subDraftId,
      @Valid @RequestBody UpdatePodSubDraftRequest request) {
    siglusPodService.updateSubDraft(request, subDraftId);
  }

  @DeleteMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDraft(@PathVariable("id") UUID podId, @PathVariable("subDraftId") UUID subDraftId) {
    siglusPodService.deleteSubDraft(podId, subDraftId);
  }
}
