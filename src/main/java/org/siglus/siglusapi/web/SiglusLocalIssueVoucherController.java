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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.siglus.siglusapi.dto.LocalIssueVoucherDto;
import org.siglus.siglusapi.service.SiglusLocalIssueVoucherService;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse.SubDraftInfo;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
@AllArgsConstructor
@RequestMapping("/api/siglusapi/localIssueVouchers")
public class SiglusLocalIssueVoucherController {

  private final SiglusLocalIssueVoucherService localIssueVoucherService;

  private static final String SUB_DRAFT_ID = "subDraftId";

  @PostMapping
  public LocalIssueVoucherDto createLocalIssueVoucher(@Validated @RequestBody LocalIssueVoucherDto dto) {
    return localIssueVoucherService.createLocalIssueVoucher(dto);
  }

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{id}/subDrafts")
  public SubDraftInfo createLocalIssueVoucherSubDraft(@PathVariable("id") UUID localIssueVoucherId) {
    return localIssueVoucherService.createLocalIssueVoucherSubDraft(localIssueVoucherId);
  }

  @GetMapping("/{id}/subDrafts")
  public PodSubDraftsSummaryResponse searchLocalIssueVoucherSubDrafts(@PathVariable("id") UUID localIssueVoucherId) {
    return localIssueVoucherService.searchLocalIssueVoucherSubDrafts(localIssueVoucherId);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(NO_CONTENT)
  public void deleteLocalIssueVoucher(@PathVariable("id") UUID localIssueVoucherId) {
    localIssueVoucherService.deleteLocalIssueVoucher(localIssueVoucherId);
  }

  @GetMapping("/{id}/subDraft/{subDraftId}")
  public ProofOfDeliveryDto getSubDraftDetail(@PathVariable("id") UUID podId,
      @PathVariable(SUB_DRAFT_ID) UUID subDraftId,
      @RequestParam Set<String> expand) {
    return localIssueVoucherService.getSubDraftDetail(podId, subDraftId, expand);
  }

  @DeleteMapping("/{id}/subDrafts/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void deleteSubDraft(@PathVariable("id") UUID podId, @PathVariable(SUB_DRAFT_ID) UUID subDraftId) {
    localIssueVoucherService.deleteSubDraft(podId, subDraftId);
  }

  @PutMapping("/{id}/subDraft/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void updateSubDraft(@PathVariable(SUB_DRAFT_ID) UUID subDraftId,
      @Valid @RequestBody UpdatePodSubDraftRequest request) {
    localIssueVoucherService.updateSubDraft(request, subDraftId);
  }

  @PostMapping("/{id}/clearSubDraftFillingPage/{subDraftId}")
  @ResponseStatus(NO_CONTENT)
  public void clearFillingPage(@PathVariable("id") UUID podId, @PathVariable(SUB_DRAFT_ID) UUID subDraftId) {
    localIssueVoucherService.clearFillingPage(podId, subDraftId);
  }

  @GetMapping("/availableProduct")
  public List<OrderableDto> availableProduct(@RequestParam("podId") UUID podId) {
    return localIssueVoucherService.getAvailableOrderables(podId);
  }
}
