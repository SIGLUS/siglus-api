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

import static org.openlmis.requisition.web.ResourceNames.ORDERABLES;
import static org.openlmis.requisition.web.ResourceNames.PROCESSING_PERIODS;
import static org.openlmis.requisition.web.ResourceNames.PROGRAMS;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ExtraDataConstants.CLIENT_SUBMITTED_TIME;
import static org.siglus.common.constant.ExtraDataConstants.IS_SAVED;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;
import static org.siglus.common.constant.KitConstants.ALL_KITS;
import static org.siglus.common.constant.KitConstants.KIT_26A01;
import static org.siglus.common.constant.KitConstants.KIT_26A02;
import static org.siglus.siglusapi.constant.AndroidConstants.SCHEDULE_CODE;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.COLUMN_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.GROUP_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.COLLECTION_KIT_OPENED;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.COLLECTION_KIT_RECEIVED;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.SERVICE_CHW;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.SERVICE_HF;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.CONTAIN_DM;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.CONTAIN_DS;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.CONTAIN_DT;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_4;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_4;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_5;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_6;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_7;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.PATIENT_TYPE;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DM_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TOTAL_COLUMN;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_COMMUNITY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_PATIENT;

import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.RequisitionTemplateService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.exception.NotFoundException;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.repository.ProcessingPeriodRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.config.AndroidTemplateConfigProperties;
import org.siglus.siglusapi.constant.UsageSectionConstants.UsageInformationLineItems;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.dto.ExtraDataSignatureDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;
import org.siglus.siglusapi.dto.android.androidenum.NewSection0;
import org.siglus.siglusapi.dto.android.androidenum.NewSection1;
import org.siglus.siglusapi.dto.android.androidenum.NewSection2;
import org.siglus.siglusapi.dto.android.androidenum.NewSection3;
import org.siglus.siglusapi.dto.android.androidenum.NewSection4;
import org.siglus.siglusapi.dto.android.androidenum.PatientLineItemName;
import org.siglus.siglusapi.dto.android.androidenum.PatientType;
import org.siglus.siglusapi.dto.android.androidenum.RegimenSummaryCode;
import org.siglus.siglusapi.dto.android.request.PatientLineItemColumnRequest;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.RegimenLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.dto.android.request.UsageInformationLineItemRequest;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class AndroidCreateRequisitionService {

  private final AndroidTemplateConfigProperties androidTemplateConfigProperties;
  private final SiglusAuthenticationHelper authHelper;
  private final RequisitionService requisitionService;
  private final RequisitionTemplateService requisitionTemplateService;
  private final SiglusProgramService siglusProgramService;
  private final SiglusOrderableService siglusOrderableService;
  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  private final SupervisoryNodeReferenceDataService supervisoryNodeService;
  private final SiglusUsageReportService siglusUsageReportService;
  private final PermissionService permissionService;
  private final RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  private final RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;
  private final RequisitionRepository requisitionRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final RegimenRepository regimenRepository;
  private final SyncUpHashRepository syncUpHashRepository;

  @Transactional
  public void create(RequisitionCreateRequest request) {
    UserDto user = authHelper.getCurrentUser();
    String syncUpHash = request.getSyncUpHash(user);
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip create requisition as syncUpHash: {} existed", syncUpHash);
      return;
    }
    UUID authorId = user.getId();
    Requisition requisition = initiateRequisition(request, user);
    requisition = submitRequisition(requisition, authorId);
    requisition = authorizeRequisition(requisition, authorId);
    internalApproveRequisition(requisition, authorId);
    log.info("save requisition syncUpHash: {}", syncUpHash);
    syncUpHashRepository.save(new SyncUpHash(syncUpHash));
  }

  private Requisition initiateRequisition(RequisitionCreateRequest request, UserDto user) {
    String programCode = request.getProgramCode();
    UUID programId = siglusProgramService.getProgramIdByCode(programCode);
    UUID homeFacilityId = user.getHomeFacilityId();
    checkPermission(() -> permissionService.canInitRequisition(programId, homeFacilityId));
    Requisition newRequisition = RequisitionBuilder.newRequisition(homeFacilityId, programId, request.getEmergency());
    newRequisition.setTemplate(getRequisitionTemplate(programCode));
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newRequisition.setProcessingPeriodId(getPeriodId(request));
    newRequisition.setNumberOfMonthsInPeriod(1);
    newRequisition.setDraftStatusMessage(request.getComments());
    newRequisition.setReportOnly("ML".equals(programCode));
    buildStatusChanges(newRequisition, user.getId());
    buildRequisitionApprovedProduct(newRequisition, homeFacilityId, programId);
    buildRequisitionExtraData(newRequisition, request);
    buildRequisitionLineItems(newRequisition, request);
    log.info("initiate android requisition: {}", newRequisition);
    Requisition requisition = requisitionRepository.save(newRequisition);
    buildRequisitionExtension(requisition, request);
    buildRequisitionLineItemsExtension(requisition, request);
    buildRequisitionUsageSections(requisition, request, programId);
    return requisition;
  }

  private Requisition submitRequisition(Requisition requisition, UUID authorId) {
    checkPermission(() -> permissionService.canSubmitRequisition(requisition));
    requisition.setModifiedDate(ZonedDateTime.now());
    requisition.setStatus(RequisitionStatus.SUBMITTED);
    buildStatusChanges(requisition, authorId);
    log.info("submit android requisition: {}", requisition);
    return requisitionRepository.save(requisition);
  }

  private Requisition authorizeRequisition(Requisition requisition, UUID authorId) {
    checkPermission(() -> permissionService.canAuthorizeRequisition(requisition));
    UUID supervisoryNodeId = supervisoryNodeService.findSupervisoryNode(
        requisition.getProgramId(), requisition.getFacilityId()).getId();
    requisition.setSupervisoryNodeId(supervisoryNodeId);
    requisition.setModifiedDate(ZonedDateTime.now());
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    buildStatusChanges(requisition, authorId);
    log.info("authorize android requisition: {}", requisition);
    return requisitionRepository.save(requisition);
  }

  private void internalApproveRequisition(Requisition requisition, UUID authorId) {
    checkPermission(() -> permissionService.canApproveRequisition(requisition));
    SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeService.findOne(requisition.getSupervisoryNodeId());
    requisition.setSupervisoryNodeId(supervisoryNodeDto.getParentNodeId());
    requisition.setModifiedDate(ZonedDateTime.now());
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    buildStatusChanges(requisition, authorId);
    log.info("internal-approve android requisition: {}", requisition);
    requisitionRepository.save(requisition);
  }

  private void checkPermission(Supplier<ValidationResult> supplier) {
    supplier.get().throwExceptionIfHasErrors();
  }

  private void buildStatusChanges(Requisition requisition, UUID authorId) {
    requisition.getStatusChanges().add(StatusChange.newStatusChange(requisition, authorId));
  }

  private UUID getPeriodId(RequisitionCreateRequest request) {
    YearMonth month = request.getActualStartDate().query(YearMonth::from);
    return processingPeriodRepository.findPeriodByCodeAndMonth(SCHEDULE_CODE, month)
        .map(BaseEntity::getId)
        .orElseThrow(EntityNotFoundException::new);
  }

  private RequisitionTemplate getRequisitionTemplate(String programCode) {
    RequisitionTemplate requisitionTemplate = requisitionTemplateService
        .findTemplateById(androidTemplateConfigProperties.findAndroidTemplateId(programCode));
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisitionTemplate.getId());
    requisitionTemplate.setTemplateExtension(templateExtension);
    return requisitionTemplate;
  }

  private void buildRequisitionExtension(Requisition requisition, RequisitionCreateRequest request) {
    RequisitionExtension requisitionExtension = siglusRequisitionExtensionService
        .buildRequisitionExtension(requisition.getId(), requisition.getEmergency(), requisition.getFacilityId());
    requisitionExtension.setIsApprovedByInternal(true);
    requisitionExtension.setActualStartDate(request.getActualStartDate());
    log.info("save requisition extension: {}", requisitionExtension);
    requisitionExtensionRepository.save(requisitionExtension);
  }

  private void buildRequisitionLineItemsExtension(Requisition requisition,
      RequisitionCreateRequest requisitionRequest) {
    if (CollectionUtils.isEmpty(requisition.getRequisitionLineItems())) {
      return;
    }
    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
      extension.setRequisitionLineItemId(requisitionLineItem.getId());
      RequisitionLineItemRequest requisitionProduct = requisitionRequest.getProducts().stream()
          .filter(product -> siglusOrderableService.getOrderableByCode(product.getProductCode()).getId()
              .equals(requisitionLineItem.getOrderable().getId()))
          .findFirst()
          .orElse(new RequisitionLineItemRequest());
      extension.setAuthorizedQuantity(requisitionProduct.getAuthorizedQuantity());
      extension.setExpirationDate(requisitionProduct.getExpirationDate());
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
    if (CollectionUtils.isEmpty(requisitionRequest.getProducts())) {
      return;
    }
    List<RequisitionLineItem> requisitionLineItems = new ArrayList<>();
    for (RequisitionLineItemRequest product : requisitionRequest.getProducts()) {
      OrderableDto orderableDto = siglusOrderableService.getOrderableByCode(product.getProductCode());
      RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
      requisitionLineItem.setRequisition(requisition);
      requisitionLineItem.setOrderable(new VersionEntityReference(orderableDto.getId(),
          orderableDto.getVersionNumber()));
      requisitionLineItem.setBeginningBalance(product.getBeginningBalance());
      requisitionLineItem.setTotalLossesAndAdjustments(product.getTotalLossesAndAdjustments());
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
      boolean isKit = ALL_KITS.contains(product.getProductCode());
      requisitionLineItem.setSkipped(isKit);
      requisitionLineItems.add(requisitionLineItem);
    }
    requisition.setRequisitionLineItems(requisitionLineItems);
  }

  private void buildRequisitionApprovedProduct(Requisition requisition, UUID homeFacilityId, UUID programId) {
    ApproveProductsAggregator approvedProductsContainKit = requisitionService
        .getApproveProduct(homeFacilityId, programId, requisition.getReportOnly());
    List<ApprovedProductDto> approvedProductDtos = approvedProductsContainKit.getFullSupplyProducts();
    ApproveProductsAggregator approvedProducts = new ApproveProductsAggregator(approvedProductDtos, programId);
    Set<ApprovedProductReference> availableProductIdentities = approvedProducts.getApprovedProductReferences();
    requisition.setAvailableProducts(availableProductIdentities);
  }

  private void buildRequisitionUsageSections(Requisition requisition, RequisitionCreateRequest request,
      UUID programId) {
    RequisitionV2Dto dto = new RequisitionV2Dto();
    requisition.export(dto);
    BasicRequisitionTemplateDto templateDto = BasicRequisitionTemplateDto.newInstance(requisition.getTemplate());
    templateDto.setExtension(RequisitionTemplateExtensionDto.from(requisition.getTemplate().getTemplateExtension()));
    dto.setTemplate(templateDto);
    dto.setProcessingPeriod(new ObjectReferenceDto(requisition.getProcessingPeriodId(), "", PROCESSING_PERIODS));
    dto.setProgram(new ObjectReferenceDto(requisition.getProgramId(), "", PROGRAMS));
    buildAvailableProducts(dto, requisition);
    SiglusRequisitionDto requisitionDto = siglusUsageReportService.initiateUsageReport(dto);
    buildConsultationNumber(requisitionDto, request);
    buildRequisitionKitUsage(requisitionDto, request);
    updateRegimenLineItems(requisitionDto, programId, request);
    updateRegimenSummaryLineItems(requisitionDto, request);
    updatePatientLineItems(requisitionDto, request);
    updateUsageInformationLineItems(requisitionDto, request);
    siglusUsageReportService.saveUsageReport(requisitionDto, dto);
  }

  private void updatePatientLineItems(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (CollectionUtils.isEmpty(request.getPatientLineItems())) {
      return;
    }
    Map<String, PatientGroupDto> patientNameToPatientGroupDto = requisitionDto.getPatientLineItems().stream()
        .collect(Collectors.toMap(PatientGroupDto::getName, Function.identity()));
    List<PatientLineItemsRequest> patientLineItemsRequests = request.getPatientLineItems();
    splitTableDispensedPatientData(patientLineItemsRequests);
    patientLineItemsRequests.forEach(patientRequest ->
        buildPatientGroupDtoData(
            patientNameToPatientGroupDto.get(PatientLineItemName.findValueByKey(patientRequest.getName())),
            patientRequest)
    );
    calculatePatientDispensedTotal(patientNameToPatientGroupDto);
  }

  private void splitTableDispensedPatientData(List<PatientLineItemsRequest> patientLineItemsRequests) {
    PatientLineItemsRequest dispensed = patientLineItemsRequests.stream()
        .filter(p -> TABLE_DISPENSED_KEY.equals(p.getName()))
        .findFirst()
        .orElse(null);
    if (dispensed != null) {
      List<PatientLineItemColumnRequest> dsList = new ArrayList<>();
      List<PatientLineItemColumnRequest> dtList = new ArrayList<>();
      List<PatientLineItemColumnRequest> dmList = new ArrayList<>();
      dispensed.getColumns().forEach(v -> {
        if (v.getName().contains(CONTAIN_DS)) {
          dsList.add(v);
        } else if (v.getName().contains(CONTAIN_DT)) {
          dtList.add(v);
        } else if (v.getName().contains(CONTAIN_DM)) {
          dmList.add(v);
        }
      });
      patientLineItemsRequests.add(new PatientLineItemsRequest(TABLE_DISPENSED_DS_KEY, dsList));
      patientLineItemsRequests.add(new PatientLineItemsRequest(TABLE_DISPENSED_DT_KEY, dtList));
      patientLineItemsRequests.add(new PatientLineItemsRequest(TABLE_DISPENSED_DM_KEY, dmList));
      patientLineItemsRequests.remove(dispensed);
    }
  }

  private void buildPatientGroupDtoData(PatientGroupDto patientGroupDto, PatientLineItemsRequest patientRequest) {
    if (patientGroupDto == null) {
      throw new NotFoundException("patientGroupDto not found");
    }
    Map<String, PatientColumnDto> patientGroupDtoColumns = patientGroupDto.getColumns();
    List<PatientLineItemColumnRequest> patientRequestColumns = patientRequest.getColumns();
    patientRequestColumns.forEach(k -> {
      String name = patientGroupDto.getName();
      String patientGroupDtoKey = null;
      if (PATIENT_TYPE.equals(name)) {
        patientGroupDtoKey = PatientType.findValueByKey(k.getName());
      } else if (NEW_SECTION_0.equals(name)) {
        patientGroupDtoKey = NewSection0.findValueByKey(k.getName());
      } else if (NEW_SECTION_1.equals(name)) {
        patientGroupDtoKey = NewSection1.findValueByKey(k.getName());
      } else if (NEW_SECTION_2.equals(name)) {
        patientGroupDtoKey = NewSection2.findValueByKey(k.getName());
      } else if (NEW_SECTION_3.equals(name)) {
        patientGroupDtoKey = NewSection3.findValueByKey(k.getName());
      } else if (NEW_SECTION_4.equals(name)) {
        patientGroupDtoKey = NewSection4.findValueByKey(k.getName());
      }
      PatientColumnDto patientColumnDto = patientGroupDtoColumns.get(patientGroupDtoKey);
      patientColumnDto.setValue(k.getValue());
    });
  }

  private void calculatePatientDispensedTotal(Map<String, PatientGroupDto> patientNameToPatientGroupDto) {
    PatientGroupDto patientGroupDtoSection5 = patientNameToPatientGroupDto.get(NEW_SECTION_5);
    PatientColumnDto section5TotalDto = patientGroupDtoSection5.getColumns().get(TOTAL_COLUMN);
    if (section5TotalDto.getValue() == null) {
      section5TotalDto.setValue(0);
    }
    calculatePatientDispensedTotalBySection(patientNameToPatientGroupDto.get(NEW_SECTION_2), patientGroupDtoSection5,
        NEW_SECTION_2);
    calculatePatientDispensedTotalBySection(patientNameToPatientGroupDto.get(NEW_SECTION_3), patientGroupDtoSection5,
        NEW_SECTION_3);
    calculatePatientDispensedTotalBySection(patientNameToPatientGroupDto.get(NEW_SECTION_4), patientGroupDtoSection5,
        NEW_SECTION_4);

    PatientGroupDto patientGroupDtoSection6 = patientNameToPatientGroupDto.get(NEW_SECTION_6);
    Integer section2TotalValue = patientNameToPatientGroupDto.get(NEW_SECTION_2).getColumns().get(TOTAL_COLUMN)
        .getValue();
    Integer section3TotalValue = patientNameToPatientGroupDto.get(NEW_SECTION_3).getColumns().get(TOTAL_COLUMN)
        .getValue();
    Integer section4TotalValue = patientNameToPatientGroupDto.get(NEW_SECTION_4).getColumns().get(TOTAL_COLUMN)
        .getValue();

    patientGroupDtoSection6.getColumns().get(NEW_COLUMN).setValue(section2TotalValue);
    patientGroupDtoSection6.getColumns().get(NEW_COLUMN_0).setValue(section3TotalValue);
    patientGroupDtoSection6.getColumns().get(NEW_COLUMN_1).setValue(section4TotalValue);
    patientGroupDtoSection6.getColumns().get(TOTAL_COLUMN)
        .setValue(section2TotalValue + section3TotalValue + section4TotalValue);

    PatientGroupDto patientGroupDtoSection7 = patientNameToPatientGroupDto.get(NEW_SECTION_7);
    patientGroupDtoSection7.getColumns().get(NEW_COLUMN).setValue(
        Math.round(Float.valueOf(patientGroupDtoSection6.getColumns().get(TOTAL_COLUMN).getValue())
            / Float.valueOf(patientGroupDtoSection5.getColumns().get(TOTAL_COLUMN).getValue())));
  }

  private void calculatePatientDispensedTotalBySection(PatientGroupDto patientGroupDtoSection,
      PatientGroupDto patientGroupDtoSection5, String sectionKey) {
    PatientColumnDto sectionTotalDto = patientGroupDtoSection.getColumns().get(TOTAL_COLUMN);
    if (sectionTotalDto.getValue() == null) {
      sectionTotalDto.setValue(0);
    }
    patientGroupDtoSection.getColumns().forEach((k, v) -> {
      if (TOTAL_COLUMN.equals(k)) {
        return;
      }
      int updateValue = v.getValue();
      int totalValue = sectionTotalDto.getValue();
      sectionTotalDto.setValue(totalValue + updateValue);
      PatientColumnDto section5TotalDto = patientGroupDtoSection5.getColumns().get(TOTAL_COLUMN);
      if (NEW_SECTION_2.equals(sectionKey) && NEW_COLUMN_4.equals(k)) {
        patientGroupDtoSection5.getColumns().get(NEW_COLUMN).setValue(updateValue);
        section5TotalDto.setValue(section5TotalDto.getValue() + updateValue);
      } else if (NEW_SECTION_3.equals(sectionKey) && NEW_COLUMN_1.equals(k)) {
        patientGroupDtoSection5.getColumns().get(NEW_COLUMN_0).setValue(updateValue);
        section5TotalDto.setValue(section5TotalDto.getValue() + updateValue);
      } else if (NEW_SECTION_4.equals(sectionKey) && NEW_COLUMN.equals(k)) {
        patientGroupDtoSection5.getColumns().get(NEW_COLUMN_1).setValue(updateValue);
        section5TotalDto.setValue(section5TotalDto.getValue() + updateValue);
      }
    });
  }

  private void updateRegimenSummaryLineItems(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (CollectionUtils.isEmpty(request.getRegimenSummaryLineItems())) {
      return;
    }
    Map<String, RegimenSummaryLineDto> regimenNameToRegimenSummaryLineDto = requisitionDto.getRegimenSummaryLineItems()
        .stream()
        .collect(Collectors.toMap(RegimenSummaryLineDto::getName, Function.identity()));

    request.getRegimenSummaryLineItems().forEach(
        summaryRequest -> buildRegimenSummaryPatientsAndCommunity(
            regimenNameToRegimenSummaryLineDto.get(RegimenSummaryCode.findValueByKey(summaryRequest.getCode())),
            summaryRequest, regimenNameToRegimenSummaryLineDto.get(TOTAL_COLUMN))
    );
  }

  private void buildRegimenSummaryPatientsAndCommunity(RegimenSummaryLineDto summaryLineDto,
      RegimenLineItemRequest summaryRequest, RegimenSummaryLineDto totalDto) {
    if (summaryLineDto == null) {
      throw new NotFoundException("summaryLineDto not found");
    }
    Map<String, RegimenColumnDto> columns = summaryLineDto.getColumns();
    columns.get(COLUMN_NAME_PATIENT).setValue(summaryRequest.getPatientsOnTreatment());
    columns.get(COLUMN_NAME_COMMUNITY).setValue(summaryRequest.getComunitaryPharmacy());

    Map<String, RegimenColumnDto> totalColumns = totalDto.getColumns();
    int patientsTotal =
        totalColumns.get(COLUMN_NAME_PATIENT).getValue() == null ? 0 : totalColumns.get(COLUMN_NAME_PATIENT).getValue();
    int communityTotal =
        totalColumns.get(COLUMN_NAME_COMMUNITY).getValue() == null ? 0
            : totalColumns.get(COLUMN_NAME_COMMUNITY).getValue();
    totalColumns.get(COLUMN_NAME_PATIENT).setValue(patientsTotal + summaryRequest.getPatientsOnTreatment());
    totalColumns.get(COLUMN_NAME_COMMUNITY).setValue(communityTotal + summaryRequest.getComunitaryPharmacy());
  }

  private void updateRegimenLineItems(SiglusRequisitionDto requisitionDto, UUID programId,
      RequisitionCreateRequest request) {
    if (CollectionUtils.isEmpty(request.getRegimenLineItems())) {
      return;
    }
    List<RegimenDto> regimenDtosByProgramId = regimenRepository.findAllByProgramIdAndActiveTrue(programId).stream()
        .map(RegimenDto::from).collect(Collectors.toList());
    if (CollectionUtils.isEmpty(regimenDtosByProgramId)) {
      return;
    }
    Map<String, RegimenDto> regimenCodeToRegimenDto = regimenDtosByProgramId.stream()
        .collect(Collectors.toMap(RegimenDto::getCode, Function.identity()));
    Map<UUID, RegimenDto> regimenIdToRegimenDto = regimenDtosByProgramId.stream()
        .collect(Collectors.toMap(RegimenDto::getId, Function.identity()));
    List<RegimenLineItem> regimenLineItems = buildRegimenPatientsAndCommunity(request.getRegimenLineItems(),
        requisitionDto.getId(), regimenCodeToRegimenDto);
    requisitionDto.setRegimenLineItems(RegimenLineDto.from(regimenLineItems, regimenIdToRegimenDto));
  }

  private List<RegimenLineItem> buildRegimenPatientsAndCommunity(List<RegimenLineItemRequest> regimenLineItemRequests,
      UUID requisitionId, Map<String, RegimenDto> regimenCodeToRegimenDto) {
    List<RegimenLineItem> regimenLineItems = new ArrayList<>();
    regimenLineItemRequests.forEach(itemRequest -> {
      RegimenDto regimenDto = regimenCodeToRegimenDto.get(itemRequest.getCode());
      if (regimenDto == null) {
        throw new NotFoundException("regimenDto not found");
      }
      RegimenLineItem patientRegimenLineItem = RegimenLineItem.builder()
          .requisitionId(requisitionId)
          .regimenId(regimenDto.getId())
          .column(COLUMN_NAME_PATIENT)
          .value(itemRequest.getPatientsOnTreatment())
          .build();
      RegimenLineItem communityRegimenLineItem = RegimenLineItem.builder()
          .requisitionId(requisitionId)
          .regimenId(regimenDto.getId())
          .column(COLUMN_NAME_COMMUNITY)
          .value(itemRequest.getComunitaryPharmacy())
          .build();
      regimenLineItems.add(patientRegimenLineItem);
      regimenLineItems.add(communityRegimenLineItem);
    });
    return regimenLineItems;
  }

  private void buildConsultationNumber(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    requisitionDto.getConsultationNumberLineItems().forEach(consultationNumber -> {
      if (GROUP_NAME.equals(consultationNumber.getName())) {
        consultationNumber.getColumns().get(COLUMN_NAME).setValue(request.getConsultationNumber());
      }
    });
  }

  private void buildRequisitionKitUsage(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (CollectionUtils.isEmpty(request.getProducts())) {
      return;
    }
    int kitReceivedChw = request.getProducts().stream()
        .filter(product -> KIT_26A02.equals(product.getProductCode()))
        .mapToInt(RequisitionLineItemRequest::getTotalReceivedQuantity)
        .sum();
    int kitReceivedHf = request.getProducts().stream()
        .filter(product -> KIT_26A01.equals(product.getProductCode()))
        .mapToInt(RequisitionLineItemRequest::getTotalReceivedQuantity)
        .sum();
    int kitOpenedChw = request.getProducts().stream()
        .filter(product -> KIT_26A02.equals(product.getProductCode()))
        .mapToInt(RequisitionLineItemRequest::getTotalConsumedQuantity)
        .sum();
    int kitOpenedHf = request.getProducts().stream()
        .filter(product -> KIT_26A01.equals(product.getProductCode()))
        .mapToInt(RequisitionLineItemRequest::getTotalConsumedQuantity)
        .sum();
    requisitionDto.getKitUsageLineItems().forEach(kitUsage -> {
      if (COLLECTION_KIT_RECEIVED.equals(kitUsage.getCollection())) {
        kitUsage.getServices().get(SERVICE_CHW).setValue(kitReceivedChw);
        kitUsage.getServices().get(SERVICE_HF).setValue(kitReceivedHf);
      } else if (COLLECTION_KIT_OPENED.equals(kitUsage.getCollection())) {
        kitUsage.getServices().get(SERVICE_CHW).setValue(kitOpenedChw);
        kitUsage.getServices().get(SERVICE_HF).setValue(kitOpenedHf);
      }
    });
  }

  private void updateUsageInformationLineItems(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (CollectionUtils.isEmpty(request.getUsageInformationLineItems())) {
      return;
    }
    List<UsageInformationLineItemRequest> usageInformationLineItemRequests = request.getUsageInformationLineItems();
    List<UsageInformationLineItem> usageInformationLineItems = usageInformationLineItemRequests.stream()
        .map(t -> buildUsageInfos(requisitionDto.getId(), t))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    List<UsageInformationServiceDto> usageInformationServiceDtos = UsageInformationServiceDto
        .from(usageInformationLineItems);
    requisitionDto.setUsageInformationLineItems(usageInformationServiceDtos);
  }

  private List<UsageInformationLineItem> buildUsageInfos(UUID requisitionId,
      UsageInformationLineItemRequest usageInfoLineRequest) {
    UsageInformationLineItem hfUsageInfoLineItem = UsageInformationLineItem.builder()
        .information(usageInfoLineRequest.getInformation())
        .requisitionId(requisitionId)
        .orderableId(siglusOrderableService.getOrderableByCode(usageInfoLineRequest.getProductCode()).getId())
        .service(UsageInformationLineItems.SERVICE_HF)
        .value(usageInfoLineRequest.getHf())
        .build();
    UsageInformationLineItem chwUsageInfoLineItem = UsageInformationLineItem.builder()
        .information(usageInfoLineRequest.getInformation())
        .requisitionId(requisitionId)
        .orderableId(siglusOrderableService.getOrderableByCode(usageInfoLineRequest.getProductCode()).getId())
        .service(UsageInformationLineItems.SERVICE_CHW)
        .value(usageInfoLineRequest.getChw())
        .build();
    UsageInformationLineItem totalUsageInfoLineItem = UsageInformationLineItem.builder()
        .information(usageInfoLineRequest.getInformation())
        .requisitionId(requisitionId)
        .orderableId(siglusOrderableService.getOrderableByCode(usageInfoLineRequest.getProductCode()).getId())
        .service(UsageInformationLineItems.SERVICE_TOTAL)
        .value(Math.addExact(usageInfoLineRequest.getHf(), usageInfoLineRequest.getChw()))
        .build();
    return Arrays.asList(hfUsageInfoLineItem, chwUsageInfoLineItem, totalUsageInfoLineItem);
  }

  private void buildAvailableProducts(RequisitionV2Dto dto, Requisition requisition) {
    Set<VersionObjectReferenceDto> availableProducts = new HashSet<>();
    Optional
        .ofNullable(requisition.getAvailableProducts())
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .forEach(orderable -> {
          VersionObjectReferenceDto reference = new VersionObjectReferenceDto(
              orderable.getId(), "", ORDERABLES, orderable.getVersionNumber());
          availableProducts.add(reference);
        });
    dto.setAvailableProducts(availableProducts);
  }
}

