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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.openlmis.requisition.web.ResourceNames.FACILITIES;
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
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_4;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_5;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_6;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_7;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DM_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TOTAL_COLUMN;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_COMMUNITY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_PATIENT;
import static org.siglus.siglusapi.constant.UsageSectionConstants.TestConsumptionLineItems.SERVICE_APES;
import static org.springframework.util.CollectionUtils.isEmpty;

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
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.ProgramAdditionalOrderableDto;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.constant.ProgramConstants;
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
import org.siglus.siglusapi.dto.TestConsumptionOutcomeDto;
import org.siglus.siglusapi.dto.TestConsumptionProjectDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.enumeration.PatientLineItemName;
import org.siglus.siglusapi.dto.android.enumeration.PatientTableName;
import org.siglus.siglusapi.dto.android.enumeration.RegimenSummaryCode;
import org.siglus.siglusapi.dto.android.enumeration.TestOutcome;
import org.siglus.siglusapi.dto.android.enumeration.TestProject;
import org.siglus.siglusapi.dto.android.enumeration.TestService;
import org.siglus.siglusapi.dto.android.request.PatientLineItemColumnRequest;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.RegimenLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.dto.android.request.TestConsumptionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.UsageInformationLineItemRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.exception.InvalidProgramCodeException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.UnsupportedProductsException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramAdditionalOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.slf4j.profiler.Profiler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionCreateService {

  private final SiglusAuthenticationHelper authHelper;
  private final RequisitionService requisitionService;
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
  private final SupportedProgramsHelper supportedProgramsHelper;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final SiglusProgramAdditionalOrderableService additionalOrderableService;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final RequisitionTemplateRepository requisitionTemplateRepository;
  private final SiglusNotificationService siglusNotificationService;

  @Transactional
  @Validated(PerformanceSequence.class)
  public UUID createRequisition(@Valid RequisitionCreateRequest request) {
    UserDto user = authHelper.getCurrentUser();
    String syncUpHash = request.getSyncUpHash(user);
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip create requisition as syncUpHash: {} existed", syncUpHash);
      return null;
    }
    UUID authorId = user.getId();
    Requisition requisition = initiateRequisition(request, user);
    requisition = submitRequisition(requisition, authorId);
    requisition = authorizeRequisition(requisition, authorId);
    internalApproveRequisition(requisition, authorId);
    log.info("generate notification for requisition: {}", requisition.getId());
    siglusNotificationService.postApprove(buildBaseRequisitionDto(requisition));
    log.info("save requisition syncUpHash: {}", syncUpHash);
    SyncUpHash syncUpHashDomain = SyncUpHash.builder()
        .hash(syncUpHash)
        .type("Requisition")
        .referenceId(requisition.getId())
        .build();
    syncUpHashRepository.save(syncUpHashDomain);
    return requisition.getId();
  }

  private Requisition initiateRequisition(RequisitionCreateRequest request, UserDto user) {
    log.info("prepare android requisition: {}", request);
    Profiler profiler = new Profiler("initiateRequisition");
    profiler.setLogger(log);
    String programCode = request.getProgramCode();
    profiler.start("get program");
    UUID programId = siglusProgramService.getProgramByCode(programCode).map(org.openlmis.requisition.dto.BaseDto::getId)
        .orElseThrow(() -> InvalidProgramCodeException.requisition(programCode));
    profiler.start("get user facility");
    UUID homeFacilityId = user.getHomeFacilityId();
    profiler.start("check permission: init req");
    checkPermission(() -> permissionService.canInitRequisition(programId, homeFacilityId));
    profiler.start("check supported products");
    checkSupportedProducts(homeFacilityId, programId, request);
    Profiler newReqProfiler = profiler.startNested("new req");
    newReqProfiler.start("build req");
    Requisition newRequisition = RequisitionBuilder.newRequisition(homeFacilityId, programId, request.getEmergency());
    newReqProfiler.start("get template");
    newRequisition.setTemplate(getRequisitionTemplate(programId, homeFacilityId));
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newReqProfiler.start("get period");
    newRequisition.setProcessingPeriodId(getPeriodId(request));
    newRequisition.setNumberOfMonthsInPeriod(1);
    newRequisition.setDraftStatusMessage(request.getComments());
    newRequisition.setReportOnly(ProgramConstants.MALARIA_PROGRAM_CODE.equals(programCode));
    newReqProfiler.start("build status change for init");
    buildStatusChanges(newRequisition, user.getId());
    newReqProfiler.start("build approved product");
    buildRequisitionApprovedProduct(newRequisition, homeFacilityId, programId);
    newReqProfiler.start("build extra data");
    buildRequisitionExtraData(newRequisition, request);
    newReqProfiler.start("build line items");
    buildRequisitionLineItems(newRequisition, request);
    profiler.start("save req and flush");
    Requisition requisition = requisitionRepository.saveAndFlush(newRequisition);
    profiler.start("build req extension");
    buildRequisitionExtension(requisition, request, profiler.startNested("build req extension"));
    buildRequisitionLineItemsExtension(requisition, request, profiler.startNested("build line extensions"));
    buildRequisitionUsageSections(requisition, request, programId, profiler.startNested("build usage sections"));
    profiler.stop().log();
    return requisition;
  }

  private Requisition submitRequisition(Requisition requisition, UUID authorId) {
    Profiler profiler = new Profiler("submitRequisition");
    profiler.setLogger(log);
    profiler.start("check permission: submit req");
    checkPermission(() -> permissionService.canSubmitRequisition(requisition));
    requisition.setModifiedDate(ZonedDateTime.now());
    requisition.setStatus(RequisitionStatus.SUBMITTED);
    profiler.start("build status change for submit");
    buildStatusChanges(requisition, authorId);
    log.info("submit android requisition: {}", requisition);
    profiler.start("save and flush for submit");
    Requisition saved = requisitionRepository.saveAndFlush(requisition);
    profiler.stop().log();
    return saved;
  }

  private Requisition authorizeRequisition(Requisition requisition, UUID authorId) {
    Profiler profiler = new Profiler("authorizeRequisition");
    profiler.setLogger(log);
    profiler.start("check permission: authorize req");
    checkPermission(() -> permissionService.canAuthorizeRequisition(requisition));
    profiler.start("find supervisory node");
    UUID supervisoryNodeId = supervisoryNodeService.findSupervisoryNode(
        requisition.getProgramId(), requisition.getFacilityId()).getId();
    requisition.setSupervisoryNodeId(supervisoryNodeId);
    requisition.setModifiedDate(ZonedDateTime.now());
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    profiler.start("build status change for auth");
    buildStatusChanges(requisition, authorId);
    log.info("authorize android requisition: {}", requisition);
    profiler.start("save and flush for auth");
    Requisition saved = requisitionRepository.saveAndFlush(requisition);
    profiler.stop().log();
    return saved;
  }

  private void internalApproveRequisition(Requisition requisition, UUID authorId) {
    Profiler profiler = new Profiler("internalApproveRequisition");
    profiler.setLogger(log);
    profiler.start("check permission: int-approve req");
    checkPermission(() -> permissionService.canApproveRequisition(requisition));
    profiler.start("find supervisory node");
    SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeService.findOne(requisition.getSupervisoryNodeId());
    requisition.setSupervisoryNodeId(supervisoryNodeDto.getParentNodeId());
    requisition.setModifiedDate(ZonedDateTime.now());
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    profiler.start("build status change for int-approve");
    buildStatusChanges(requisition, authorId);
    log.info("internal-approve android requisition: {}", requisition);
    profiler.start("save and flush for int-approve");
    requisitionRepository.save(requisition);
    profiler.stop().log();
  }

  private void checkPermission(Supplier<ValidationResult> supplier) {
    supplier.get().throwExceptionIfHasErrors();
  }

  private void checkSupportedProducts(UUID facilityId, UUID programId, RequisitionCreateRequest request) {
    Map<UUID, Set<String>> programIdToProductCodes = supportedProgramsHelper.findHomeFacilitySupportedProgramIds()
        .stream()
        .collect(
            toMap(Function.identity(),
                supportProgramId -> approvedProductDataService.getApprovedProducts(facilityId, supportProgramId)
                    .stream().map(product -> product.getOrderable().getProductCode()).collect(toSet())
            )
        );
    Set<String> requisitionProductCodes = new HashSet<>();
    if (!isEmpty(request.getProducts())) {
      requisitionProductCodes = request.getProducts().stream().map(RequisitionLineItemRequest::getProductCode)
          .collect(toSet());
    } else if (!isEmpty(request.getUsageInformationLineItems())) {
      requisitionProductCodes = request.getUsageInformationLineItems().stream()
          .map(UsageInformationLineItemRequest::getProductCode)
          .collect(toSet());
    }
    Set<String> facilityUnsupportedProductCodes = getUnsupportedProductsByFacility(programIdToProductCodes,
        requisitionProductCodes);
    if (!isEmpty(facilityUnsupportedProductCodes)) {
      throw UnsupportedProductsException.asAndroidException(facilityUnsupportedProductCodes.toArray(new String[0]));
    }
    Set<String> programUnsupportedProductCodes = getUnsupportedProductsByProgram(programId,
        programIdToProductCodes, requisitionProductCodes);
    if (!isEmpty(programUnsupportedProductCodes)) {
      throw UnsupportedProductsException.asAndroidException(programUnsupportedProductCodes.toArray(new String[0]));
    }
  }

  private Set<String> getUnsupportedProductsByFacility(Map<UUID, Set<String>> programIdToProductCodes,
      Set<String> podProductCodes) {
    Set<String> approvedProductCodes = programIdToProductCodes.values().stream()
        .flatMap(Collection::stream)
        .collect(toSet());
    return podProductCodes.stream()
        .filter(podProductCode -> !approvedProductCodes.contains(podProductCode))
        .collect(toSet());
  }

  private Set<String> getUnsupportedProductsByProgram(UUID programId, Map<UUID, Set<String>> programIdToProductCodes,
      Set<String> podProductCodes) {
    Set<String> approvedProductCodes = programIdToProductCodes.get(programId);
    Set<String> filtered = podProductCodes.stream()
        .filter(code -> !approvedProductCodes.contains(code))
        .collect(toSet());

    Set<String> additionalProductCodes = additionalOrderableService.searchAdditionalOrderables(programId).stream()
        .map(ProgramAdditionalOrderableDto::getProductCode).map(Code::toString).collect(toSet());
    return filtered.stream()
        .filter(code -> !additionalProductCodes.contains(code))
        .collect(toSet());
  }

  private void buildStatusChanges(Requisition requisition, UUID authorId) {
    requisition.getStatusChanges().add(StatusChange.newStatusChange(requisition, authorId));
  }

  private UUID getPeriodId(RequisitionCreateRequest request) {
    YearMonth month = request.getActualStartDate().query(YearMonth::from);
    return processingPeriodRepository.findPeriodByCodeAndMonth(SCHEDULE_CODE, month)
        .map(ProcessingPeriod::getId)
        .orElseThrow(EntityNotFoundException::new);
  }

  private RequisitionTemplate getRequisitionTemplate(UUID programId, UUID facilityId) {
    UUID facilityTypeId = siglusFacilityReferenceDataService.findOne(facilityId).getType().getId();
    RequisitionTemplate template = requisitionTemplateRepository.findTemplate(programId, facilityTypeId);
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(template.getId());
    template.setTemplateExtension(templateExtension);
    return template;
  }

  private void buildRequisitionExtension(Requisition requisition, RequisitionCreateRequest request, Profiler profiler) {
    profiler.start("build extension");
    RequisitionExtension requisitionExtension = siglusRequisitionExtensionService
        .buildRequisitionExtension(requisition.getId(), requisition.getEmergency(), requisition.getFacilityId());
    requisitionExtension.setIsApprovedByInternal(true);
    requisitionExtension.setActualStartDate(request.getActualStartDate());
    profiler.start("save and flush");
    requisitionExtensionRepository.saveAndFlush(requisitionExtension);
  }

  private void buildRequisitionLineItemsExtension(Requisition requisition,
      RequisitionCreateRequest requisitionRequest, Profiler profiler) {
    if (isEmpty(requisition.getRequisitionLineItems())) {
      return;
    }
    profiler.start("load all products");
    log.info("requisition line size: {}", requisition.getRequisitionLineItems().size());
    log.info("requisition request product count: {}", requisitionRequest.getProducts().size());
    // since this api is cached, so load all data is even faster than for-each and load single
    Map<String, UUID> productCodeToIds =
        getAllProducts().stream().collect(toMap(OrderableDto::getProductCode, BaseDto::getId));
    Map<UUID, RequisitionLineItemRequest> productIdToLineItems = requisitionRequest.getProducts().stream()
        .collect(toMap(product -> productCodeToIds.get(product.getProductCode()), identity()));
    profiler.start("mapping line items");
    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
      extension.setRequisitionLineItemId(requisitionLineItem.getId());
      RequisitionLineItemRequest requisitionProduct = productIdToLineItems
          .getOrDefault(requisitionLineItem.getOrderable().getId(), new RequisitionLineItemRequest());
      extension.setAuthorizedQuantity(requisitionProduct.getAuthorizedQuantity());
      extension.setExpirationDate(requisitionProduct.getExpirationDate());
      requisitionLineItemExtensionRepository.save(extension);
    });
    profiler.start("flush");
    requisitionLineItemExtensionRepository.flush();
  }

  private List<OrderableDto> getAllProducts() {
    return siglusOrderableService.getAllProducts();
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
    if (isEmpty(requisitionRequest.getProducts())) {
      return;
    }
    Map<UUID, VersionEntityReference> productIdToApproveds = requisition.getAvailableProducts().stream().collect(
        toMap(product -> product.getOrderable().getId(), ApprovedProductReference::getFacilityTypeApprovedProduct)
    );
    // since this api is cached, so load all data is even faster than for-each and load single
    Map<String, OrderableDto> productCodeToOrderables =
        getAllProducts().stream().collect(toMap(OrderableDto::getProductCode, Function.identity()));
    List<RequisitionLineItem> requisitionLineItems = new ArrayList<>();
    for (RequisitionLineItemRequest product : requisitionRequest.getProducts()) {
      OrderableDto orderableDto = productCodeToOrderables.get(product.getProductCode());
      RequisitionLineItem lineItem = new RequisitionLineItem();
      lineItem.setRequisition(requisition);
      lineItem.setOrderable(new VersionEntityReference(orderableDto.getId(), orderableDto.getVersionNumber()));
      lineItem.setBeginningBalance(product.getBeginningBalance());
      lineItem.setTotalLossesAndAdjustments(product.getTotalLossesAndAdjustments());
      lineItem.setTotalReceivedQuantity(product.getTotalReceivedQuantity());
      lineItem.setTotalConsumedQuantity(product.getTotalConsumedQuantity());
      lineItem.setStockOnHand(product.getStockOnHand());
      lineItem.setRequestedQuantity(product.getRequestedQuantity());
      VersionEntityReference approvedProduct = productIdToApproveds.get(lineItem.getOrderable().getId());
      if (approvedProduct == null) {
        throw new IllegalStateException("requisition line item can't find approvedProduct by orderable id, orderable "
            + "id:" + lineItem.getOrderable().getId());
      }
      lineItem.setFacilityTypeApprovedProduct(approvedProduct);
      boolean isKit = ALL_KITS.contains(product.getProductCode());
      lineItem.setSkipped(isKit);
      requisitionLineItems.add(lineItem);
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
      UUID programId, Profiler profiler) {
    profiler.start("new instance");
    RequisitionV2Dto dto = new RequisitionV2Dto();
    requisition.export(dto);
    BasicRequisitionTemplateDto templateDto = BasicRequisitionTemplateDto.newInstance(requisition.getTemplate());
    templateDto.setExtension(RequisitionTemplateExtensionDto.from(requisition.getTemplate().getTemplateExtension()));
    dto.setTemplate(templateDto);
    dto.setFacility(new ObjectReferenceDto(requisition.getFacilityId(), "", FACILITIES));
    dto.setProcessingPeriod(new ObjectReferenceDto(requisition.getProcessingPeriodId(), "", PROCESSING_PERIODS));
    dto.setProgram(new ObjectReferenceDto(requisition.getProgramId(), "", PROGRAMS));
    buildAvailableProducts(dto, requisition);
    profiler.start("init usage report");
    SiglusRequisitionDto requisitionDto = siglusUsageReportService.initiateUsageReport(dto);
    profiler.start("buildConsultationNumber");
    buildConsultationNumber(requisitionDto, request);
    profiler.start("buildRequisitionKitUsage");
    buildRequisitionKitUsage(requisitionDto, request);
    profiler.start("updateRegimenLineItems");
    updateRegimenLineItems(requisitionDto, programId, request);
    profiler.start("updateRegimenSummaryLineItems");
    updateRegimenSummaryLineItems(requisitionDto, request);
    profiler.start("updatePatientLineItems");
    updatePatientLineItems(requisitionDto, request);
    profiler.start("updateUsageInformationLineItems");
    updateUsageInformationLineItems(requisitionDto, request);
    profiler.start("updateTestConsumptionLineItems");
    updateTestConsumptionLineItems(requisitionDto, request);
    profiler.start("save usage report");
    siglusUsageReportService.saveUsageReport(requisitionDto, dto);
  }

  private void updatePatientLineItems(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (isEmpty(request.getPatientLineItems())) {
      return;
    }
    Map<String, PatientGroupDto> patientNameToPatientGroupDto = requisitionDto.getPatientLineItems().stream()
        .collect(toMap(PatientGroupDto::getName, identity()));
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
      String patientGroupDtoKey = PatientTableName.valueOf(name.toUpperCase()).findValueByKey(k.getName());
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
    if (isEmpty(request.getRegimenSummaryLineItems())) {
      return;
    }
    Map<String, RegimenSummaryLineDto> regimenNameToRegimenSummaryLineDto = requisitionDto.getRegimenSummaryLineItems()
        .stream()
        .collect(toMap(RegimenSummaryLineDto::getName, identity()));

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

  private void updateTestConsumptionLineItems(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (isEmpty(request.getTestConsumptionLineItems())) {
      return;
    }
    Map<String, Map<String, TestConsumptionProjectDto>> serviceToTestProjectDto = requisitionDto
        .getTestConsumptionLineItems().stream()
        .collect(toMap(TestConsumptionServiceDto::getService, TestConsumptionServiceDto::getProjects));
    request.getTestConsumptionLineItems().forEach(item -> buildTestOutcomeValue(item, serviceToTestProjectDto));
  }

  private void buildTestOutcomeValue(TestConsumptionLineItemRequest testRequest,
      Map<String, Map<String, TestConsumptionProjectDto>> serviceToTestProjectDto) {
    if (!SERVICE_APES.equals(testRequest.getService())) {
      TestConsumptionOutcomeDto totalOutcomeDto = serviceToTestProjectDto.get(UsageInformationLineItems.SERVICE_TOTAL)
          .get(TestProject.valueOf(testRequest.getTestProject()).getValue())
          .getOutcomes()
          .get(TestOutcome.valueOf(testRequest.getTestOutcome()).getValue());
      int totalValue = totalOutcomeDto.getValue() == null ? 0 : totalOutcomeDto.getValue();
      totalOutcomeDto.setValue(totalValue + testRequest.getValue());
    }
    serviceToTestProjectDto.get(TestService.valueOf(testRequest.getService()).getValue())
        .get(TestProject.valueOf(testRequest.getTestProject()).getValue())
        .getOutcomes()
        .get(TestOutcome.valueOf(testRequest.getTestOutcome()).getValue())
        .setValue(testRequest.getValue());
  }

  private void updateRegimenLineItems(SiglusRequisitionDto requisitionDto, UUID programId,
      RequisitionCreateRequest request) {
    if (isEmpty(request.getRegimenLineItems())) {
      return;
    }
    List<RegimenDto> regimenDtosByProgramId = regimenRepository.findAllByProgramIdAndActiveTrue(programId).stream()
        .map(RegimenDto::from).collect(Collectors.toList());
    if (isEmpty(regimenDtosByProgramId)) {
      return;
    }
    Map<String, RegimenDto> regimenCodeToRegimenDto = regimenDtosByProgramId.stream()
        .collect(toMap(RegimenDto::getCode, identity()));
    Map<UUID, RegimenDto> regimenIdToRegimenDto = regimenDtosByProgramId.stream()
        .collect(toMap(RegimenDto::getId, identity()));
    List<RegimenLineItem> regimenLineItems = buildRegimenPatientsAndCommunity(request.getRegimenLineItems(),
        regimenCodeToRegimenDto);
    requisitionDto.setRegimenLineItems(RegimenLineDto.from(regimenLineItems, regimenIdToRegimenDto));
  }

  private List<RegimenLineItem> buildRegimenPatientsAndCommunity(List<RegimenLineItemRequest> regimenLineItemRequests,
      Map<String, RegimenDto> regimenCodeToRegimenDto) {
    Map<String, Integer> totalMap = new HashMap<>();
    totalMap.put(COLUMN_NAME_PATIENT, 0);
    totalMap.put(COLUMN_NAME_COMMUNITY, 0);
    List<RegimenLineItem> regimenLineItems = new ArrayList<>();
    regimenLineItemRequests.forEach(itemRequest -> {
      RegimenDto regimenDto = regimenCodeToRegimenDto.get(itemRequest.getCode());
      if (regimenDto == null) {
        throw new NotFoundException("regimenDto not found,error code is :" + itemRequest.getCode());
      }
      int patientTotal = totalMap.get(COLUMN_NAME_PATIENT);
      RegimenLineItem patientRegimenLineItem = RegimenLineItem.builder()
          .regimenId(regimenDto.getId())
          .column(COLUMN_NAME_PATIENT)
          .value(itemRequest.getPatientsOnTreatment())
          .build();
      totalMap.put(COLUMN_NAME_PATIENT, patientTotal + itemRequest.getPatientsOnTreatment());
      int communityTotal = totalMap.get(COLUMN_NAME_COMMUNITY);
      RegimenLineItem communityRegimenLineItem = RegimenLineItem.builder()
          .regimenId(regimenDto.getId())
          .column(COLUMN_NAME_COMMUNITY)
          .value(itemRequest.getComunitaryPharmacy())
          .build();
      totalMap.put(COLUMN_NAME_COMMUNITY, communityTotal + itemRequest.getComunitaryPharmacy());
      regimenLineItems.add(patientRegimenLineItem);
      regimenLineItems.add(communityRegimenLineItem);
    });
    buildRegimenLineItemTotalValue(regimenLineItems, totalMap);
    return regimenLineItems;
  }

  private void buildRegimenLineItemTotalValue(List<RegimenLineItem> regimenLineItems, Map<String, Integer> totalMap) {
    RegimenLineItem totalPatientRegimenLineItem = RegimenLineItem.builder()
        .column(COLUMN_NAME_PATIENT)
        .value(totalMap.get(COLUMN_NAME_PATIENT))
        .build();
    RegimenLineItem totalCommunityRegimenLineItem = RegimenLineItem.builder()
        .column(COLUMN_NAME_COMMUNITY)
        .value(totalMap.get(COLUMN_NAME_COMMUNITY))
        .build();
    regimenLineItems.add(totalPatientRegimenLineItem);
    regimenLineItems.add(totalCommunityRegimenLineItem);
  }

  private void buildConsultationNumber(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    requisitionDto.getConsultationNumberLineItems().forEach(consultationNumber -> {
      if (GROUP_NAME.equals(consultationNumber.getName())) {
        consultationNumber.getColumns().get(COLUMN_NAME).setValue(request.getConsultationNumber());
      }
    });
  }

  private void buildRequisitionKitUsage(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (isEmpty(request.getProducts())) {
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
    if (isEmpty(request.getUsageInformationLineItems())) {
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

  private BasicRequisitionDto buildBaseRequisitionDto(Requisition requisition) {
    BasicRequisitionDto basicRequisitionDto = new BasicRequisitionDto();
    basicRequisitionDto.setId(requisition.getId());
    basicRequisitionDto.setStatus(requisition.getStatus());
    MinimalFacilityDto facility = new MinimalFacilityDto();
    facility.setId(requisition.getFacilityId());
    basicRequisitionDto.setFacility(facility);
    BasicProgramDto program = new BasicProgramDto();
    program.setId(requisition.getProgramId());
    basicRequisitionDto.setProgram(program);
    basicRequisitionDto.setEmergency(requisition.getEmergency());
    BasicProcessingPeriodDto processingPeriod = new BasicProcessingPeriodDto();
    processingPeriod.setId(requisition.getProcessingPeriodId());
    basicRequisitionDto.setProcessingPeriod(processingPeriod);
    return basicRequisitionDto;
  }
}

