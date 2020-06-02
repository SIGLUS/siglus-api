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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.requisition.web.RequisitionV2Controller;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/siglusapi/requisitions")
public class SiglusRequisitionController {

  @Autowired
  private RequisitionController requisitionController;

  @Autowired
  private RequisitionV2Controller requisitionV2Controller;

  @Autowired
  private SiglusRequisitionService siglusRequisitionService;

  @PostMapping("/initiate")
  @ResponseStatus(HttpStatus.CREATED)
  public RequisitionV2Dto initiate(@RequestParam(value = "program") UUID programId,
      @RequestParam(value = "facility") UUID facilityId,
      @RequestParam(value = "suggestedPeriod", required = false) UUID suggestedPeriod,
      @RequestParam(value = "emergency") boolean emergency,
      @RequestParam(value = "physicalInventoryDate", required = false)
          String physicalInventoryDateStr,
      HttpServletRequest request, HttpServletResponse response) {
    return requisitionV2Controller.initiate(programId, facilityId, suggestedPeriod, emergency,
        physicalInventoryDateStr, request, response);
  }

  @GetMapping("/{id}")
  public RequisitionV2Dto searchRequisition(@PathVariable("id") UUID requisitionId) {
    return siglusRequisitionService.searchRequisition(requisitionId);
  }

  @PostMapping("/{id}/submit")
  public BasicRequisitionDto submitRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    return requisitionController.submitRequisition(requisitionId, request, response);
  }

  @PostMapping("/{id}/authorize")
  public BasicRequisitionDto authorizeRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    return requisitionController.authorizeRequisition(requisitionId, request, response);
  }

  @PostMapping("/createLineItem")
  public List<RequisitionLineItemV2Dto> createRequisitionLineItem(
      @RequestParam(value = "requisitionId") UUID requisitonId,
      @RequestBody List<UUID> orderableIds) {
    return siglusRequisitionService.createRequisitionLineItem(requisitonId, orderableIds);
  }
}
