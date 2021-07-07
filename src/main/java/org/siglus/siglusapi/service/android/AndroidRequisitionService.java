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

package org.siglus.siglusapi.service.android;

import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ExtraDataConstants.CLIENT_SUBMITTED_TIME;
import static org.siglus.common.constant.ExtraDataConstants.IS_SAVED;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequisitionTemplateService;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.ExtraDataSignatureDto;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AndroidRequisitionService {

  @Value("${android.via.templateId}")
  private String androidViaTemplateId;

  private final RequisitionTemplateService requisitionTemplateService;
  private final SiglusProgramService siglusProgramService;
  private final SiglusOrderableService siglusOrderableService;
  private final SiglusAuthenticationHelper authHelper;
  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  private final RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  private final RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;
  private final RequisitionRepository requisitionRepository;

  @Transactional
  public void create(RequisitionRequest request) {
    initiateRequisition(request);
  }

  private void initiateRequisition(RequisitionRequest request) {
    RequisitionTemplate requisitionTemplate = requisitionTemplateService
        .findTemplateById(UUID.fromString(androidViaTemplateId));
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisitionTemplate.getId());
    requisitionTemplate.setTemplateExtension(templateExtension);
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    UUID programId = siglusProgramService.getProgramIdByCode(request.getProgramCode());
    Requisition newRequisition = RequisitionBuilder.newRequisition(homeFacilityId, programId, request.getEmergency());
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    // TODO: find Android peridId by Android period start date
    UUID initiator = authHelper.getCurrentUser().getId();
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newRequisition.getStatusChanges().add(StatusChange.newStatusChange(newRequisition, initiator));
    newRequisition.setProcessingPeriodId(UUID.fromString("1934880e-d955-11eb-afc2-acde48001122"));
    newRequisition.setReportOnly(false);
    buildExtraData(newRequisition, request);
    buildRequisitionLineItems(newRequisition, request);
    log.info("save android requisition: {}", newRequisition);
    Requisition requisition = requisitionRepository.save(newRequisition);
    initiateRequisitionNumber(requisition);
    initiateAuthorizedQuantity(requisition, request);
  }

  private void initiateRequisitionNumber(Requisition requisition) {
    siglusRequisitionExtensionService.createRequisitionExtension(requisition.getId(), requisition.getEmergency(),
        requisition.getFacilityId());
  }

  private void initiateAuthorizedQuantity(Requisition requisition, RequisitionRequest request) {
    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
      extension.setRequisitionLineItemId(requisitionLineItem.getId());
      Integer authorizedQuantity = request.getProducts().stream()
          .filter(product -> siglusOrderableService.getOrderableByCode(product.getProductCode()).getId()
              .equals(requisitionLineItem.getOrderable().getId()))
          .findFirst()
          .map(RequisitionLineItemRequest::getAuthorizedQuantity)
          .orElse(null);
      extension.setAuthorizedQuantity(authorizedQuantity);
      log.info("save requisition line item extensions: {}", extension);
      requisitionLineItemExtensionRepository.save(extension);
    });
  }

  private void buildExtraData(Requisition requisition, RequisitionRequest request) {
    Map<String, Object> extraData = new HashMap<>();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    extraData.put(ACTUAL_START_DATE, request.getActualStartDate().format(dateTimeFormatter));
    extraData.put(ACTUAL_END_DATE, request.getActualEndDate().format(dateTimeFormatter));
    extraData.put(CLIENT_SUBMITTED_TIME, request.getClientSubmittedTime());
    extraData.put(SIGNATURE, buildSignature(request));
    extraData.put(IS_SAVED, false);
    requisition.setExtraData(extraData);
  }

  private ExtraDataSignatureDto buildSignature(RequisitionRequest request) {
    String submitter = getSignatureNameByEventType(request, "SUBMITTER");
    String approver = getSignatureNameByEventType(request, "APPROVER");
    return ExtraDataSignatureDto.builder()
        .submit(submitter)
        .authorize(submitter)
        .approve(new String[]{approver})
        .build();
  }

  private String getSignatureNameByEventType(RequisitionRequest request, String eventType) {
    RequisitionSignatureRequest signatureRequest = request.getSignatures().stream()
        .filter(signature -> eventType.equals(signature.getType()))
        .findFirst()
        .orElseThrow(() -> new ValidationMessageException("signature missed"));
    return signatureRequest.getName();
  }

  private void buildRequisitionLineItems(Requisition requisition, RequisitionRequest request) {
    List<RequisitionLineItem> requisitionLineItems = new ArrayList<>();
    for (RequisitionLineItemRequest product : request.getProducts()) {
      OrderableDto orderableDto = siglusOrderableService.getOrderableByCode(product.getProductCode());
      RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
      requisitionLineItem.setOrderable(new VersionEntityReference(orderableDto.getId(),
          orderableDto.getVersionNumber()));
      requisitionLineItem.setBeginningBalance(product.getBeginningBalance());
      requisitionLineItem.setTotalReceivedQuantity(product.getTotalReceivedQuantity());
      requisitionLineItem.setTotalConsumedQuantity(product.getTotalConsumedQuantity());
      requisitionLineItem.setStockOnHand(product.getStockOnHand());
      requisitionLineItem.setRequestedQuantity(product.getRequestedQuantity());
      requisitionLineItems.add(requisitionLineItem);
    }
    requisition.setRequisitionLineItems(requisitionLineItems);
  }

}
