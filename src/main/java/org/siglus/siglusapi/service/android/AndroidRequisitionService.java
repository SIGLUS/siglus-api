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

import static org.openlmis.requisition.web.ResourceNames.PROCESSING_PERIODS;
import static org.openlmis.requisition.web.ResourceNames.PROGRAMS;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ExtraDataConstants.CLIENT_SUBMITTED_TIME;
import static org.siglus.common.constant.ExtraDataConstants.IS_SAVED;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;
import static org.siglus.siglusapi.constant.AndroidConstants.PERIOD_NAME_FORMATTER;
import static org.siglus.siglusapi.constant.AndroidConstants.SCHEDULE_CODE;
import static org.siglus.siglusapi.domain.UsageCategory.CONSULTATIONNUMBER;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.RequisitionTemplateService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.repository.ProcessingPeriodRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.ExtraDataSignatureDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class AndroidRequisitionService {

  @Value("${android.via.templateId}")
  private String androidViaTemplateId;

  private final SiglusAuthenticationHelper authHelper;
  private final RequisitionService requisitionService;
  private final RequisitionTemplateService requisitionTemplateService;
  private final SiglusProgramService siglusProgramService;
  private final SiglusOrderableService siglusOrderableService;
  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  private final SupervisoryNodeReferenceDataService supervisoryNodeService;
  private final SiglusUsageReportService siglusUsageReportService;
  private final RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  private final RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;
  private final RequisitionRepository requisitionRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;

  @Transactional
  public UUID create(RequisitionCreateRequest request) {
    Requisition requisition = initiateRequisition(request);
    requisition = submitRequisition(requisition);
    requisition = authorizeRequisition(requisition);
    requisition = internalApproveRequisition(requisition);
    return requisition.getId();
  }

  private Requisition initiateRequisition(RequisitionCreateRequest request) {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    UUID programId = siglusProgramService.getProgramIdByCode(request.getProgramCode());
    Requisition newRequisition = RequisitionBuilder.newRequisition(homeFacilityId, programId, request.getEmergency());
    newRequisition.setTemplate(getRequisitionTemplate());
    UUID initiator = authHelper.getCurrentUser().getId();
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newRequisition.getStatusChanges().add(StatusChange.newStatusChange(newRequisition, initiator));
    newRequisition.setProcessingPeriodId(getPeriodId(request));
    newRequisition.setReportOnly(false);
    newRequisition.setNumberOfMonthsInPeriod(1);
    buildRequisitionApprovedProduct(newRequisition, request);
    buildRequisitionExtraData(newRequisition, request);
    buildRequisitionLineItems(newRequisition, request);
    log.info("initiate android requisition: {}", newRequisition);
    Requisition requisition = requisitionRepository.save(newRequisition);
    buildRequisitionExtension(requisition);
    buildRequisitionLineItemsExtension(requisition, request);
    buildRequisitionConsultationNumber(requisition, request);
    return requisition;
  }

  private UUID getPeriodId(RequisitionCreateRequest request) {
    String periodName = PERIOD_NAME_FORMATTER.format(request.getActualStartDate());
    return processingPeriodRepository.findPeriodByCodeAndName(SCHEDULE_CODE, periodName)
        .map(BaseEntity::getId)
        .orElseThrow(EntityNotFoundException::new);
  }

  private Requisition submitRequisition(Requisition requisition) {
    UUID submitter = authHelper.getCurrentUser().getId();
    requisition.setStatus(RequisitionStatus.SUBMITTED);
    requisition.getStatusChanges().add(StatusChange.newStatusChange(requisition, submitter));
    log.info("submit android requisition: {}", requisition);
    requisition.setModifiedDate(ZonedDateTime.now());
    return requisitionRepository.save(requisition);
  }

  private Requisition authorizeRequisition(Requisition requisition) {
    UUID supervisoryNodeId = supervisoryNodeService.findSupervisoryNode(
        requisition.getProgramId(), requisition.getFacilityId()).getId();
    requisition.setSupervisoryNodeId(supervisoryNodeId);
    UUID authorizer = authHelper.getCurrentUser().getId();
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    requisition.getStatusChanges().add(StatusChange.newStatusChange(requisition, authorizer));
    requisition.setModifiedDate(ZonedDateTime.now());
    log.info("authorize android requisition: {}", requisition);
    return requisitionRepository.save(requisition);
  }

  private Requisition internalApproveRequisition(Requisition requisition) {
    SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeService.findOne(requisition.getSupervisoryNodeId());
    requisition.setSupervisoryNodeId(supervisoryNodeDto.getParentNodeId());
    UUID approver = authHelper.getCurrentUser().getId();
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    requisition.getStatusChanges().add(StatusChange.newStatusChange(requisition, approver));
    requisition.setModifiedDate(ZonedDateTime.now());
    log.info("internal-approve android requisition: {}", requisition);
    return requisitionRepository.save(requisition);
  }

  private RequisitionTemplate getRequisitionTemplate() {
    RequisitionTemplate requisitionTemplate = requisitionTemplateService
        .findTemplateById(UUID.fromString(androidViaTemplateId));
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisitionTemplate.getId());
    requisitionTemplate.setTemplateExtension(templateExtension);
    return requisitionTemplate;
  }

  private void buildRequisitionExtension(Requisition requisition) {
    siglusRequisitionExtensionService.createRequisitionExtension(requisition.getId(), requisition.getEmergency(),
        requisition.getFacilityId());
  }

  private void buildRequisitionLineItemsExtension(Requisition requisition,
      RequisitionCreateRequest requisitionRequest) {
    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
      extension.setRequisitionLineItemId(requisitionLineItem.getId());
      Integer authorizedQuantity = requisitionRequest.getProducts().stream()
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

  private void buildRequisitionExtraData(Requisition requisition, RequisitionCreateRequest requisitionRequest) {
    Map<String, Object> extraData = new HashMap<>();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    extraData.put(ACTUAL_START_DATE, requisitionRequest.getActualStartDate().format(dateTimeFormatter));
    extraData.put(ACTUAL_END_DATE, requisitionRequest.getActualEndDate().format(dateTimeFormatter));
    extraData.put(CLIENT_SUBMITTED_TIME, requisitionRequest.getClientSubmittedTime().toString());
    extraData.put(SIGNATURE, buildSignature(requisitionRequest));
    extraData.put(IS_SAVED, false);
    requisition.setExtraData(extraData);
  }

  private ExtraDataSignatureDto buildSignature(RequisitionCreateRequest requisitionRequest) {
    String submitter = getSignatureNameByEventType(requisitionRequest, "SUBMITTER");
    String approver = getSignatureNameByEventType(requisitionRequest, "APPROVER");
    return ExtraDataSignatureDto.builder()
        .submit(submitter)
        .authorize(submitter)
        .approve(new String[]{approver})
        .build();
  }

  private String getSignatureNameByEventType(RequisitionCreateRequest requisitionRequest, String eventType) {
    RequisitionSignatureRequest signatureRequest = requisitionRequest.getSignatures().stream()
        .filter(signature -> eventType.equals(signature.getType()))
        .findFirst()
        .orElseThrow(() -> new ValidationMessageException("signature missed"));
    return signatureRequest.getName();
  }

  private void buildRequisitionLineItems(Requisition requisition, RequisitionCreateRequest requisitionRequest) {
    List<RequisitionLineItem> requisitionLineItems = new ArrayList<>();
    for (RequisitionLineItemRequest product : requisitionRequest.getProducts()) {
      OrderableDto orderableDto = siglusOrderableService.getOrderableByCode(product.getProductCode());
      RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
      requisitionLineItem.setRequisition(requisition);
      requisitionLineItem.setOrderable(new VersionEntityReference(orderableDto.getId(),
          orderableDto.getVersionNumber()));
      requisitionLineItem.setBeginningBalance(product.getBeginningBalance());
      requisitionLineItem.setTotalReceivedQuantity(product.getTotalReceivedQuantity());
      requisitionLineItem.setTotalConsumedQuantity(product.getTotalConsumedQuantity());
      requisitionLineItem.setStockOnHand(product.getStockOnHand());
      requisitionLineItem.setRequestedQuantity(product.getRequestedQuantity());
      VersionEntityReference approvedProduct = requisition.getAvailableProducts().stream()
          .filter(approvedProductReference -> approvedProductReference.getOrderable().getId()
              .equals(requisitionLineItem.getOrderable().getId()))
          .findFirst()
          .orElseThrow(NullPointerException::new)
          .getFacilityTypeApprovedProduct();
      requisitionLineItem.setFacilityTypeApprovedProduct(
          new VersionEntityReference(approvedProduct.getId(), approvedProduct.getVersionNumber()));
      requisitionLineItems.add(requisitionLineItem);
    }
    requisition.setRequisitionLineItems(requisitionLineItems);
  }

  private void buildRequisitionApprovedProduct(Requisition requisition, RequisitionCreateRequest requisitionRequest) {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    UUID programId = siglusProgramService.getProgramIdByCode(requisitionRequest.getProgramCode());
    ApproveProductsAggregator approvedProductsContainKit = requisitionService
        .getApproveProduct(homeFacilityId, programId, false);
    List<ApprovedProductDto> approvedProductDtos = approvedProductsContainKit.getFullSupplyProducts();
    ApproveProductsAggregator approvedProducts = new ApproveProductsAggregator(approvedProductDtos, programId);
    Set<ApprovedProductReference> availableProductIdentities = approvedProducts.getApprovedProductReferences();
    requisition.setAvailableProducts(availableProductIdentities);
  }

  private void buildRequisitionConsultationNumber(Requisition requisition, RequisitionCreateRequest request) {
    RequisitionV2Dto dto = new RequisitionV2Dto();
    requisition.export(dto);
    BasicRequisitionTemplateDto templateDto = BasicRequisitionTemplateDto.newInstance(requisition.getTemplate());
    templateDto.setExtension(RequisitionTemplateExtensionDto.from(requisition.getTemplate().getTemplateExtension()));
    dto.setTemplate(templateDto);
    dto.setProcessingPeriod(new ObjectReferenceDto(requisition.getProcessingPeriodId(), "", PROCESSING_PERIODS));
    dto.setProgram(new ObjectReferenceDto(requisition.getProgramId(), "", PROGRAMS));
    SiglusRequisitionDto requisitionDto = siglusUsageReportService.initiateUsageReport(dto);
    requisitionDto.getConsultationNumberLineItems().stream()
        .findFirst()
        .orElseThrow(NullPointerException::new)
        .getColumns()
        .get(CONSULTATIONNUMBER.getName())
        .setValue(request.getConsultationNumber());
    siglusUsageReportService.saveUsageReport(requisitionDto, dto);
  }

}
