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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.web.RequisitionController;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
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
@RequestMapping("/api/siglusapi/requisitions")
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusRequisitionController {

  @Autowired
  private RequisitionController requisitionController;

  @Autowired
  private SiglusRequisitionService siglusRequisitionService;

  @Autowired
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  @Autowired
  private SiglusNotificationService notificationService;

  @PostMapping("/initiate")
  @ResponseStatus(HttpStatus.CREATED)
  public SiglusRequisitionDto initiate(@RequestParam(value = "program") UUID programId,
      @RequestParam(value = "facility") UUID facilityId,
      @RequestParam(value = "suggestedPeriod", required = false) UUID suggestedPeriod,
      @RequestParam(value = "emergency") boolean emergency,
      @RequestParam(value = "physicalInventoryDate", required = false)
          String physicalInventoryDateStr,
      HttpServletRequest request, HttpServletResponse response) {
    return siglusRequisitionService.initiate(programId, facilityId, suggestedPeriod, emergency,
        physicalInventoryDateStr, request, response);
  }

  @GetMapping("/{id}")
  public SiglusRequisitionDto searchRequisition(@PathVariable("id") UUID requisitionId) {
    return siglusRequisitionService.searchRequisition(requisitionId);
  }

  @PutMapping("/{id}")
  public SiglusRequisitionDto updateRequisition(@PathVariable("id") UUID requisitionId,
      @RequestBody SiglusRequisitionDto requisitionDto,
      HttpServletRequest request, HttpServletResponse response) {
    return siglusRequisitionService
        .updateRequisition(requisitionId, requisitionDto, request, response);
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
    BasicRequisitionDto basicRequisitionDto = requisitionController
        .submitRequisition(requisitionId, request, response);
    notificationService.postSubmit(basicRequisitionDto);
    siglusRequisitionService
        .activateArchivedProducts(requisitionId, basicRequisitionDto.getFacility().getId());
    return basicRequisitionDto;
  }

  @PostMapping("/{id}/authorize")
  public BasicRequisitionDto authorizeRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    return siglusRequisitionService.authorizeRequisition(requisitionId, request, response);
  }

  @PostMapping("/{id}/approve")
  public BasicRequisitionDto approveRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    return siglusRequisitionService.approveRequisition(requisitionId, request, response);
  }

  /**
   * why we redo this api? for bug card #228, change dependency:
   * {@linkplain org.openlmis.requisition.domain.requisition.Requisition#reject(Map, UUID)}  method}
   * requisition.reject->updateConsumptions(products)->
   * {@linkplain
   * org.openlmis.requisition.domain.requisition.Requisition#filterLineItems(Boolean, Boolean, Map)}
   * method}
   * getNonSkippedFullSupplyRequisitionLineItems->filterLineItems
   */
  @PutMapping("/{id}/reject")
  public BasicRequisitionDto rejectRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    BasicRequisitionDto dto =
        requisitionController.rejectRequisition(requisitionId, request, response);
    siglusRequisitionService.deleteExtensionForRequisition(requisitionId);
    notificationService.postReject(dto);
    return dto;
  }

  @PostMapping("/createLineItem")
  public List<SiglusRequisitionLineItemDto> createRequisitionLineItem(
      @RequestParam(value = "requisitionId") UUID requisitionId,
      @RequestBody List<UUID> orderableIds) {
    return siglusRequisitionService.createRequisitionLineItem(requisitionId, orderableIds);
  }

  @GetMapping("/periodsForInitiate")
  public Collection<RequisitionPeriodDto> searchProcessingPeriodIds(
      @RequestParam(value = "programId") UUID programId,
      @RequestParam(value = "facilityId") UUID facilityId,
      @RequestParam(value = "emergency") boolean emergency) {

    return siglusProcessingPeriodService.getPeriods(
        programId, facilityId, emergency
    );
  }

  @GetMapping("/requisitionsForApproval")
  public Page<BasicRequisitionDto> searchRequisitionsForApproval(
      @RequestParam(value = "program", required = false) UUID programId,
      @RequestParam(value = "facility", required = false) UUID facilityId,
      Pageable pageable) {
    return requisitionController.requisitionsForApproval(programId, facilityId, pageable);
  }

  @GetMapping("/facilitiesForApproval")
  public Set<FacilityDto> getFacilitiesForApproval() {
    return siglusRequisitionService.searchFacilitiesForApproval();
  }

}
