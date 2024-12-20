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

import static org.siglus.siglusapi.constant.PaginationConstants.DEFAULT_PAGE_NUMBER;
import static org.siglus.siglusapi.constant.PaginationConstants.NO_PAGINATION;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.openlmis.requisition.web.RequisitionController;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.SiglusRequisitionDateDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.localmachine.event.requisition.web.approve.RequisitionInternalApproveEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.createforclient.RequisitionCreateForClientEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.finalapprove.RequisitionFinalApproveEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.reject.RequisitionRejectEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.release.RequisitionReleaseEmitter;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.siglus.siglusapi.service.SiglusRequisitionExportService;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.siglus.siglusapi.service.scheduledtask.SiglusRequisitionAutoCloseService;
import org.siglus.siglusapi.web.request.BuildRequisitionDraftRequest;
import org.siglus.siglusapi.web.response.RequisitionPeriodExtensionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
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
@Slf4j
@RequestMapping("/api/siglusapi/requisitions")
@RequiredArgsConstructor
@Validated
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class SiglusRequisitionController {

  @Autowired
  private RequisitionController requisitionController;
  @Autowired
  private SiglusRequisitionService siglusRequisitionService;
  @Autowired
  private RequisitionService requisitionService;
  @Autowired
  private SiglusRequisitionAutoCloseService siglusRequisitionAutoCloseService;
  @Autowired
  private SiglusProcessingPeriodService siglusProcessingPeriodService;
  @Autowired
  private SiglusNotificationService notificationService;
  @Autowired
  private AuthenticationHelper authenticationHelper;
  @Autowired
  private RequisitionInternalApproveEmitter requisitionInternalApproveEmitter;
  @Autowired
  private RequisitionFinalApproveEmitter requisitionFinalApproveEmitter;
  @Autowired
  private RequisitionReleaseEmitter requisitionReleaseEmitter;
  @Autowired
  private RequisitionRejectEmitter requisitionRejectEmitter;
  @Autowired
  private RequisitionExtensionRepository requisitionExtensionRepository;
  @Autowired
  private RequisitionCreateForClientEmitter requisitionCreateForClientEmitter;
  @Autowired
  private SiglusRequisitionExportService siglusRequisitionExportService;

  @PostMapping("/initiate")
  @ResponseStatus(HttpStatus.CREATED)
  public SiglusRequisitionDto initiate(@RequestParam(value = "program") UUID programId,
      @RequestParam(value = "facility") UUID facilityId,
      @RequestParam(value = "suggestedPeriod") UUID suggestedPeriod,
      @RequestParam(value = "emergency") boolean emergency,
      @RequestParam(value = "physicalInventoryDate", required = false)
      String physicalInventoryDateStr,
      HttpServletRequest request, HttpServletResponse response) {
    return siglusRequisitionService.initiate(programId, facilityId, suggestedPeriod, emergency,
        physicalInventoryDateStr, request, response);
  }

  @PostMapping("/draft")
  public SiglusRequisitionDto buildDraftForRegular(
      @RequestBody BuildRequisitionDraftRequest buildRequisitionDraftRequest,
      HttpServletRequest request, HttpServletResponse response) {
    SiglusRequisitionDto dto = siglusRequisitionService.buildDraftForRegular(
        buildRequisitionDraftRequest.getFacilityId(),
        buildRequisitionDraftRequest.getPeriodId(),
        buildRequisitionDraftRequest.getProgramId(), request, response);
    siglusRequisitionService.updateRequisition(dto.getId(), dto, request, response);
    return dto;
  }

  @GetMapping("/{id}")
  public SiglusRequisitionDto searchRequisition(@PathVariable("id") UUID requisitionId) {
    return siglusRequisitionService.searchRequisition(requisitionId);
  }

  @PutMapping("/{id}")
  public SiglusRequisitionDto updateRequisition(@PathVariable("id") UUID requisitionId,
      @RequestBody SiglusRequisitionDto requisitionDto,
      HttpServletRequest request, HttpServletResponse response) {
    return siglusRequisitionService.updateRequisition(requisitionId, requisitionDto, request, response);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRequisition(@PathVariable("id") UUID requisitionId) {
    siglusRequisitionService.deleteRequisition(requisitionId);
  }

  @GetMapping("/search")
  public Page<BasicRequisitionDto> searchRequisitions(
      @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) {
    return siglusRequisitionService.searchRequisitions(queryParams, pageable);
  }

  @PostMapping("/{id}/submit")
  public BasicRequisitionDto submitRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    return siglusRequisitionService.submitRequisition(requisitionId, request, response);
  }

  @PostMapping("/{id}/authorize")
  public BasicRequisitionDto authorizeRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    return siglusRequisitionService.authorizeRequisition(requisitionId, request, response);
  }

  @PostMapping("/{id}/approve")
  @Transactional
  public BasicRequisitionDto approveRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    BasicRequisitionDto basicRequisitionDto =
        siglusRequisitionService.approveRequisition(requisitionId, request, response);
    notificationService.postApprove(basicRequisitionDto);
    if (basicRequisitionDto.getStatus() == RequisitionStatus.IN_APPROVAL) {
      requisitionInternalApproveEmitter.emit(requisitionId);
    }
    if (basicRequisitionDto.getStatus() == RequisitionStatus.APPROVED) {
      RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
      if (requisitionExtension != null && requisitionExtension.createdBySupplier()) {
        requisitionCreateForClientEmitter.emit(requisitionId);
      } else {
        requisitionFinalApproveEmitter.emit(requisitionId);
      }
    }
    if (basicRequisitionDto.getStatus() == RequisitionStatus.RELEASED_WITHOUT_ORDER) {
      UUID userId = authenticationHelper.getCurrentUser().getId();
      ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
      releasableRequisitionDto.setRequisitionId(requisitionId);
      releasableRequisitionDto.setSupplyingDepotId(basicRequisitionDto.getFacility().getId());
      requisitionReleaseEmitter.emit(releasableRequisitionDto, userId);
    }
    return basicRequisitionDto;
  }

  @PostMapping("/clients/{id}")
  public BasicRequisitionDto createClientRequisition(
      @PathVariable(value = "id") UUID facilityId,
      @RequestBody SiglusRequisitionDto requisitionDto,
      HttpServletRequest request,
      HttpServletResponse response) {
    return siglusRequisitionService.createClientRequisition(facilityId,
        requisitionDto, request, response);
  }


  /**
   * why we redo this api? for bug card #228, change dependency:
   * {@linkplain org.openlmis.requisition.domain.requisition.Requisition#reject(Map, UUID)}  method}
   * requisition.reject->updateConsumptions(products)->
   * {@linkplain org.openlmis.requisition.domain.requisition.Requisition#filterLineItems(Boolean, Boolean, Map)} method}
   * getNonSkippedFullSupplyRequisitionLineItems->filterLineItems
   */
  @PutMapping("/{id}/reject")
  @Transactional
  public BasicRequisitionDto rejectRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    BasicRequisitionDto basicRequisitionDto = siglusRequisitionService.rejectRequisition(requisitionId, request,
        response);
    if (!internalFacilityUser(basicRequisitionDto)) {
      requisitionRejectEmitter.emit(requisitionId, basicRequisitionDto.getFacility().getId());
    }
    return basicRequisitionDto;
  }

  private boolean internalFacilityUser(BasicRequisitionDto basicRequisitionDto) {
    return authenticationHelper.getCurrentUser().getHomeFacilityId().equals(basicRequisitionDto.getFacility().getId());
  }

  @GetMapping("/statusDate")
  public List<SiglusRequisitionDateDto> getRequisitionStatusDates(
      @RequestParam List<UUID> requisitionIds) {
    return siglusRequisitionService.getRequisitionStatusDates(requisitionIds);
  }

  @PostMapping("/createLineItem")
  public List<SiglusRequisitionLineItemDto> createRequisitionLineItem(
      @RequestParam(value = "requisitionId") UUID requisitionId,
      @RequestBody List<UUID> orderableIds) {
    return siglusRequisitionService.createRequisitionLineItem(requisitionId, orderableIds);
  }

  @DeleteMapping("/deleteLineItem/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRequisitionLineItem(@PathVariable("id") UUID requisitionLineItemId) {
    siglusRequisitionService.deleteRequisitionLineItem(requisitionLineItemId);
  }

  @GetMapping("/periodsForInitiate")
  public List<RequisitionPeriodExtensionResponse> searchProcessingPeriodIds(
      @RequestParam(value = "programId") @NotNull @Valid UUID programId,
      @RequestParam(value = "facilityId") @NotNull @Valid UUID facilityId,
      @RequestParam(value = "emergency") boolean emergency) {
    return siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, facilityId, emergency);
  }

  @GetMapping("/requisitionsForApproval")
  public Page<BasicRequisitionDto> searchRequisitionsForApproval(
      @RequestParam(value = "program", required = false) UUID programId,
      @RequestParam(value = "facility", required = false) UUID facilityId,
      Pageable pageable) {
    return requisitionController.requisitionsForApproval(programId, facilityId, pageable);
  }

  @PostMapping("/closeRequisitionsForApproval")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void closeRequisitionsForApproval() {
    Pageable pageable = new PageRequest(DEFAULT_PAGE_NUMBER, NO_PAGINATION);
    org.openlmis.requisition.dto.UserDto currentUser = authenticationHelper.getCurrentUser();
    Page<Requisition> approvalRequisitions = requisitionService
        .getRequisitionsForApproval(currentUser, null, null, pageable);
    siglusRequisitionAutoCloseService.closeOldRequisitions(approvalRequisitions.getContent());
  }


  @GetMapping("/requisitionsForConvertToOrder")
  public Page<RequisitionWithSupplyingDepotsDto> searchRequisitionsForApprovalList(
      @RequestParam(value = "program", required = false) UUID programId,
      @RequestParam(value = "facility", required = false) UUID facilityId,
      Pageable pageable) {
    return siglusRequisitionService.getRequisitionsForConvertToOrder(programId, facilityId, pageable);
  }

  @GetMapping("/facilitiesForApproval")
  public List<FacilityDto> getFacilitiesForApproval() {
    return siglusRequisitionService.searchFacilitiesForApproval();
  }

  @GetMapping("/facilitiesForView")
  public List<FacilityDto> getFacilitiesForView() {
    return siglusRequisitionService.searchFacilitiesForView();
  }

  @GetMapping("/{id}/export")
  public void exportExcel(@PathVariable("id") UUID requisitionId, HttpServletResponse response) throws IOException {
    siglusRequisitionExportService.exportExcel(requisitionId, response);
  }
}
