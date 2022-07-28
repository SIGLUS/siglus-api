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

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.web.ProofOfDeliveryController;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusProofOfDeliveryService;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodSubDraftListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/api/siglusapi/proofsOfDelivery")
public class SiglusProofOfDeliveryController {

  @Autowired
  private ProofOfDeliveryController actualController;

  @Autowired
  private SiglusNotificationService notificationService;

  @Autowired
  private SiglusProofOfDeliveryService proofOfDeliveryService;

  @Autowired
  private ProofOfDeliveryController proofOfDeliveryController;

  /**
   * why we redo this api? to support #330?<br> update status of notification of pod after confirm pod
   */
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  @PutMapping("/{id}")
  @Transactional
  public ProofOfDeliveryDto updateProofOfDelivery(@PathVariable("id") UUID proofOfDeliveryId,
      @RequestBody ProofOfDeliveryDto dto,
      OAuth2Authentication authentication) {
    ProofOfDeliveryDto proofOfDeliveryDto = actualController
        .updateProofOfDelivery(proofOfDeliveryId, dto, authentication);
    if (proofOfDeliveryDto.getStatus() == ProofOfDeliveryStatus.CONFIRMED) {
      notificationService.postConfirmPod(dto);
    }
    return proofOfDeliveryDto;
  }

  @GetMapping("/{id}/print")
  public ModelAndView printProofOfDelivery(HttpServletRequest request,
      @PathVariable("id") UUID id, OAuth2Authentication authentication) throws IOException {
    return actualController.printProofOfDelivery(request, id, authentication);
  }

  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @GetMapping("/{id}")
  public ProofOfDeliveryDto getProofOfDelivery(@PathVariable("id") UUID id,
      @RequestParam(required = false) Set<String> expand) {
    return proofOfDeliveryService.getProofOfDelivery(id, expand);
  }

  @GetMapping
  public Page<ProofOfDeliveryDto> getAllProofsOfDelivery(
      @RequestParam(required = false) UUID orderId,
      @RequestParam(required = false) UUID shipmentId,
      Pageable pageable) {
    return proofOfDeliveryController.getAllProofsOfDelivery(orderId, shipmentId, pageable);
  }

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{id}/subDrafts")
  public void createSubDrafts(@PathVariable("id") UUID proofOfDeliveryId,
      @Valid @RequestBody CreatePodSubDraftRequest request) {
    proofOfDeliveryService.createSubDrafts(request);
  }

  @DeleteMapping("/{id}/subDrafts")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDrafts(@PathVariable("id") UUID proofOfDeliveryId) {
    proofOfDeliveryService.deleteSubDrafts(proofOfDeliveryId);
  }

  @GetMapping("/{id}/subDrafts/summary")
  public PodSubDraftListResponse getSubDraftSummary(@PathVariable("id") UUID proofOfDeliveryId) {
    return proofOfDeliveryService.getSubDraftSummary(proofOfDeliveryId);
  }

  @GetMapping("/{id}/subDrafts/{subDraftId}")
  public ProofOfDeliveryDto getSubDraftDetail(@PathVariable("id") UUID proofOfDeliveryId,
      @PathVariable("subDraftId") UUID subDraftId) {
    return proofOfDeliveryService.getSubDraftDetail(proofOfDeliveryId, subDraftId);
  }

  @PutMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void updateSubDraft(@PathVariable("id") UUID proofOfDeliveryId, @PathVariable("subDraftId") UUID subDraftId,
      @RequestBody UpdatePodSubDraftRequest request) {
    proofOfDeliveryService.updateSubDraft(request, subDraftId);
  }

  @DeleteMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDraft(@PathVariable("id") UUID proofOfDeliveryId, @PathVariable("subDraftId") UUID subDraftId) {
    proofOfDeliveryService.deleteSubDraft(proofOfDeliveryId, subDraftId);
  }
}
