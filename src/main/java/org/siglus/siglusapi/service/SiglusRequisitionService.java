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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.IN_APPROVAL;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED_WITHOUT_ORDER;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_ID_MISMATCH;
import static org.openlmis.requisition.web.QueryRequisitionSearchParams.REQUISITION_STATUS;
import static org.siglus.common.constant.KitConstants.APE_KITS;
import static org.siglus.common.constant.KitConstants.US_KITS;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.getTopLevelTypes;
import static org.siglus.siglusapi.constant.FieldConstants.REQUISITION_DRAFT_UUID;
import static org.siglus.siglusapi.constant.ProgramConstants.MTB_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.RAPIDTEST_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.TARV_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.VIA_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_2;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_4;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_9;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.TOTAL_COLUMN;
import static org.siglus.siglusapi.util.RequisitionUtil.getRequisitionExtraData;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem.Importer;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BaseRequisitionDto;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateColumnDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupportedProgramDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.RequisitionGroupReferenceDataService;
import org.openlmis.requisition.service.referencedata.RightReferenceDataService;
import org.openlmis.requisition.service.referencedata.RoleReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.web.QueryRequisitionSearchParams;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.requisition.web.RequisitionV2Controller;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.repository.StockManagementRepository;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.ConsultationNumberLineItemDraft;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.domain.KitUsageLineItemDraft;
import org.siglus.siglusapi.domain.PatientLineItemDraft;
import org.siglus.siglusapi.domain.RegimenLineItemDraft;
import org.siglus.siglusapi.domain.RegimenOrderable;
import org.siglus.siglusapi.domain.RegimenSummaryLineItemDraft;
import org.siglus.siglusapi.domain.RequisitionDraft;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemDraft;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.domain.TestConsumptionLineItemDraft;
import org.siglus.siglusapi.domain.UsageInformationLineItemDraft;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.dto.SimpleRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionOutcomeDto;
import org.siglus.siglusapi.dto.TestConsumptionProjectDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.dto.android.db.StockStatusCmm;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.RequisitionBuildDraftException;
import org.siglus.siglusapi.localmachine.event.requisition.web.finalapprove.RequisitionFinalApproveEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.release.RequisitionReleaseEmitter;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.NotSubmittedMonthlyRequisitionsRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenOrderableRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionDraftRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionNativeSqlRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.fc.FcCmmCpService;
import org.siglus.siglusapi.util.HashEncoder;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals", "PMD.CyclomaticComplexity"})
public class SiglusRequisitionService {

  @Autowired
  private RequisitionV2Controller requisitionV2Controller;
  @Autowired
  private RequisitionController requisitionController;
  @Autowired
  private RequisitionService requisitionService;
  @Autowired
  private PermissionService permissionService;
  @Autowired
  private SiglusOrderableService siglusOrderableService;
  @Autowired
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;
  @Autowired
  private SiglusArchiveProductService archiveProductService;
  @Autowired
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;
  @Autowired
  private SiglusRequisitionTemplateService siglusRequisitionTemplateService;
  @Autowired
  private RequisitionLineItemExtensionRepository lineItemExtensionRepository;
  @Autowired
  private FacilityTypeApprovedProductReferenceDataService facilityTypeApprovedProductReferenceDataService;
  @Autowired
  private RequisitionRepository requisitionRepository;
  @Autowired
  private AuthenticationHelper authenticationHelper;
  @Autowired
  private SiglusUsageReportService siglusUsageReportService;
  @Autowired
  private RequisitionDraftRepository draftRepository;
  @Autowired
  private OperatePermissionService operatePermissionService;
  @Autowired
  private RightReferenceDataService rightReferenceDataService;
  @Autowired
  private RoleReferenceDataService roleReferenceDataService;
  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;
  @Autowired
  private RequisitionGroupReferenceDataService requisitionGroupReferenceDataService;
  @Autowired
  private SiglusNotificationService notificationService;
  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;
  @Autowired
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  @Autowired
  private RegimenDataProcessor regimenDataProcessor;
  @Autowired
  private RequisitionExtensionRepository requisitionExtensionRepository;
  @Autowired
  private FacilityExtensionRepository facilityExtensionRepository;
  @Autowired
  private FcCmmCpService fcCmmCpService;
  @Autowired
  private SiglusFilterAddProductForEmergencyService filterProductService;
  @Autowired
  private SupportedProgramsHelper supportedProgramsHelper;
  @Autowired
  private NotSubmittedMonthlyRequisitionsRepository notSubmittedMonthlyRequisitionsRepository;
  @Autowired
  private HttpServletResponse response;
  @Autowired
  private StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;
  @Autowired
  private StockManagementRepository stockManagementRepository;
  @Autowired
  private SiglusGeneratedNumberService siglusGeneratedNumberService;
  @Autowired
  private ProcessingPeriodRepository processingPeriodRepository;
  @Autowired
  private RequisitionNativeSqlRepository requisitionNativeSqlRepository;
  @Autowired
  private FacilityCmmsRepository facilityCmmsRepository;
  @Autowired
  private RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  @Autowired
  private AgeGroupLineItemRepository ageGroupLineItemRepository;
  @Autowired
  private ConsultationNumberLineItemRepository consultationNumberLineItemRepository;
  @Autowired
  private UsageInformationLineItemRepository usageInformationLineItemRepository;
  @Autowired
  private PatientLineItemRepository patientLineItemRepository;
  @Autowired
  private TestConsumptionLineItemRepository testConsumptionLineItemRepository;
  @Autowired
  private RegimenLineItemRepository regimenLineItemRepository;
  @Autowired
  private RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;
  @Autowired
  private KitUsageLineItemRepository kitUsageRepository;
  public static final double DEFAULT_CORRECTION_FACTOR = 1.0;
  public static final int ESTIMATED_QUANTITY_FACTOR = 3;
  public static final int REQUESTED_QUANTITY_FACTOR = 1;
  public static final String MMIA_COLUMN_NAME = "patients";
  @Value("${service.url}")
  private String serviceUrl;
  public static final String SUGGESTED_QUANTITY_COLUMN = "suggestedQuantity";
  protected static final List<String> MMIA_SECTIONS = Lists.newArrayList(NEW_SECTION_2, NEW_SECTION_3, NEW_SECTION_4);
  protected static final List<String> CALC_PROGRAM_CODE = Lists.newArrayList(MTB_PROGRAM_CODE, RAPIDTEST_PROGRAM_CODE,
      TARV_PROGRAM_CODE);
  public static final String MMIA_COLUMN_TOTAL = "total";
  public static final String ESTIMATED_QUANTITY = "estimatedQuantity";
  public static final String REQUESTED_QUANTITY = "requestedQuantity";
  public static final String MMIT_TEMPLATE_HC = "MMIT Template - HC";
  public static final String MMIT_TEMPLATE_CS_PS_HOSPITAL = "MMIT Template - CS/PS/Hospital";
  @Autowired
  private OrderableRepository orderableRepository;
  @Autowired
  private ProgramRepository programRepository;
  @Autowired
  private RegimenOrderableRepository regimenOrderableRepository;
  @Autowired
  private RequisitionLineItemRepository requisitionLineItemRepository;
  @Autowired
  private RequisitionReleaseEmitter requisitionReleaseEmitter;
  @Autowired
  private BatchReleaseRequisitionService batchReleaseRequisitionService;
  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;
  @Autowired
  private RequisitionFinalApproveEmitter requisitionFinalApproveEmitter;
  @Autowired
  private SiglusAuthenticationHelper authHelper;
  @Autowired
  private SiglusProgramService siglusProgramService;
  @Autowired
  private StockCardRepository stockCardRepository;
  @Autowired
  private SyncUpHashRepository syncUpHashRepository;
  @Autowired
  private OrderExternalRepository orderExternalRepository;
  @Autowired
  private SiglusOrdersRepository siglusOrdersRepository;

  private final Map<String, BiConsumer<SiglusRequisitionDto, Set<RequisitionLineItem>>> programToRequestedQuantity =
      ImmutableMap.<String, BiConsumer<SiglusRequisitionDto, Set<RequisitionLineItem>>>builder()
          .put(MTB_PROGRAM_CODE, this::calcRequestedQuantityForMmtb)
          .put(RAPIDTEST_PROGRAM_CODE, this::calcRequestedQuantityForMmit)
          .put(TARV_PROGRAM_CODE, this::calcRequestedQuantityForMmia)
          .build();
  private final Map<String, BiConsumer<SiglusRequisitionDto, RequisitionDraft>> programToRequestedQuantityDraft =
      ImmutableMap.<String, BiConsumer<SiglusRequisitionDto, RequisitionDraft>>builder()
          .put(MTB_PROGRAM_CODE, this::calcRequestedQuantityDraftForMmtb)
          .put(RAPIDTEST_PROGRAM_CODE, this::calcRequestedQuantityDraftForMmit)
          .put(TARV_PROGRAM_CODE, this::calcRequestedQuantityDraftForMmia)
          .build();
  private final Map<String, BiConsumer<SiglusRequisitionDto, RequisitionDraft>> programToEstimatedQuantityDraft =
      ImmutableMap.<String, BiConsumer<SiglusRequisitionDto, RequisitionDraft>>builder()
          .put(MTB_PROGRAM_CODE, this::calcEstimatedQuantityDraftForMmtb)
          .put(RAPIDTEST_PROGRAM_CODE, this::calcEstimatedQuantityDraftForMmit)
          .put(TARV_PROGRAM_CODE, this::calcEstimatedQuantityDraftForMmia)
          .build();
  private final Map<String, BiConsumer<SiglusRequisitionDto, List<RequisitionLineItemExtension>>>
      programToEstimatedQuantity =
      ImmutableMap.<String, BiConsumer<SiglusRequisitionDto, List<RequisitionLineItemExtension>>>builder()
          .put(MTB_PROGRAM_CODE, this::calcEstimatedQuantityForMmtb)
          .put(RAPIDTEST_PROGRAM_CODE, this::calcEstimatedQuantityForMmit)
          .put(TARV_PROGRAM_CODE, this::calcEstimatedQuantityForMmia)
          .build();


  @Transactional
  public void approveAndReleaseWithoutOrder(Requisition requisition,
      Table<UUID, UUID, UUID> programSupervisoryNodeFacilities) {
    log.info("auto close requisition id: {}", requisition.getId());
    approveRequisition(requisition.getId(), null, null);

    org.siglus.siglusapi.dto.UserDto author = authHelper.getCurrentUser();

    if (RequisitionStatus.APPROVED.equals(requisition.getStatus())) {
      requisitionFinalApproveEmitter.emit(requisition.getId());
      ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
      releasableRequisitionDto.setRequisitionId(requisition.getId());
      releasableRequisitionDto.setSupplyingDepotId(programSupervisoryNodeFacilities
          .get(requisition.getProgramId(), requisition.getSupervisoryNodeId()));

      ReleasableRequisitionBatchDto releasableRequisitionBatchDto = new ReleasableRequisitionBatchDto();
      releasableRequisitionBatchDto.setCreateOrder(false);
      releasableRequisitionBatchDto.setRequisitionsToRelease(Arrays.asList(releasableRequisitionDto));
      // release without order
      batchReleaseRequisitionService
          .getRequisitionsProcessingStatusDtoResponse(releasableRequisitionBatchDto);
      requisitionReleaseEmitter.emit(releasableRequisitionDto, author == null ? null : author.getId());
    } else if (RequisitionStatus.RELEASED_WITHOUT_ORDER.equals(requisition.getStatus())) {
      ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
      releasableRequisitionDto.setRequisitionId(requisition.getId());
      releasableRequisitionDto.setSupplyingDepotId(programSupervisoryNodeFacilities
          .get(requisition.getProgramId(), requisition.getSupervisoryNodeId()));
      requisitionReleaseEmitter.emit(releasableRequisitionDto, author == null ? null : author.getId());
    }
  }

  @Transactional
  public void releaseWithoutOrder(RequisitionWithSupplyingDepotsDto requisitionDto) {
    ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
    releasableRequisitionDto.setRequisitionId(requisitionDto.getRequisition().getId());
    List<FacilityDto> supplyingDepots = requisitionDto.getSupplyingDepots();
    if (CollectionUtils.isNotEmpty(supplyingDepots)) {
      releasableRequisitionDto.setSupplyingDepotId(supplyingDepots.get(0).getId());
    }
    log.info("auto close requisition id: {}", releasableRequisitionDto.getRequisitionId());
    ReleasableRequisitionBatchDto releasableRequisitionBatchDto = new ReleasableRequisitionBatchDto();
    releasableRequisitionBatchDto.setCreateOrder(false);
    releasableRequisitionBatchDto.setRequisitionsToRelease(Arrays.asList(releasableRequisitionDto));

    org.siglus.siglusapi.dto.UserDto author = authHelper.getCurrentUser();
    // release without order
    batchReleaseRequisitionService
        .getRequisitionsProcessingStatusDtoResponse(releasableRequisitionBatchDto);
    requisitionReleaseEmitter.emit(releasableRequisitionDto, author == null ? null : author.getId());
  }

  @Transactional
  public SiglusRequisitionDto updateRequisition(UUID requisitionId, SiglusRequisitionDto requisitionDto,
      HttpServletRequest request, HttpServletResponse response) {
    if (null != requisitionDto.getId() && !Objects.equals(requisitionDto.getId(), requisitionId)) {
      throw new ValidationMessageException(ERROR_ID_MISMATCH);
    }
    if (operatePermissionService.canSubmit(requisitionDto)) {
      return saveRequisitionWithoutValidation(requisitionId, requisitionDto, request, response);
    } else {
      return saveRequisitionDraft(requisitionDto);
    }
  }

  @Transactional
  public SiglusRequisitionDto initiate(UUID programId, UUID facilityId, UUID suggestedPeriod, boolean emergency,
      String physicalInventoryDateStr, HttpServletRequest request, HttpServletResponse response) {
    RequisitionV2Dto v2Dto = requisitionV2Controller.initiate(programId, facilityId, suggestedPeriod, emergency,
        physicalInventoryDateStr, request, response);
    processStockMovementAfterTheDateSubmitted(v2Dto);
    SiglusRequisitionDto siglusRequisitionDto = siglusUsageReportService.initiateUsageReport(v2Dto);

    RequisitionTemplate template = requisitionRepository.findOne(siglusRequisitionDto.getId()).getTemplate();
    if (template.isColumnDisplayed(REQUESTED_QUANTITY) && template.isColumnStockBased(REQUESTED_QUANTITY)
        && siglusRequisitionDto.getStatus().isSubmittable()) {
      calcRequestedQuantity(siglusRequisitionDto);
    }

    initiateRequisitionNumber(siglusRequisitionDto);
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    initiateExpirationDate(lineItems, facilityId);
    initiateSuggestedQuantity(lineItems, facilityId, siglusRequisitionDto.getProcessingPeriodId(),
        siglusRequisitionDto.getProgramId(), siglusRequisitionDto.getTemplate());
    // reset data for (MMIT Template - CS/PS/Hospital and MMIT Template - HC)
    if (MMIT_TEMPLATE_HC.equals(template.getName()) || MMIT_TEMPLATE_CS_PS_HOSPITAL.equals(template.getName())) {
      requisitionLineItemRepository.resetTotalLossesAndAdjustmentsByRequisitionId(v2Dto.getId());
    }
    saveLineItemExtensions(lineItems);
    notSubmittedMonthlyRequisitionsRepository.deleteByFacilityIdAndProgramIdAndProcessingPeriodId(facilityId,
        programId, v2Dto.getProcessingPeriodId());
    return siglusRequisitionDto;
  }

  @Transactional
  public SiglusRequisitionDto initiateForClient(
      UUID programId,
      UUID facilityId,
      UUID suggestedPeriod,
      boolean emergency,
      String physicalInventoryDateStr,
      HttpServletRequest request,
      HttpServletResponse response) {
    RequisitionV2Dto v2Dto = requisitionV2Controller.initiate(programId, facilityId, suggestedPeriod, emergency,
        physicalInventoryDateStr, request, response);
    processStockMovementAfterTheDateSubmitted(v2Dto);
    SiglusRequisitionDto siglusRequisitionDto = siglusUsageReportService.initiateUsageReportForClient(v2Dto);

    RequisitionTemplate template = requisitionRepository.findOne(siglusRequisitionDto.getId()).getTemplate();
    if (template.isColumnDisplayed(REQUESTED_QUANTITY) && template.isColumnStockBased(REQUESTED_QUANTITY)
        && siglusRequisitionDto.getStatus().isSubmittable()) {
      calcRequestedQuantity(siglusRequisitionDto);
    }

    initiateRequisitionNumber(siglusRequisitionDto);
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    initiateExpirationDate(lineItems, facilityId);
    initiateSuggestedQuantity(lineItems, facilityId, siglusRequisitionDto.getProcessingPeriodId(),
        siglusRequisitionDto.getProgramId(), siglusRequisitionDto.getTemplate());
    saveLineItemExtensions(lineItems);
    notSubmittedMonthlyRequisitionsRepository.deleteByFacilityIdAndProgramIdAndProcessingPeriodId(facilityId,
        programId, v2Dto.getProcessingPeriodId());
    return siglusRequisitionDto;
  }

  @Transactional
  public void buildDraftForRegular(UUID facilityId, UUID periodId, UUID programId,
      HttpServletRequest request, HttpServletResponse response) {
    RequisitionV2Dto v2Dto = requisitionV2Controller.initiate(programId, facilityId, periodId, false,
        null, request, response);
    SiglusRequisitionDto siglusRequisitionDto = siglusUsageReportService.initiateUsageReport(v2Dto);

    RequisitionTemplate template = requisitionRepository.findOne(siglusRequisitionDto.getId()).getTemplate();
    if (template.isColumnDisplayed(REQUESTED_QUANTITY) && template.isColumnStockBased(REQUESTED_QUANTITY)
        && siglusRequisitionDto.getStatus().isSubmittable()) {
      calcRequestedQuantity(siglusRequisitionDto);
    }
    initiateRequisitionNumber(siglusRequisitionDto);
    // set default datas
    siglusRequisitionDto.setId(REQUISITION_DRAFT_UUID);
    siglusRequisitionDto.setRequisitionLineItems(newArrayList());
    siglusRequisitionDto.getExtraData().put("signaure", "");
    throw new RequisitionBuildDraftException(siglusRequisitionDto);
  }

  public void processStockMovementAfterTheDateSubmitted(RequisitionV2Dto v2Dto) {
    if (Boolean.TRUE.equals(v2Dto.getEmergency())) {
      return;
    }
    Requisition mock = new Requisition();
    if (v2Dto.getProcessingPeriod() == null) {
      return;
    }
    mock.setProcessingPeriodId(v2Dto.getProcessingPeriod().getId());
    mock.setFacilityId(v2Dto.getFacilityId());
    mock.setProgramId(v2Dto.getProgramId());
    Requisition previousRegularRequisition;

    previousRegularRequisition = requisitionService.getPreviousRegularRequisition(mock);

    if (previousRegularRequisition == null) {
      return;
    }

    // TODO when problem happens, the inventory date & requisition create date must be the same date
    // if inventory date before the create date, then the end date is inventory date,
    // then all movement at that date will be included,  so no problem here!!!
    LocalDate previousSubmitDate = previousRegularRequisition.getActualEndDate();
    ZonedDateTime previousCreateTime = previousRegularRequisition.getCreatedDate();

    Set<UUID> orderableIds = v2Dto.getLineItems().stream().map(lineItem ->
        lineItem.getOrderableIdentity().getId()).collect(toSet());
    if (CollectionUtils.isEmpty(orderableIds)) {
      return;
    }
    List<StockCard> stockCards = stockCardRepository.findByOrderableIdInAndFacilityId(orderableIds,
        v2Dto.getFacilityId());

    Map<UUID, List<StockCardLineItem>> orderableIdToLineItems = new HashMap<>();
    stockCards.forEach(stockCard -> {
      UUID orderableId = stockCard.getOrderableId();
      List<StockCardLineItem> items = stockCard.getLineItems().stream().filter(item ->
          previousSubmitDate.equals(item.getOccurredDate())
              && previousCreateTime.isBefore(item.getProcessedDate())).collect(toList());
      if (!items.isEmpty()) {
        if (orderableIdToLineItems.containsKey(stockCard.getOrderableId())) {
          orderableIdToLineItems.get(orderableId).addAll(items);
        } else {
          orderableIdToLineItems.put(orderableId, items);
        }
      }
    });

    Map<UUID, Integer> orderableIdToAdditonalIssue = new HashMap<>();
    Map<UUID, Integer> orderableIdToAdditonalReceive = new HashMap<>();
    orderableIdToLineItems.forEach((orderableId, lineItems) -> {
      Integer totalIssue = lineItems.stream()
          .filter(lineItem -> !lineItem.isPhysicalInventory())
          .filter(lineItem -> !lineItem.isPositive())
          .map(StockCardLineItem::getQuantity)
          .reduce(Integer::sum).orElse(0);

      Integer totalReceive = lineItems.stream()
          .filter(lineItem -> !lineItem.isPhysicalInventory())
          .filter(lineItem -> lineItem.isPositive())
          .map(StockCardLineItem::getQuantity)
          .reduce(Integer::sum).orElse(0);

      orderableIdToAdditonalIssue.put(orderableId, totalIssue);
      orderableIdToAdditonalReceive.put(orderableId, totalReceive);
    });

    Set<UUID> requisitionLinItemIds = v2Dto.getLineItems().stream()
        .filter(lineItem -> orderableIdToLineItems.containsKey(lineItem.getOrderableIdentity().getId()))
        .map(BaseDto::getId).collect(toSet());
    if (CollectionUtils.isEmpty(requisitionLinItemIds)) {
      return;
    }
    Set<RequisitionLineItem> requisitionLineItems = requisitionLineItemRepository.findAllById(requisitionLinItemIds);
    requisitionLineItems.forEach(lineItem -> {
      UUID orderableId = lineItem.getOrderable().getId();
      // TODO what about the initial inventory should change?
      //  Since the SOH is by date in calculated SOH, so we need to add back???
      int totalTotalReceivedQuantity = lineItem.getTotalReceivedQuantity() == null
          ? 0 : lineItem.getTotalReceivedQuantity();
      lineItem.setTotalReceivedQuantity(totalTotalReceivedQuantity
          + orderableIdToAdditonalReceive.getOrDefault(orderableId, 0));
      int totalConsumedQuantity = lineItem.getTotalConsumedQuantity() == null
          ? 0 : lineItem.getTotalConsumedQuantity();
      lineItem.setTotalConsumedQuantity(totalConsumedQuantity
          + orderableIdToAdditonalIssue.getOrDefault(orderableId, 0));
    });
    requisitionLineItemRepository.save(requisitionLineItems);
  }


  public void calcRequestedQuantity(SiglusRequisitionDto siglusRequisitionDto) {
    Set<UUID> lineItemIds = siglusRequisitionDto.getLineItems().stream()
        .map(BaseDto::getId)
        .filter(Objects::nonNull)
        .collect(toSet());
    Set<RequisitionLineItem> requisitionLineItems = new HashSet<>();
    if (CollectionUtils.isNotEmpty(lineItemIds)) {
      requisitionLineItems = requisitionLineItemRepository.findAllById(lineItemIds);
    }

    String programCode = programRepository.findOne(siglusRequisitionDto.getProgramId()).getCode().toString();
    if (CALC_PROGRAM_CODE.contains(programCode)) {
      programToRequestedQuantity.get(programCode).accept(siglusRequisitionDto, requisitionLineItems);
    }

    log.info("saved requisitionLineItems {}", requisitionLineItems);
    requisitionLineItemRepository.save(requisitionLineItems);
  }

  public void calcRequestedQuantityForMmia(SiglusRequisitionDto siglusRequisitionDto,
      Set<RequisitionLineItem> requisitionLineItems) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    Map<UUID, RequisitionLineItem> idToRequisitionLineItems = requisitionLineItems.stream()
        .collect(toMap(BaseEntity::getId, Function.identity()));

    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumberForMmia(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(regimenCodeToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByRegimenCodeIn(regimenCodeToPatientNumber.keySet());
    }
    double correctionFactorForMmia = getCorrectionFactorForMmia(siglusRequisitionDto.getPatientLineItems());
    log.info("calcRequestedQuantityForMmia: {}", correctionFactorForMmia);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItems.forEach(lineItem -> {
      if (lineItem.getId() != null) {
        Integer requestedQuantity = getQuantityByLineItem(lineItem,
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            correctionFactorForMmia,
            REQUESTED_QUANTITY_FACTOR);
        idToRequisitionLineItems.get(lineItem.getId()).setRequestedQuantity(requestedQuantity);
      } else {
        requisitionLineItems.add(lineItemDtoToEntity(lineItem));
      }
    });
  }

  public void calcEstimatedQuantityToRequest(RequisitionV2Dto requisitionDto) {
    UUID viaProgramId = siglusProgramService.getProgramByCode(VIA_PROGRAM_CODE)
        .orElseThrow(() -> new NotFoundException("VIA program not found"))
        .getId();
    if (viaProgramId.equals(requisitionDto.getProgramId())) {
      FacilityDto facilityDto = facilityReferenceDataService.findOne(requisitionDto.getFacilityId());
      if ((facilityDto != null) && getTopLevelTypes().contains(facilityDto.getType().getCode())) {
        List<StockStatusCmm> stockStatusCmms = facilityCmmsRepository
            .findStockStatusCmmByFacilityCode(facilityDto.getCode());
        if (CollectionUtils.isNotEmpty(stockStatusCmms)) {
          Map<String, Double> productCodeToCmm = stockStatusCmms.stream().collect(
              toMap(StockStatusCmm::getProductCode, StockStatusCmm::getCmm));
          Map<UUID, String> orderableIdToCode = siglusOrderableService.getAllProductIdToCode();
          requisitionDto.getRequisitionLineItems().forEach(lineItem -> {
            RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
            Double cmm = productCodeToCmm
                .getOrDefault(orderableIdToCode.get(lineItemV2Dto.getOrderable().getId()), 0.0);
            Integer estimatedQuantityToRequest = (int) (cmm * 5) - lineItem.getStockOnHand();
            if (estimatedQuantityToRequest > 0) {
              lineItemV2Dto.setEstimatedQuantityToRequest(estimatedQuantityToRequest);
            } else {
              lineItemV2Dto.setEstimatedQuantityToRequest(0);
            }
          });
        }
      }
    }
  }

  public void calcRequestedQuantityForMmit(SiglusRequisitionDto siglusRequisitionDto,
      Set<RequisitionLineItem> requisitionLineItems) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    Map<UUID, RequisitionLineItem> idToRequisitionLineItems = requisitionLineItems.stream()
        .collect(toMap(BaseEntity::getId, Function.identity()));

    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmit(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItems.forEach(lineItem -> {
      if (lineItem.getId() != null) {
        Integer requestedQuantity = getQuantityByLineItem(lineItem,
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            REQUESTED_QUANTITY_FACTOR);
        idToRequisitionLineItems.get(lineItem.getId()).setRequestedQuantity(requestedQuantity);
      } else {
        requisitionLineItems.add(lineItemDtoToEntity(lineItem));
      }
    });
  }


  public void calcRequestedQuantityForMmtb(SiglusRequisitionDto siglusRequisitionDto,
      Set<RequisitionLineItem> requisitionLineItems) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    Map<UUID, RequisitionLineItem> idToRequisitionLineItems = requisitionLineItems.stream()
        .collect(toMap(BaseEntity::getId, Function.identity()));

    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmtb(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItems.forEach(lineItem -> {
      if (lineItem.getId() != null) {
        Integer requestedQuantity = getQuantityByLineItem(lineItem,
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            REQUESTED_QUANTITY_FACTOR);
        idToRequisitionLineItems.get(lineItem.getId()).setRequestedQuantity(requestedQuantity);
      } else {
        requisitionLineItems.add(lineItemDtoToEntity(lineItem));
      }
    });
  }

  public RequisitionLineItem lineItemDtoToEntity(BaseRequisitionLineItemDto lineItem) {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setAdjustedConsumption(lineItem.getAdjustedConsumption());
    requisitionLineItem.setAverageConsumption(lineItem.getAverageConsumption());
    requisitionLineItem.setBeginningBalance(lineItem.getBeginningBalance());
    VersionIdentityDto orderableIdentity = lineItem.getOrderableIdentity();
    VersionEntityReference versionEntityReference = new VersionEntityReference(orderableIdentity.getId(),
        orderableIdentity.getVersionNumber());
    requisitionLineItem.setOrderable(versionEntityReference);
    requisitionLineItem.setPacksToShip(lineItem.getPacksToShip());
    requisitionLineItem.setSkipped(lineItem.getSkipped());
    requisitionLineItem.setStockOnHand(lineItem.getStockOnHand());
    requisitionLineItem.setTotalConsumedQuantity(lineItem.getTotalConsumedQuantity());
    requisitionLineItem.setTotalCost(lineItem.getTotalCost());
    requisitionLineItem.setTotalLossesAndAdjustments(lineItem.getTotalLossesAndAdjustments());
    requisitionLineItem.setTotalReceivedQuantity(lineItem.getTotalReceivedQuantity());
    VersionIdentityDto approvedProductIdentity = lineItem.getApprovedProductIdentity();
    VersionEntityReference versionEntityReference1 = new VersionEntityReference(approvedProductIdentity.getId(),
        approvedProductIdentity.getVersionNumber());
    requisitionLineItem.setFacilityTypeApprovedProduct(versionEntityReference1);
    requisitionLineItem.setRequestedQuantity(0);
    return requisitionLineItem;
  }

  @Transactional
  public List<SiglusRequisitionLineItemDto> createRequisitionLineItem(UUID requisitionId, List<UUID> orderableIds) {
    Profiler profiler = requisitionController.getProfiler("ADD_NEW_REQUISITION_LINE_ITEM_FOR_SPEC_ORDERABLE");
    Requisition requisition = requisitionController.findRequisition(requisitionId, profiler);
    for (UUID orderableId : orderableIds) {
      boolean alreadyHaveCurrentOrderable = requisition.getRequisitionLineItems().stream()
          .anyMatch(requisitionLineItem -> requisitionLineItem.getOrderable().getId().equals(orderableId));
      if (alreadyHaveCurrentOrderable) {
        throw new ValidationMessageException(new Message(MessageKeys.ERROR_ORDERABLE_ALREADY_IN_GIVEN_REQUISITION));
      }
    }
    UUID programId = requisition.getProgramId();
    ProgramDto program = requisitionController.findProgram(programId, profiler);
    UUID facilityId = requisition.getFacilityId();
    FacilityDto facility = requisitionController.findFacility(facilityId, profiler);
    permissionService.canInitOrAuthorizeRequisition(programId, facilityId);
    UserDto userDto = authenticationHelper.getCurrentUser();
    FacilityDto userFacility = requisitionController.findFacility(userDto.getHomeFacilityId(), profiler);

    BasicRequisitionTemplateDto template = requisitionV2Controller.getRequisition(requisitionId, null)
        .getTemplate();
    List<RequisitionLineItem> lineItemList = createLineItemWhenAddProduct(requisition, template, program, facility,
        orderableIds, userFacility);
    boolean isApprove = requisitionService.validateCanApproveRequisition(requisition, userDto.getId())
        .isSuccess();
    boolean isInternalFacility = userDto.getHomeFacilityId().equals(requisition.getFacilityId());
    boolean isExternalApprove = isApprove && !isInternalFacility;
    List<SiglusRequisitionLineItemDto> siglusLineItems = buildSiglusLineItem(lineItemList, isExternalApprove);
    List<BaseRequisitionLineItemDto> lineItems = siglusLineItems.stream()
        .map(item -> (BaseRequisitionLineItemDto) item.getLineItem()).collect(toList());
    initiateExpirationDate(lineItems, facilityId);
    initiateSuggestedQuantity(lineItems, facilityId, requisition.getProcessingPeriodId(), programId, template);
    return siglusLineItems;
  }

  @Transactional
  public BasicRequisitionDto submitRequisition(UUID requisitionId, HttpServletRequest request,
      HttpServletResponse response) {
    saveRequisitionWithValidation(requisitionId, request, response);
    BasicRequisitionDto basicRequisitionDto = requisitionController.submitRequisition(requisitionId, request, response);
    notificationService.postSubmit(basicRequisitionDto);
    activateArchivedProducts(requisitionId, basicRequisitionDto.getFacility().getId());
    return basicRequisitionDto;
  }

  @Transactional
  public BasicRequisitionDto authorizeRequisition(UUID requisitionId, HttpServletRequest request,
      HttpServletResponse response) {
    saveRequisitionWithValidation(requisitionId, request, response);
    BasicRequisitionDto basicRequisitionDto = requisitionController.authorizeRequisition(requisitionId, request,
        response);
    notificationService.postAuthorize(basicRequisitionDto);
    UUID facilityId = basicRequisitionDto.getFacility().getId();
    activateArchivedProducts(requisitionId, facilityId);
    return basicRequisitionDto;
  }

  @Transactional
  public BasicRequisitionDto rejectRequisition(UUID requisitionId, HttpServletRequest request,
      HttpServletResponse response) {
    verifyIsAndroidFacilityRequisition(requisitionId);
    BasicRequisitionDto dto = requisitionController.rejectRequisition(requisitionId, request, response);
    revertRequisition(requisitionId);
    notificationService.postReject(dto);
    return dto;
  }

  @Transactional
  public BasicRequisitionDto approveRequisition(UUID requisitionId, HttpServletRequest request,
      HttpServletResponse response) {
    saveRequisitionWithValidation(requisitionId, request, response);
    BasicRequisitionDto basicRequisitionDto = requisitionController
        .approveRequisition(requisitionId, request, response);
    UUID facilityId = basicRequisitionDto.getFacility().getId();
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    if (requisitionExtension == null) {
      requisitionExtension = new RequisitionExtension();
    }
    if (checkIsInternal(facilityId, authenticationHelper.getCurrentUser())) {
      activateArchivedProducts(requisitionId, facilityId);
      requisitionExtension.setIsApprovedByInternal(true);
    } else {
      requisitionExtension.setIsApprovedByInternal(false);
    }
    requisitionExtensionRepository.save(requisitionExtension);
    return basicRequisitionDto;
  }

  public Page<RequisitionWithSupplyingDepotsDto> getRequisitionsForConvertToOrder(UUID programId,
      UUID facilityId, Pageable pageable) {
    Page<RequisitionWithSupplyingDepotsDto> page = requisitionController.listForConvertToOrder(programId, facilityId,
        pageable);
    if (!page.hasContent()) {
      return page;
    }

    List<RequisitionWithSupplyingDepotsDto> dtos = page.getContent();
    Map<UUID, SimpleRequisitionDto> requisitionIdToSimpleRequisitionDto = getRequisitionIdToSimpleRequisitionDto(
        getRequisitionIds(dtos));
    dtos.forEach(dto -> {
      BasicRequisitionDto requisition = dto.getRequisition();
      requisition.setExtraData(getRequisitionExtraData(requisitionIdToSimpleRequisitionDto.get(requisition.getId())));
    });
    return page;
  }

  public Map<UUID, SimpleRequisitionDto> getRequisitionIdToSimpleRequisitionDto(Set<UUID> requisitionIds) {
    return requisitionNativeSqlRepository
        .findSimpleRequisitionDto(requisitionIds).stream()
        .collect(Collectors.toMap(SimpleRequisitionDto::getId, e -> e));
  }

  private SiglusRequisitionDto searchRequisition(UUID requisitionId, HttpServletResponse response) {
    RequisitionV2Dto requisitionDto = requisitionV2Controller.getRequisition(requisitionId, response);

    if (requisitionDto.getEmergency()) {
      Requisition regularRequisition = siglusRequisitionRepository.findOneByFacilityIdAndProgramIdAndProcessingPeriodId(
          requisitionDto.getFacilityId(), requisitionDto.getProgramId(), requisitionDto.getProcessingPeriodId());
      Set<UUID> regularFulFilledOrderableIds = getRegularFulFilledOrderableIds(regularRequisition);
      Set<UUID> regularRequestedOrderableIds = regularRequisition.getRequisitionLineItems().stream()
          .map(item -> item.getOrderable().getId()).collect(toSet());

      Set<VersionObjectReferenceDto> availableProducts = requisitionDto.getAvailableProducts().stream()
          .filter(product -> !regularRequestedOrderableIds.contains(product.getId())
              || regularFulFilledOrderableIds.contains(product.getId()))
          .collect(toSet());
      requisitionDto.setAvailableProducts(availableProducts);
    }

    setLineItemExtension(requisitionDto);
    RequisitionTemplateExtension extension = setTemplateExtension(requisitionDto);
    filterKits(requisitionDto);
    filterProductsIfEmergency(requisitionDto);
    SiglusRequisitionDto siglusRequisitionDto = getSiglusRequisitionDto(requisitionId, extension, requisitionDto);
    // set available products in approve page
    setAvailableProductsForApprovePage(siglusRequisitionDto);
    setApprovedByInternal(requisitionId, siglusRequisitionDto);
    siglusRequisitionDto.setRequisitionNumber(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId));
    calcEstimatedQuantityToRequest(requisitionDto);
    setIsCreateForClient(requisitionId, siglusRequisitionDto);
    return setIsFinalApproval(siglusRequisitionDto);
  }

  public SiglusRequisitionDto searchRequisition(UUID requisitionId) {
    return searchRequisition(requisitionId, response);
  }

  private Set<UUID> getRegularFulFilledOrderableIds(Requisition regularRequisition) {
    Map<UUID, Integer> orderableIdToRequestQuantityMap = regularRequisition.getRequisitionLineItems().stream()
        .peek(item -> log.info(item.getOrderable().toString() + "  quantity: " + item.getRequestedQuantity()))
        .collect(toMap(item -> item.getOrderable().getId(), RequisitionLineItem::getRequestedQuantity));
    Map<UUID, Long> orderableIdToFulfillQuantityMap = new HashMap<>();
    Set<UUID> orderExternalIds = orderExternalRepository
        .findOrderExternalIdByRequisitionId(regularRequisition.getId()).stream()
        .map(UUID::fromString).collect(toSet());
    orderExternalIds.add(regularRequisition.getId());
    List<Order> orders = siglusOrdersRepository.findAllByExternalIdIn(orderExternalIds);
    orders.forEach(order ->
        order.getOrderLineItems().forEach(item ->
            orderableIdToFulfillQuantityMap.put(item.getOrderable().getId(),
                orderableIdToFulfillQuantityMap.getOrDefault(item.getOrderable().getId(), 0L)
                    + item.getOrderedQuantity()
            ))
    );
    return orderableIdToFulfillQuantityMap.keySet().stream()
        .filter(orderableId ->
            orderableIdToFulfillQuantityMap.get(orderableId)
                >= orderableIdToRequestQuantityMap.getOrDefault(orderableId, 0))
        .collect(toSet());
  }

  public SiglusRequisitionDto searchRequisitionForFc(UUID requisitionId) {
    return searchRequisition(requisitionId, null);
  }

  private void setIsCreateForClient(UUID requisitionId, SiglusRequisitionDto siglusRequisitionDto) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    if (requisitionExtension == null
        || requisitionExtension.getCreatedByFacilityId() == null
        || Objects.equals(requisitionExtension.getCreatedByFacilityId(), siglusRequisitionDto.getFacilityId())) {
      siglusRequisitionDto.setIsCreateForClient(Boolean.FALSE);
    } else {
      siglusRequisitionDto.setIsCreateForClient(Boolean.TRUE);
    }
  }

  private Set<UUID> getRequisitionIds(List<RequisitionWithSupplyingDepotsDto> dtos) {
    Set<UUID> requisitionIds = Sets.newHashSet();
    dtos.forEach(dto -> requisitionIds.add(dto.getRequisition().getId()));
    return requisitionIds;
  }

  private void verifyIsAndroidFacilityRequisition(UUID requisitionId) {
    Profiler profiler = requisitionController.getProfiler("GET_REQUISITION", requisitionId);
    Requisition requisition = requisitionController.findRequisition(requisitionId, profiler);
    if (requisition != null) {
      FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(requisition.getFacilityId());
      if (facilityExtension != null && facilityExtension.getIsAndroid()) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message("siglusapi.error.noPermission.reject.android.requisition"));
      }
    }
  }

  private void setApprovedByInternal(UUID requisitionId, SiglusRequisitionDto siglusRequisitionDto) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    siglusRequisitionDto.setIsApprovedByInternal(null != requisitionExtension
        && null != requisitionExtension.getIsApprovedByInternal()
        && requisitionExtension.getIsApprovedByInternal());
  }

  public void revertRequisition(UUID requisitionId) {
    revertRequisitionLineItem(requisitionId);
    revertRequisitionLineItemExtension(requisitionId);
    deleteSiglusDraft(requisitionId);
  }

  private void revertRequisitionLineItem(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      requisitionLineItem.setApprovedQuantity(null);
      requisitionLineItem.setRemarks(null);
      requisitionLineItem.setSkipped(false);
    });
    requisitionRepository.save(requisition);
  }

  private void revertRequisitionLineItemExtension(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    List<UUID> ids = findLineItemIds(requisition);
    List<RequisitionLineItemExtension> extensions =
        ids.isEmpty() ? newArrayList() : lineItemExtensionRepository.findLineItems(ids);
    extensions.forEach(extension -> extension.setAuthorizedQuantity(null));
    if (CollectionUtils.isNotEmpty(extensions)) {
      log.info("clear line item extension authorizedQuantity: {}", extensions);
      lineItemExtensionRepository.save(extensions);
    }
  }

  private SiglusRequisitionDto getSiglusRequisitionDto(UUID requisitionId,
      RequisitionTemplateExtension extension, RequisitionV2Dto requisitionDto) {
    SiglusRequisitionDto siglusRequisitionDto;
    if (operatePermissionService.isEditable(requisitionDto)) {
      UserDto user = authenticationHelper.getCurrentUser();
      RequisitionDraft draft = draftRepository.findRequisitionDraftByRequisitionIdAndFacilityId(requisitionId,
          user.getHomeFacilityId());
      if (draft != null) {
        siglusRequisitionDto = SiglusRequisitionDto.from(requisitionDto);
        siglusUsageReportService.setUsageTemplateDto(requisitionDto.getTemplate().getId(), siglusRequisitionDto);
        fillRequisitionFromDraft(draft, extension, siglusRequisitionDto);
        return siglusRequisitionDto;
      }
    }
    return siglusUsageReportService.searchUsageReport(requisitionDto);
  }

  public Page<BasicRequisitionDto> searchRequisitions(MultiValueMap<String, String> queryParams, Pageable pageable) {
    Set<RequisitionStatus> requisitionStatusDisplayInRequisitionHistory =
        getRequisitionStatusDisplayInRequisitionHistory(
            UUID.fromString(queryParams.getFirst(QueryRequisitionSearchParams.FACILITY)),
            UUID.fromString(queryParams.getFirst(QueryRequisitionSearchParams.PROGRAM)));
    requisitionStatusDisplayInRequisitionHistory.forEach(requisitionStatus -> queryParams
        .add(REQUISITION_STATUS, requisitionStatus.toString()));
    RequisitionSearchParams params = new QueryRequisitionSearchParams(queryParams);
    return siglusRequisitionRequisitionService.searchRequisitions(params, pageable);
  }

  public List<FacilityDto> searchFacilitiesForApproval() {
    RightDto right = rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE);
    return getFacilitiesByRigth(right, PermissionService.REQUISITION_APPROVE);
  }

  public List<FacilityDto> searchFacilitiesForView() {
    RightDto right = rightReferenceDataService.findRight(PermissionService.REQUISITION_VIEW);
    return getFacilitiesByRigth(right, PermissionService.REQUISITION_VIEW);
  }

  private List<FacilityDto> getFacilitiesByRigth(RightDto right, String rightType) {
    List<RoleDto> roleDtos = roleReferenceDataService.search(right.getId());
    Set<UUID> roleIds = roleDtos.stream().map(RoleDto::getId).collect(toSet());
    UserDto userDto = authenticationHelper.getCurrentUser();

    // id : supervisory node
    Map<UUID, SupervisoryNodeDto> nodeDtoMap =
        supervisoryNodeReferenceDataService.findAllSupervisoryNodes()
            .stream()
            .collect(toMap(SupervisoryNodeDto::getId, node -> node));

    List<RequisitionGroupDto> requisitionGroupDtos = requisitionGroupReferenceDataService.findAll();
    // id : requisition group
    Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap = requisitionGroupDtos.stream()
        .collect(toMap(RequisitionGroupDto::getId, groupDto -> groupDto));
    Map<UUID, List<RequisitionGroupDto>> snIdToRequisitionGroupDtos = new HashMap<>();
    requisitionGroupDtos.forEach(rgDto -> {
      List<RequisitionGroupDto> rgDtos = snIdToRequisitionGroupDtos.get(rgDto.getSupervisoryNode().getId());
      if (rgDtos == null) {
        rgDtos = new ArrayList<>();
      }
      rgDtos.add(rgDto);
      snIdToRequisitionGroupDtos.put(rgDto.getSupervisoryNode().getId(), rgDtos);
    });

    // id : facility
    Map<UUID, FacilityDto> facilityDtoMap =
        facilityReferenceDataService.findAll()
            .stream()
            .collect(toMap(FacilityDto::getId, facilityDto -> facilityDto));

    Set<SupervisoryNodeDto> supervisoryNodeDtos = userDto.getRoleAssignments()
        .stream()
        .filter(roleAssignment -> roleIds.contains(roleAssignment.getRoleId()))
        .map(RoleAssignmentDto::getSupervisoryNodeId)
        .filter(Objects::nonNull)
        .map(nodeDtoMap::get)
        .collect(toSet());

    UUID homeId = userDto.getHomeFacilityId();

    Set<FacilityDto> facilityDtos = supervisoryNodeDtos
        .stream()
        .flatMap(supervisoryNodeDto ->
            getAllFacilityDtos(homeId, supervisoryNodeDto, requisitionGroupDtoMap, facilityDtoMap, nodeDtoMap,
                rightType, snIdToRequisitionGroupDtos).stream())
        .collect(toSet());
    if (PermissionService.REQUISITION_VIEW.equals(rightType)) {
      return addProgramsAndConvertToList(facilityDtos, facilityDtoMap.get(homeId));
    } else {
      return convertToList(facilityDtos, facilityDtoMap.get(homeId));
    }
  }

  // move homeFacility to first position
  private List<FacilityDto> convertToList(Set<FacilityDto> set, FacilityDto homeFacility) {
    List<FacilityDto> list = new ArrayList<>();
    if (set.contains(homeFacility) && set.size() > 1) {
      set.remove(homeFacility);
      list.add(homeFacility);
    }
    List<FacilityDto> sorted = sortFacility(set);
    list.addAll(sorted);
    return list;
  }

  private List<FacilityDto> addProgramsAndConvertToList(Set<FacilityDto> set, FacilityDto homeFacility) {
    set.remove(homeFacility);
    List<SupportedProgramDto> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedPrograms().stream()
        .map(this::copySupportedProgramDto)
        .collect(Collectors.toList());
    List<FacilityDto> list = new ArrayList<>();
    if (homeFacility != null) {
      homeFacility.setSupportedPrograms(supportedPrograms);
      list.add(homeFacility);
    }
    List<FacilityDto> sorted = sortFacility(set);
    list.addAll(sorted);
    return list;
  }

  private SupportedProgramDto copySupportedProgramDto(
      org.siglus.siglusapi.dto.SupportedProgramDto fromSupportedProgramDto) {
    SupportedProgramDto toSupportedProgramDto = new SupportedProgramDto();
    BeanUtils.copyProperties(fromSupportedProgramDto, toSupportedProgramDto);
    return toSupportedProgramDto;
  }

  private List<FacilityDto> sortFacility(Set<FacilityDto> set) {
    List<FacilityDto> list = new ArrayList<>(set);
    list.sort((FacilityDto f1, FacilityDto f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
    return list;
  }

  // check if internal only && not final approve
  private boolean isInternalApproveOnly(UUID userHomeFacilityId, String rightType,
      SupervisoryNodeDto supervisoryNodeDto, Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap) {
    UUID requisitionGroupId = supervisoryNodeDto.getRequisitionGroupId();
    if (requisitionGroupId == null) {
      return false;
    }
    Set<UUID> idsToApprove = requisitionGroupDtoMap.get(requisitionGroupId)
        .getMemberFacilities()
        .stream()
        .map(FacilityDto::getId)
        .collect(toSet());
    boolean isInternalApproveOnly =
        idsToApprove.contains(userHomeFacilityId) && supervisoryNodeDto.getParentNode() != null;
    if (!isInternalApproveOnly && PermissionService.REQUISITION_VIEW.equals(rightType)) {
      isInternalApproveOnly = (userHomeFacilityId != supervisoryNodeDto.getFacility().getId());
    }
    return isInternalApproveOnly;
  }

  // get all facilities of this supervisoryNode
  private Set<FacilityDto> getAllFacilityDtos(UUID userHomeFacilityId,
      SupervisoryNodeDto supervisoryNodeDto,
      Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap,
      Map<UUID, FacilityDto> facilityDtoMap,
      Map<UUID, SupervisoryNodeDto> nodeDtoMap,
      String rightType, Map<UUID, List<RequisitionGroupDto>> snIdToRequisitionGroupDtos) {
    if (isInternalApproveOnly(userHomeFacilityId, rightType, supervisoryNodeDto, requisitionGroupDtoMap)) {
      return Sets.newHashSet(facilityDtoMap.get(userHomeFacilityId));
    }
    return getFacilityDtosByOwnAndChildSupervisoryNode(supervisoryNodeDto, requisitionGroupDtoMap, nodeDtoMap,
        snIdToRequisitionGroupDtos);

  }

  // get facilities to approve of its own node
  private Set<FacilityDto> getFacilityDtosBySupervisoryNode(Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap,
      List<RequisitionGroupDto> rgDtos) {
    if (rgDtos == null) {
      return Collections.emptySet();
    }
    return rgDtos
        .stream()
        .map(rgDto -> requisitionGroupDtoMap.get(rgDto.getId()).getMemberFacilities())
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  // get facilities to approve of its own and all child supervisory node
  private Set<FacilityDto> getFacilityDtosByOwnAndChildSupervisoryNode(
      SupervisoryNodeDto supervisoryNodeDto,
      Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap,
      Map<UUID, SupervisoryNodeDto> nodeDtoMap,
      Map<UUID, List<RequisitionGroupDto>> snIdToRequisitionGroupDtos
  ) {
    if (supervisoryNodeDto.getChildNodes().isEmpty()) {
      return getFacilityDtosBySupervisoryNode(requisitionGroupDtoMap,
          snIdToRequisitionGroupDtos.get(supervisoryNodeDto.getId()));
    } else {
      return supervisoryNodeDto.getChildNodes()
          .stream()
          .flatMap(child -> {
            // needs optimize
            SupervisoryNodeDto childNode = nodeDtoMap.get(child.getId());
            Set<FacilityDto> childSet = getFacilityDtosByOwnAndChildSupervisoryNode(childNode, requisitionGroupDtoMap,
                nodeDtoMap, snIdToRequisitionGroupDtos);
            Set<FacilityDto> allSet = getFacilityDtosBySupervisoryNode(requisitionGroupDtoMap,
                snIdToRequisitionGroupDtos.get(supervisoryNodeDto.getId()));
            return mergeSets(allSet, childSet).stream();
          }).collect(toSet());
    }
  }

  Set<FacilityDto> mergeSets(Set<FacilityDto> set1, Set<FacilityDto> set2) {
    if (set1.isEmpty()) {
      return set2;
    }

    if (set2.isEmpty()) {
      return set1;
    }

    set1.addAll(set2);
    return set1;
  }

  private boolean checkIsInternal(UUID requisitionFacilityId, UserDto userDto) {
    // permission check needs requisition, ignore requisitionService.validateCanApproveRequisition
    return userDto.getHomeFacilityId().equals(requisitionFacilityId);
  }

  private SiglusRequisitionDto saveRequisitionDraft(SiglusRequisitionDto requisitionDto) {
    UserDto user = authenticationHelper.getCurrentUser();
    RequisitionDraft draft = draftRepository.findByRequisitionId(requisitionDto.getId());
    RequisitionTemplate template = requisitionRepository.findOne(requisitionDto.getId()).getTemplate();
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(template.getId());
    template.setTemplateExtension(templateExtension);
    RequisitionDraft requisitionDraft = RequisitionDraft
        .from(requisitionDto, template, (draft == null ? null : draft.getId()), user);

    if (template.isColumnDisplayed(ESTIMATED_QUANTITY) && template.isColumnCalculated(ESTIMATED_QUANTITY)) {
      calcEstimatedQuantityDraft(requisitionDto, requisitionDraft);
    }
    if (template.isColumnDisplayed(REQUESTED_QUANTITY) && template.isColumnStockBased(REQUESTED_QUANTITY)) {
      calcRequestedQuantityDraft(requisitionDto, requisitionDraft);
    }
    log.info("save requisition draft extension: {}", requisitionDraft);
    draft = draftRepository.save(requisitionDraft);
    fillRequisitionFromDraft(draft, template.getTemplateExtension(), requisitionDto);
    return requisitionDto;
  }

  public void calcRequestedQuantityDraft(SiglusRequisitionDto siglusRequisitionDto, RequisitionDraft requisitionDraft) {
    String programCode = programRepository.findOne(siglusRequisitionDto.getProgramId()).getCode().toString();
    if (CALC_PROGRAM_CODE.contains(programCode)) {
      programToRequestedQuantityDraft.get(programCode).accept(siglusRequisitionDto, requisitionDraft);
    }
  }

  public void calcRequestedQuantityDraftForMmtb(SiglusRequisitionDto siglusRequisitionDto,
      RequisitionDraft requisitionDraft) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));

    List<RequisitionLineItemDraft> lineItemDrafts = requisitionDraft.getLineItems();
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() == null) {
        lineItemDraft.setRequestedQuantity(0);
      }
    });
    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmtb(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() != null) {
        Integer requestedQuantity = getQuantityByLineItem(idToLineItems.get(lineItemDraft.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            REQUESTED_QUANTITY_FACTOR);
        lineItemDraft.setRequestedQuantity(requestedQuantity);
      }
    });
  }

  public void calcRequestedQuantityDraftForMmia(SiglusRequisitionDto siglusRequisitionDto,
      RequisitionDraft requisitionDraft) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));

    List<RequisitionLineItemDraft> lineItemDrafts = requisitionDraft.getLineItems();
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() == null) {
        lineItemDraft.setRequestedQuantity(0);
      }
    });

    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumberForMmia(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(regimenCodeToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByRegimenCodeIn(regimenCodeToPatientNumber.keySet());
    }
    double correctionFactorForMmia = getCorrectionFactorForMmia(siglusRequisitionDto.getPatientLineItems());
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() != null) {
        Integer requestedQuantity = getQuantityByLineItem(idToLineItems.get(lineItemDraft.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            correctionFactorForMmia,
            REQUESTED_QUANTITY_FACTOR);
        lineItemDraft.setRequestedQuantity(requestedQuantity);
      }
    });
  }

  public void calcRequestedQuantityDraftForMmit(SiglusRequisitionDto siglusRequisitionDto,
      RequisitionDraft requisitionDraft) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));

    List<RequisitionLineItemDraft> lineItemDrafts = requisitionDraft.getLineItems();
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() == null) {
        lineItemDraft.setRequestedQuantity(0);
      }
    });

    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmit(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() != null) {
        Integer requestedQuantity = getQuantityByLineItem(idToLineItems.get(lineItemDraft.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            REQUESTED_QUANTITY_FACTOR);
        lineItemDraft.setRequestedQuantity(requestedQuantity);
      }
    });
  }

  public void calcEstimatedQuantityDraft(SiglusRequisitionDto siglusRequisitionDto, RequisitionDraft requisitionDraft) {
    String programCode = programRepository.findOne(siglusRequisitionDto.getProgramId()).getCode().toString();
    if (CALC_PROGRAM_CODE.contains(programCode)) {
      programToEstimatedQuantityDraft.get(programCode).accept(siglusRequisitionDto, requisitionDraft);
    }
  }

  public void calcEstimatedQuantityDraftForMmia(SiglusRequisitionDto siglusRequisitionDto,
      RequisitionDraft requisitionDraft) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());

    List<RequisitionLineItemDraft> lineItemDrafts = requisitionDraft.getLineItems();
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() == null) {
        lineItemDraft.setEstimatedQuantity(0);
      }
    });

    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumberForMmia(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(regimenCodeToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByRegimenCodeIn(regimenCodeToPatientNumber.keySet());
    }
    double correctionFactorForMmia = getCorrectionFactorForMmia(siglusRequisitionDto.getPatientLineItems());
    log.info("calcEstimatedQuantityDraftForMmia: {}", correctionFactorForMmia);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() != null) {
        Integer estimatedQuantity = getQuantityByLineItem(idToLineItems.get(lineItemDraft.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            correctionFactorForMmia,
            ESTIMATED_QUANTITY_FACTOR);
        lineItemDraft.setEstimatedQuantity(estimatedQuantity);
      }
    });
  }

  public void calcEstimatedQuantityDraftForMmit(SiglusRequisitionDto siglusRequisitionDto,
      RequisitionDraft requisitionDraft) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));

    List<RequisitionLineItemDraft> lineItemDrafts = requisitionDraft.getLineItems();
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() == null) {
        lineItemDraft.setEstimatedQuantity(0);
      }
    });

    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmit(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() != null) {
        Integer estimatedQuantity = getQuantityByLineItem(idToLineItems.get(lineItemDraft.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            ESTIMATED_QUANTITY_FACTOR);
        lineItemDraft.setEstimatedQuantity(estimatedQuantity);
      }
    });
  }

  public void calcEstimatedQuantityDraftForMmtb(SiglusRequisitionDto siglusRequisitionDto,
      RequisitionDraft requisitionDraft) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));

    List<RequisitionLineItemDraft> lineItemDrafts = requisitionDraft.getLineItems();
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() == null) {
        lineItemDraft.setEstimatedQuantity(0);
      }
    });
    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmtb(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    lineItemDrafts.forEach(lineItemDraft -> {
      if (lineItemDraft.getRequisitionLineItemId() != null) {
        Integer estimatedQuantity = getQuantityByLineItem(idToLineItems.get(lineItemDraft.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            ESTIMATED_QUANTITY_FACTOR);
        lineItemDraft.setEstimatedQuantity(estimatedQuantity);
      }
    });
  }

  private SiglusRequisitionDto saveRequisitionWithoutValidation(UUID requisitionId,
      SiglusRequisitionDto requisitionDto, HttpServletRequest request, HttpServletResponse response) {
    return saveRequisition(requisitionId, requisitionDto, request, response, false);
  }

  private void saveRequisitionWithValidation(UUID requisitionId,
      HttpServletRequest request, HttpServletResponse response) {
    saveRequisition(requisitionId, null, request, response, true);
  }

  private SiglusRequisitionDto saveRequisition(UUID requisitionId,
      SiglusRequisitionDto requisitionDto, HttpServletRequest request,
      HttpServletResponse response, boolean validate) {
    // call modify OpenLMIS API
    RequisitionDraft draft = null;
    if (requisitionDto == null) {
      RequisitionV2Dto dto = siglusRequisitionRequisitionService.searchRequisition(requisitionId);
      requisitionDto = SiglusRequisitionDto.from(dto);
      draft = draftRepository.findByRequisitionId(requisitionDto.getId());
      if (draft != null) {
        RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
            .findByRequisitionTemplateId(dto.getTemplate().getId());
        fillRequisitionFromDraft(draft, templateExtension, requisitionDto);
      } else {
        setLineItemExtension(requisitionDto);
        RequisitionTemplateExtension extension = setTemplateExtension(requisitionDto);
        requisitionDto = getSiglusRequisitionDto(requisitionId, extension, requisitionDto);
      }
    }
    RequisitionV2Dto updateRequisitionDto = requisitionV2Controller
        .updateRequisition(requisitionId, requisitionDto, request, response);
    if (draft != null) {
      draftRepository.delete(draft.getId());
    }

    saveLineItemExtension(requisitionDto, updateRequisitionDto, requisitionDto);
    RequisitionTemplate template = requisitionRepository.findOne(requisitionDto.getId()).getTemplate();
    if (template.isColumnDisplayed(REQUESTED_QUANTITY) && template.isColumnStockBased(REQUESTED_QUANTITY)) {
      calcRequestedQuantity(requisitionDto);
    }

    if (validate) {
      return siglusUsageReportService.saveUsageReportWithValidation(requisitionDto, updateRequisitionDto);
    }
    return siglusUsageReportService.saveUsageReport(requisitionDto, updateRequisitionDto);
  }

  public void calcEstimatedQuantity(SiglusRequisitionDto siglusRequisitionDto,
      List<RequisitionLineItemExtension> extensions) {
    String programCode = programRepository.findOne(siglusRequisitionDto.getProgramId()).getCode().toString();
    if (CALC_PROGRAM_CODE.contains(programCode)) {
      programToEstimatedQuantity.get(programCode).accept(siglusRequisitionDto, extensions);
    }
  }


  public void calcEstimatedQuantityForMmia(SiglusRequisitionDto siglusRequisitionDto,
      List<RequisitionLineItemExtension> extensions) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());

    extensions.forEach(extension -> {
      if (extension.getId() == null) {
        extension.setEstimatedQuantity(0);
      }
    });

    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumberForMmia(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(regimenCodeToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByRegimenCodeIn(regimenCodeToPatientNumber.keySet());
    }
    double correctionFactorForMmia = getCorrectionFactorForMmia(siglusRequisitionDto.getPatientLineItems());
    log.info("calcEstimatedQuantityForMmia: {}", correctionFactorForMmia);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));
    extensions.forEach(extension -> {
      if (extension.getId() != null) {
        Integer estimatedQuantity = getQuantityByLineItem(idToLineItems.get(extension.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            correctionFactorForMmia,
            ESTIMATED_QUANTITY_FACTOR);
        extension.setEstimatedQuantity(estimatedQuantity);
      }
    });
  }

  public void calcEstimatedQuantityForMmit(SiglusRequisitionDto siglusRequisitionDto,
      List<RequisitionLineItemExtension> extensions) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));

    extensions.forEach(extension -> {
      if (extension.getId() == null) {
        extension.setEstimatedQuantity(0);
      }
    });

    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmit(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    extensions.forEach(extension -> {
      if (extension.getId() != null) {
        Integer estimatedQuantity = getQuantityByLineItem(idToLineItems.get(extension.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            ESTIMATED_QUANTITY_FACTOR);
        extension.setEstimatedQuantity(estimatedQuantity);
      }
    });
  }

  public void calcEstimatedQuantityForMmtb(SiglusRequisitionDto siglusRequisitionDto,
      List<RequisitionLineItemExtension> extensions) {
    List<BaseRequisitionLineItemDto> lineItems = siglusRequisitionDto.getLineItems();
    lineItems = lineItems.stream()
        .filter(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getId() != null)
        .collect(toList());
    Map<UUID, BaseRequisitionLineItemDto> idToLineItems = lineItems.stream()
        .collect(toMap(BaseDto::getId, Function.identity()));

    extensions.forEach(extension -> {
      if (extension.getId() == null) {
        extension.setEstimatedQuantity(0);
      }
    });

    Map<String, Integer> mappingKeyToPatientNumber = getMappingKeyToPatientNumberForMmtb(siglusRequisitionDto);
    Set<RegimenOrderable> regimenOrderables = new HashSet<>();
    if (CollectionUtils.isNotEmpty(mappingKeyToPatientNumber.keySet())) {
      regimenOrderables = regimenOrderableRepository.findByMappingKeyIn(mappingKeyToPatientNumber.keySet());
    }
    Map<String, Integer> regimenCodeToPatientNumber = getRegimenCodeToPatientNumber(mappingKeyToPatientNumber,
        regimenOrderables);
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables =
        getOrderableCodeToRegimenOrderables(regimenOrderables);
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(lineItems);
    extensions.forEach(extension -> {
      if (extension.getId() != null) {
        Integer estimatedQuantity = getQuantityByLineItem(idToLineItems.get(extension.getRequisitionLineItemId()),
            orderableCodeToRegimenOrderables,
            orderableIdToCode,
            regimenCodeToPatientNumber,
            DEFAULT_CORRECTION_FACTOR,
            ESTIMATED_QUANTITY_FACTOR);
        extension.setEstimatedQuantity(estimatedQuantity);
      }
    });
  }

  public Integer getQuantityByLineItem(BaseRequisitionLineItemDto baseRequisitionLineItemDto,
      Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables,
      Map<UUID, String> orderableIdToCode,
      Map<String, Integer> regimenCodeToPatientNumber,
      double correctionFactor,
      int calcFactor) {
    double consumptions = 0.0;
    List<RegimenOrderable> regimenOrderables = getRegimenOrderablesFromOrderable(baseRequisitionLineItemDto,
        orderableCodeToRegimenOrderables, orderableIdToCode);
    if (regimenOrderables != null) {
      for (RegimenOrderable regimenOrderable : regimenOrderables) {
        double singleQtyInSingleRegimen = regimenOrderable.getQuantity();
        int patientsInSingleRegimen = regimenCodeToPatientNumber.getOrDefault(
            regimenOrderable.getRegimenCode(), 0);
        consumptions += singleQtyInSingleRegimen * patientsInSingleRegimen * correctionFactor;
      }
    }
    Integer inventory = baseRequisitionLineItemDto.getStockOnHand();
    inventory = Optional.ofNullable(inventory).orElse(0);
    return Math.max(0, (int) Math.ceil(consumptions * calcFactor) - inventory);
  }

  public List<RegimenOrderable> getRegimenOrderablesFromOrderable(
      BaseRequisitionLineItemDto baseRequisitionLineItemDto,
      Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables,
      Map<UUID, String> orderableIdToCode) {
    UUID orderableId = baseRequisitionLineItemDto.getOrderableIdentity().getId();
    return orderableCodeToRegimenOrderables.get(orderableIdToCode.get(orderableId));
  }

  public Map<String, List<RegimenOrderable>> getOrderableCodeToRegimenOrderables(
      Set<RegimenOrderable> regimenOrderables) {
    Map<String, List<RegimenOrderable>> orderableCodeToRegimenOrderables = new HashMap<>();
    regimenOrderables.forEach(regimenOrderable -> {
      if (orderableCodeToRegimenOrderables.containsKey(regimenOrderable.getOrderableCode())) {
        orderableCodeToRegimenOrderables.get(regimenOrderable.getOrderableCode()).add(regimenOrderable);
      } else {
        orderableCodeToRegimenOrderables.put(regimenOrderable.getOrderableCode(), newArrayList(regimenOrderable));
      }
    });
    return orderableCodeToRegimenOrderables;
  }

  Map<UUID, String> getOrderableIdToCode(
      List<BaseRequisitionLineItemDto> baseRequisitionLineItemDtos) {
    // get products
    Set<UUID> orderableIds = baseRequisitionLineItemDtos.stream()
        .map(baseRequisitionLineItemDto -> baseRequisitionLineItemDto.getOrderableIdentity().getId())
        .collect(toSet());

    List<Orderable> orderables = new ArrayList<>();
    if (!orderableIds.isEmpty()) {
      orderables = orderableRepository.findLatestByIds(orderableIds);
    }

    return orderables.stream()
        .collect(toMap(Orderable::getId, orderable -> orderable.getProductCode().toString()));
  }

  Map<String, Integer> getRegimenCodeToPatientNumber(Map<String, Integer> mappingKeyToPatientNumber,
      Set<RegimenOrderable> regimenOrderables) {
    Map<String, Integer> regimenCodeToPatientNumber = new HashMap<>();
    regimenOrderables.forEach(regimenOrderable ->
        regimenCodeToPatientNumber.put(regimenOrderable.getRegimenCode(),
            mappingKeyToPatientNumber.get(regimenOrderable.getMappingKey())));
    return regimenCodeToPatientNumber;
  }

  public Map<String, Integer> getMappingKeyToPatientNumberForMmtb(SiglusRequisitionDto siglusRequisitionDto) {
    Map<String, Integer> mappingKeyToPatientNumber = new HashMap<>();
    Set<String> existedMappingKeys = regimenOrderableRepository.findByExistedMappingKey();
    List<PatientGroupDto> patientGroupDtos = siglusRequisitionDto.getPatientLineItems();
    patientGroupDtos.forEach(patientGroupDto -> {
      Map<String, PatientColumnDto> columns = patientGroupDto.getColumns();
      String groupName = patientGroupDto.getName();
      columns.forEach((key, value) -> {
        String mappingKey = groupName + FieldConstants.SEPARATOR + key;
        if (existedMappingKeys.contains(mappingKey)) {
          mappingKeyToPatientNumber.put(mappingKey, Optional.ofNullable(value.getValue()).orElse(0));
        }
      });
    });
    return mappingKeyToPatientNumber;
  }

  public Map<String, Integer> getMappingKeyToPatientNumberForMmit(SiglusRequisitionDto siglusRequisitionDto) {
    Map<String, Integer> mappingKeyToPatientNumber = new HashMap<>();
    Set<String> existedMappingKeys = regimenOrderableRepository.findByExistedMappingKey();
    List<TestConsumptionServiceDto> testConsumptionServiceDtos = siglusRequisitionDto.getTestConsumptionLineItems();
    testConsumptionServiceDtos.forEach(testConsumptionServiceDto -> {
      String serviceName = testConsumptionServiceDto.getService();
      Map<String, TestConsumptionProjectDto> projects = testConsumptionServiceDto.getProjects();
      projects.values().forEach(value -> {
        Map<String, TestConsumptionOutcomeDto> outcomes = value.getOutcomes();
        outcomes.values().forEach(value2 -> {
          String mappingKey = serviceName
              + FieldConstants.SEPARATOR
              + value.getProject()
              + FieldConstants.SEPARATOR
              + value2.getOutcome();
          if (existedMappingKeys.contains(mappingKey)) {
            mappingKeyToPatientNumber.put(mappingKey, Optional.ofNullable(value2.getValue()).orElse(0));
          }
        });
      });
    });
    return mappingKeyToPatientNumber;
  }

  double getCorrectionFactorForMmia(List<PatientGroupDto> patientGroupDtos) {
    PatientGroupDto dsGroup = patientGroupDtos.stream()
        .filter(p -> NEW_SECTION_2.equals(p.getName())).findFirst()
        .orElseThrow(() -> new NotFoundException(NEW_SECTION_2 + " not found"));
    PatientGroupDto dtGroup = patientGroupDtos.stream()
        .filter(p -> NEW_SECTION_3.equals(p.getName())).findFirst()
        .orElseThrow(() -> new NotFoundException(NEW_SECTION_3 + " not found"));
    Optional<PatientGroupDto> dbGroup = patientGroupDtos.stream()
        .filter(p -> NEW_SECTION_9.equals(p.getName())).findFirst();
    PatientGroupDto dmGroup = patientGroupDtos.stream()
        .filter(p -> NEW_SECTION_4.equals(p.getName())).findFirst()
        .orElseThrow(() -> new NotFoundException(NEW_SECTION_4 + " not found"));

    Integer dsTotal = dsGroup.getColumns().get(TOTAL_COLUMN).getValue();
    Integer dtTotal = dtGroup.getColumns().get(TOTAL_COLUMN).getValue();
    Integer dbTotal = 0;
    if (dbGroup.isPresent()) {
      PatientColumnDto column = dbGroup.get().getColumns().get(TOTAL_COLUMN);
      if (column != null && column.getValue() != null) {
        dbTotal = column.getValue();
      }
    }
    Integer dmTotal = dmGroup.getColumns().get(TOTAL_COLUMN).getValue();
    if (dsTotal == null || dtTotal == null || dmTotal == null) {
      return 1;
    }
    int totalPatients = dsTotal + dtTotal + dbTotal + dmTotal;

    Integer dsColumn3 = dsGroup.getColumns().get(NEW_COLUMN_3).getValue();
    Integer dtColumn1 = dtGroup.getColumns().get(NEW_COLUMN_1).getValue();
    Integer dbThisMonth = 0;
    if (dbGroup.isPresent()) {
      PatientColumnDto column = dbGroup.get().getColumns().get(NEW_COLUMN_1);
      if (column != null && column.getValue() != null) {
        dbThisMonth = column.getValue();
      }
    }
    Integer dmColumn = dmGroup.getColumns().get(NEW_COLUMN).getValue();
    if (dsColumn3 == null || dtColumn1 == null || dmColumn == null) {
      return 1;
    }
    int totalPatientsInThisMonth = dsColumn3 + dtColumn1 + dbThisMonth + dmColumn;

    if (totalPatients == 0 || totalPatientsInThisMonth == 0) {
      return 1;
    }
    return totalPatients * 1.0 / totalPatientsInThisMonth;
  }

  Map<String, Integer> getRegimenCodeToPatientNumberForMmia(SiglusRequisitionDto siglusRequisitionDto) {
    Map<String, Integer> regimenCodeToPatientNumber = new HashMap<>();
    List<RegimenLineDto> regimenLineDtos = siglusRequisitionDto.getRegimenLineItems();
    List<RegimenLineDto> filteredRegimenLineItems = regimenLineDtos.stream()
        .filter(regimenLineDto -> regimenLineDto.getRegimen() != null).collect(toList());
    filteredRegimenLineItems.forEach(filteredRegimenLineItem -> {
      Map<String, RegimenColumnDto> columns = filteredRegimenLineItem.getColumns();
      Integer patients = columns.get(MMIA_COLUMN_NAME).getValue();
      patients = Optional.ofNullable(patients).orElse(0);
      regimenCodeToPatientNumber.put(filteredRegimenLineItem.getRegimen().getCode(), patients);
    });
    return regimenCodeToPatientNumber;
  }

  private void saveLineItemExtension(RequisitionV2Dto toUpdatedDto, RequisitionV2Dto updatedDto,
      SiglusRequisitionDto siglusRequisitionDto) {
    List<RequisitionLineItem.Importer> lineItems = updatedDto.getRequisitionLineItems();
    if (!lineItems.isEmpty()) {
      List<UUID> lineItemsId = updatedDto.getRequisitionLineItems()
          .stream()
          .map(Importer::getId)
          .collect(Collectors.toList());
      List<RequisitionLineItemExtension> updateExtension = new ArrayList<>();
      List<RequisitionLineItemExtension> extensions =
          lineItemExtensionRepository.findLineItems(lineItemsId);
      lineItems.forEach(lineItem -> {
        RequisitionLineItemV2Dto dto = findDto(lineItem, toUpdatedDto);
        if (null == dto) {
          return;
        }
        RequisitionLineItemExtension existedExtension = findLineItemExtension(extensions, dto);
        if (existedExtension != null) {
          existedExtension.setAuthorizedQuantity(dto.getAuthorizedQuantity());
          existedExtension.setSuggestedQuantity(dto.getSuggestedQuantity());
          existedExtension.setExpirationDate(dto.getExpirationDate());
          updateExtension.add(existedExtension);
        } else {
          RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
          extension.setRequisitionLineItemId(lineItem.getId());
          extension.setAuthorizedQuantity(dto.getAuthorizedQuantity());
          extension.setSuggestedQuantity(dto.getSuggestedQuantity());
          extension.setExpirationDate(dto.getExpirationDate());
          extension.setEstimatedQuantity(0);
          updateExtension.add(extension);
        }
      });

      RequisitionTemplate template = requisitionRepository.findOne(siglusRequisitionDto.getId()).getTemplate();
      if (template.isColumnDisplayed(ESTIMATED_QUANTITY) && template.isColumnCalculated(ESTIMATED_QUANTITY)
          && siglusRequisitionDto.getStatus().duringApproval()) {
        calcEstimatedQuantity(siglusRequisitionDto, updateExtension);
      }

      log.info("lineItem Extension Repository {}", updateExtension);
      lineItemExtensionRepository.save(updateExtension);
    }
  }

  private RequisitionLineItemV2Dto findDto(RequisitionLineItem.Importer lineItem, RequisitionV2Dto dto) {
    for (RequisitionLineItem.Importer lineItemV2Dto : dto.getRequisitionLineItems()) {
      if (lineItemV2Dto.getOrderableIdentity().getId()
          .equals(lineItem.getOrderableIdentity().getId())) {
        return (RequisitionLineItemV2Dto) lineItemV2Dto;
      }
    }
    return null;
  }

  private List<RequisitionLineItem> createLineItemWhenAddProduct(Requisition requisition,
      BasicRequisitionTemplateDto template, ProgramDto program,
      FacilityDto facility, List<UUID> orderableIds, FacilityDto userFacility) {
    List<StockCardRangeSummaryDto> stockCardRangeSummaries = Collections.emptyList();
    List<ApprovedProductDto> approvedProducts =
        requisitionService.getApprovedProducts(userFacility.getId(), program.getId())
            .stream()
            .filter(approvedProduct -> orderableIds.contains(approvedProduct.getOrderable().getId()))
            .collect(Collectors.toList());
    if (template.isPopulateStockOnHandFromStockCards() && Boolean.TRUE.equals(requisition.getEmergency())) {
      Map<UUID, List<ApprovedProductDto>> groupApprovedProduct =
          approvedProducts.stream().collect(groupingBy(approvedProduct -> approvedProduct.getProgram().getId()));
      stockCardRangeSummaries = getStockCardRangeSummaryDtos(facility, groupApprovedProduct,
          requisition.getActualStartDate(), requisition.getActualEndDate());
    }
    Map<UUID, Integer> orderableBeginning = stockManagementRepository.getStockOnHandByProduct(facility.getId(),
        requisition.getActualStartDate().minusDays(1));
    Map<UUID, Integer> orderableSoh = stockManagementRepository.getStockOnHandByProduct(facility.getId(),
        requisition.getActualEndDate());
    List<RequisitionLineItem> lineItemList = new ArrayList<>();
    for (ApprovedProductDto approvedProductDto : approvedProducts) {
      UUID orderableId = approvedProductDto.getOrderable().getId();
      Integer beginningBalances = orderableBeginning.get(orderableId);
      Integer stockOnHand = orderableSoh.get(orderableId);
      lineItemList.add(requisition.createLineItemWhenAddProduct(template, stockOnHand,
          beginningBalances, approvedProductDto, stockCardRangeSummaries, program.getCode(),
          facility.getType().getCode()));
    }
    return lineItemList;
  }

  private List<StockCardRangeSummaryDto> getStockCardRangeSummaryDtos(FacilityDto facility,
      Map<UUID, List<ApprovedProductDto>> groupApprovedProduct, LocalDate startDate, LocalDate endDate) {
    return groupApprovedProduct.keySet().stream()
        .map(programId -> {
          Set<VersionIdentityDto> orderableIdentities = groupApprovedProduct.get(programId)
              .stream()
              .map(approveProduct -> {
                OrderableDto orderableDto = approveProduct.getOrderable();
                return new VersionIdentityDto(orderableDto.getId(), orderableDto.getVersionNumber());
              })
              .collect(Collectors.toSet());
          return stockCardRangeSummaryStockManagementService.search(programId, facility.getId(), orderableIdentities,
              null, startDate, endDate);
        })
        .flatMap(Collection::stream)
        .collect(toList());
  }

  @Transactional
  public void deleteRequisition(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    deleteExtensionForRequisition(requisition);
    revertGeneratedNumber(requisition);
    deleteSiglusDraft(requisitionId);
    siglusUsageReportService.deleteUsageReport(requisitionId);
    RequisitionExtension requisitionExtension =
        siglusRequisitionExtensionService.deleteRequisitionExtension(requisitionId);
    if (requisitionExtension != null
        && !requisitionExtension.getCreatedByFacilityId().equals(requisition.getFacilityId())) {
      siglusRequisitionRepository.deleteById(requisitionId);
      requisitionExtensionRepository.deleteByRequisitionId(requisitionId);
      requisitionLineItemExtensionRepository.deleteByRequisitionId(requisitionId);
      ageGroupLineItemRepository.deleteByRequisitionId(requisitionId);
      consultationNumberLineItemRepository.deleteByRequisitionId(requisitionId);
      usageInformationLineItemRepository.deleteByRequisitionId(requisitionId);
      patientLineItemRepository.deleteByRequisitionId(requisitionId);
      testConsumptionLineItemRepository.deleteByRequisitionId(requisitionId);
      regimenLineItemRepository.deleteByRequisitionId(requisitionId);
      regimenSummaryLineItemRepository.deleteByRequisitionId(requisitionId);
      kitUsageRepository.deleteByRequisitionId(requisitionId);
    } else {
      siglusRequisitionRequisitionService.deleteRequisition(requisitionId);
    }
    notificationService.postDelete(requisitionId);
  }

  private List<RequisitionV2Dto> getPreviousEmergencyRequisition(BaseRequisitionDto requisition) {
    return siglusRequisitionRequisitionService.getPreviousEmergencyRequisition(from(requisition));
  }

  private void deleteExtensionForRequisition(Requisition requisition) {
    List<UUID> ids = findLineItemIds(requisition);
    List<RequisitionLineItemExtension> extensions =
        ids.isEmpty() ? new ArrayList<>() : lineItemExtensionRepository.findLineItems(ids);
    if (!extensions.isEmpty()) {
      log.info("delete line item extension: {}", extensions);
      lineItemExtensionRepository.delete(extensions);
    }
  }

  private void revertGeneratedNumber(Requisition requisition) {
    ProcessingPeriod period = processingPeriodRepository.findOneById(requisition.getProcessingPeriodId());
    siglusGeneratedNumberService.revertGeneratedNumber(requisition.getFacilityId(), requisition.getProgramId(),
        period.getEndDate().getYear(), requisition.getEmergency());
  }

  public void deleteSiglusDraft(UUID requisitionId) {
    RequisitionDraft draft = draftRepository.findByRequisitionId(requisitionId);
    if (draft != null) {
      draftRepository.delete(draft.getId());
    }
  }

  private List<UUID> findLineItemIds(Requisition requisition) {
    List<RequisitionLineItem> lineItems = requisition.getRequisitionLineItems();
    return lineItems.stream()
        .map(BaseEntity::getId)
        .collect(Collectors.toList());
  }

  private void initiateExpirationDate(List<BaseRequisitionLineItemDto> lineItems, UUID facilityId) {
    Set<UUID> orderableIds = lineItems.stream()
        .map(lineItem -> {
          RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
          return lineItemV2Dto.getOrderable().getId();
        })
        .collect(Collectors.toSet());
    List<OrderableExpirationDateDto> expirationDateDtos = findExpirationDates(orderableIds,
        facilityId);
    lineItems.forEach(lineItem -> {
      RequisitionLineItemV2Dto lineDto = (RequisitionLineItemV2Dto) lineItem;
      setOrderableExpirationDate(expirationDateDtos, lineDto);
    });
  }

  void saveLineItemExtensions(List<BaseRequisitionLineItemDto> lineItems) {
    if (CollectionUtils.isEmpty(lineItems)) {
      return;
    }
    List<UUID> lineItemIds = lineItems.stream().map(BaseDto::getId).collect(toList());
    List<RequisitionLineItemExtension> extensions = lineItemExtensionRepository
        .findLineItems(lineItemIds);
    Map<UUID, RequisitionLineItemExtension> lineItemIdToEntityMap = Maps
        .uniqueIndex(extensions, RequisitionLineItemExtension::getRequisitionLineItemId);
    List<RequisitionLineItemExtension> extensionsToUpdate = newArrayList();
    lineItems.forEach(lineItem -> {
      RequisitionLineItemExtension existingExtension = lineItemIdToEntityMap.get(lineItem.getId());
      if (null == existingExtension) {
        RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
        extension.setAuthorizedQuantity(lineItem.getAuthorizedQuantity());
        extension.setExpirationDate(lineItem.getExpirationDate());
        extension.setSuggestedQuantity(lineItem.getSuggestedQuantity());
        extension.setRequisitionLineItemId(lineItem.getId());
        extensionsToUpdate.add(extension);
      } else {
        existingExtension.setAuthorizedQuantity(lineItem.getAuthorizedQuantity());
        existingExtension.setExpirationDate(lineItem.getExpirationDate());
        existingExtension.setSuggestedQuantity(lineItem.getSuggestedQuantity());
        extensionsToUpdate.add(existingExtension);
      }
    });
    log.info("save line item extensions: {}", extensionsToUpdate);
    lineItemExtensionRepository.save(extensionsToUpdate);
  }

  private List<OrderableExpirationDateDto> findExpirationDates(Set<UUID> orderableIds,
      UUID facilityId) {
    return orderableIds.isEmpty() ? new ArrayList<>() :
        siglusOrderableService.getOrderableExpirationDate(orderableIds, facilityId);
  }

  private List<SiglusRequisitionLineItemDto> buildSiglusLineItem(
      List<RequisitionLineItem> lineItemList, boolean isExternalApprove) {
    Set<VersionEntityReference> references = lineItemList.stream()
        .map(line -> {
          VersionEntityReference reference = line.getFacilityTypeApprovedProduct();
          return new VersionEntityReference(reference.getId(), reference.getVersionNumber());
        }).collect(toSet());
    List<ApprovedProductDto> list = facilityTypeApprovedProductReferenceDataService.findByIdentities(references);
    Map<UUID, ApprovedProductDto> approvedProductDtoMap = list.stream()
        .collect(Collectors.toMap(ApprovedProductDto::getId, dto -> dto));
    return lineItemList
        .stream()
        .map(line -> {
          // The whole object is not required here
          OrderableDto orderable = new OrderableDto();
          orderable.setId(line.getOrderable().getId());
          orderable.setMeta(new MetadataDto(line.getOrderable().getVersionNumber(), null));
          ApprovedProductDto approvedProduct = new ApprovedProductDto(null, null,
              null, null, null, null,
              new MetadataDto(line.getFacilityTypeApprovedProduct().getVersionNumber(), null));
          UUID approvedProductId = line.getFacilityTypeApprovedProduct().getId();
          approvedProduct.setId(approvedProductId);
          RequisitionLineItemV2Dto lineDto = new RequisitionLineItemV2Dto();
          lineDto.setServiceUrl(serviceUrl);
          line.export(lineDto, orderable, approvedProduct);
          if (isExternalApprove) {
            lineDto.setRequestedQuantity(0);
            lineDto.setAuthorizedQuantity(0);
            lineDto.setRequestedQuantityExplanation("0");
            lineDto.setAdditionalQuantityRequired(0);
          }
          SiglusRequisitionLineItemDto siglusRequisitionLineItemDto = new SiglusRequisitionLineItemDto();
          siglusRequisitionLineItemDto.setLineItem(lineDto);
          siglusRequisitionLineItemDto.setApprovedProduct(approvedProductDtoMap.get(approvedProductId));
          return siglusRequisitionLineItemDto;
        })
        .collect(Collectors.toList());
  }

  private void setOrderableExpirationDate(List<OrderableExpirationDateDto> expirationDateDtos,
      RequisitionLineItemV2Dto lineDto) {
    OrderableExpirationDateDto expirationDate = expirationDateDtos
        .stream()
        .filter(expirationDateDto -> expirationDateDto.getOrderableId().equals(lineDto.getOrderable().getId()))
        .findFirst()
        .orElse(null);
    if (null != expirationDate) {
      lineDto.setExpirationDate(expirationDate.getExpirationDate());
    } else {
      lineDto.setExpirationDate(null);
    }
  }

  private void filterKits(RequisitionV2Dto requisition) {
    List<UUID> kitIds = new ArrayList<>();
    US_KITS.forEach(kitCode -> kitIds.add(siglusOrderableService.getOrderableByCode(kitCode).getId()));
    APE_KITS.forEach(kitCode -> kitIds.add(siglusOrderableService.getOrderableByCode(kitCode).getId()));
    requisition.getAvailableProducts().removeIf(product -> kitIds.contains(product.getId()));
    List<Importer> requisitionLineItems = requisition.getRequisitionLineItems();
    requisitionLineItems.removeIf(lineItem -> kitIds.contains(lineItem.getOrderableIdentity().getId()));
    requisition.setRequisitionLineItems(requisitionLineItems.stream().map(this::from).collect(toList()));
  }

  private RequisitionLineItemV2Dto from(Importer lineItem) {
    RequisitionLineItemV2Dto requisitionLineItemV2Dto = new RequisitionLineItemV2Dto();
    BeanUtils.copyProperties(lineItem, requisitionLineItemV2Dto);
    return requisitionLineItemV2Dto;
  }

  private Requisition from(BaseRequisitionDto requisitionDto) {
    Requisition requisition = new Requisition();
    BeanUtils.copyProperties(requisitionDto, requisition);
    return requisition;
  }

  private void filterProductsIfEmergency(RequisitionV2Dto requisition) {
    if (!Boolean.TRUE.equals(requisition.getEmergency())) {
      return;
    }
    List<RequisitionV2Dto> previousEmergencyReqs = getPreviousEmergencyRequisition(requisition);
    if (previousEmergencyReqs.isEmpty()) {
      return;
    }
    filterInProgressProducts(previousEmergencyReqs, requisition);
    filterNotFullyShippedProducts(previousEmergencyReqs, requisition);
  }

  private void filterInProgressProducts(List<RequisitionV2Dto> previousEmergencyReqs,
      RequisitionV2Dto requisition) {
    Set<UUID> productIdsInProgress = filterProductService.getInProgressProducts(previousEmergencyReqs);
    requisition.getAvailableProducts().removeIf(product -> productIdsInProgress.contains(product.getId()));
  }

  private void filterNotFullyShippedProducts(List<RequisitionV2Dto> previousEmergencyReqs,
      RequisitionV2Dto requisition) {
    Set<UUID> productIdsNotFullyShipped = filterProductService.getNotFullyShippedProducts(previousEmergencyReqs);
    requisition.getAvailableProducts().removeIf(product -> productIdsNotFullyShipped.contains(product.getId()));
  }

  private void setAvailableProductsForApprovePage(SiglusRequisitionDto siglusRequisitionDto) {
    UUID requisitionId = siglusRequisitionDto.getId();
    Profiler profiler = requisitionController.getProfiler("GET_REQUISITION_TO_APPROVE", requisitionId);
    Requisition requisition = requisitionController.findRequisition(requisitionId, profiler);
    UserDto userDto = authenticationHelper.getCurrentUser();
    if (requisitionService.validateCanApproveRequisition(requisition, userDto.getId()).isSuccess()) {
      siglusRequisitionDto.setIsExternalApproval(
          !checkIsInternal(siglusRequisitionDto.getFacility().getId(), userDto));
      Set<VersionObjectReferenceDto> availableProducts = siglusRequisitionDto.getAvailableProducts();
      UUID programId = requisition.getProgramId();
      ApproveProductsAggregator aggregator = new ApproveProductsAggregator(
          requisitionService.getApprovedProducts(userDto.getHomeFacilityId(), programId), programId);
      Set<UUID> approverMainProgramAndAdditionalProgramApprovedProducts =
          Optional.ofNullable(aggregator.getApprovedProductReferences())
              .orElse(Collections.emptySet())
              .stream()
              .map(ApprovedProductReference::getOrderable)
              .map(VersionEntityReference::getId)
              .collect(toSet());

      // keep only products in approver facility main & associate programs
      // toggle no/full-supply will update the version
      // version mismatch in VersionObjectReferenceDto is not needed here
      siglusRequisitionDto.setAvailableProducts(availableProducts.stream()
          .filter(product -> approverMainProgramAndAdditionalProgramApprovedProducts.contains(product.getId()))
          .collect(toSet()));
    }
  }

  private RequisitionTemplateExtension setTemplateExtension(RequisitionV2Dto requisitionDto) {
    BasicRequisitionTemplateDto templateDto = requisitionDto.getTemplate();
    RequisitionTemplateExtension extension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisitionDto.getTemplate().getId());
    templateDto.setExtension(RequisitionTemplateExtensionDto.from(extension));
    requisitionDto.setTemplate(templateDto);
    return extension;
  }

  private void setLineItemExtension(RequisitionV2Dto requisitionDto) {
    List<RequisitionLineItem.Importer> lineItems = requisitionDto.getRequisitionLineItems();
    List<UUID> lineItemsId = lineItems.stream()
        .map(Importer::getId)
        .collect(Collectors.toList());
    if (!lineItemsId.isEmpty()) {
      log.info("find line item extension: {}", lineItemsId);
      List<RequisitionLineItemExtension> lineItemExtension = lineItemExtensionRepository.findLineItems(lineItemsId);
      lineItems.forEach(lineItem -> {
        RequisitionLineItemExtension itemExtension = findLineItemExtension(lineItemExtension,
            (RequisitionLineItemV2Dto) lineItem);
        if (itemExtension != null) {
          RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
          if (itemExtension.getSuggestedQuantity() == null) {
            lineItemV2Dto.setSuggestedQuantity(lineItemV2Dto.getRequestedQuantity());
          } else {
            lineItemV2Dto.setSuggestedQuantity(itemExtension.getSuggestedQuantity());
          }
          lineItemV2Dto.setAuthorizedQuantity(itemExtension.getAuthorizedQuantity());
          lineItemV2Dto.setEstimatedQuantity(itemExtension.getEstimatedQuantity());
          lineItemV2Dto.setExpirationDate(itemExtension.getExpirationDate());
        }
      });
    }
  }

  private SiglusRequisitionDto setIsFinalApproval(SiglusRequisitionDto siglusRequisitionDto) {
    if (siglusRequisitionDto.getStatus().duringApproval()) {
      UUID nodeId = siglusRequisitionDto.getSupervisoryNode();
      SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeReferenceDataService.findOne(nodeId);
      if (supervisoryNodeDto != null && supervisoryNodeDto.getParentNode() == null) {
        siglusRequisitionDto.setIsFinalApproval(Boolean.TRUE);
        return siglusRequisitionDto;
      }
    }
    siglusRequisitionDto.setIsFinalApproval(Boolean.FALSE);
    return siglusRequisitionDto;
  }

  private RequisitionLineItemExtension findLineItemExtension(List<RequisitionLineItemExtension> extensions,
      RequisitionLineItemV2Dto lineItem) {
    if (lineItem == null || lineItem.getId() == null) {
      return null;
    }
    return extensions.stream().filter(extension ->
            lineItem.getId().equals(extension.getRequisitionLineItemId()))
        .findFirst().orElse(null);
  }

  public void activateArchivedProducts(UUID requisitionId, UUID facilityId) {
    Set<UUID> orderableIds = findLineItemOrderableIds(requisitionId);
    archiveProductService.activateProducts(facilityId, orderableIds);
  }

  private Set<UUID> findLineItemOrderableIds(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    List<RequisitionLineItem> lineItems = requisition.getRequisitionLineItems();
    return lineItems.stream()
        .map(lineItem -> lineItem.getOrderable().getId())
        .collect(Collectors.toSet());
  }

  private Set<RequisitionStatus> getRequisitionStatusDisplayInRequisitionHistory(UUID facilityId, UUID programId) {
    Requisition requisition = new Requisition();
    requisition.setProgramId(programId);
    requisition.setFacilityId(facilityId);
    Set<RequisitionStatus> canSeeRequisitionStatus = Sets.newHashSet();

    final boolean canAuth = permissionService.canAuthorizeRequisition(requisition).isSuccess();
    if (canAuth) {
      canSeeRequisitionStatus.addAll(
          Arrays.asList(AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER));
      return canSeeRequisitionStatus;
    }
    final boolean canCreate = permissionService.canSubmitRequisition(requisition).isSuccess();
    if (canCreate) {
      canSeeRequisitionStatus
          .addAll(Arrays.asList(SUBMITTED, AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER));
      return canSeeRequisitionStatus;
    }
    return canSeeRequisitionStatus;
  }

  private void fillRequisitionFromDraft(RequisitionDraft draft,
      RequisitionTemplateExtension templateExtension, SiglusRequisitionDto dto) {
    dto.setExtraData(draft.getExtraData().getExtraData());
    dto.setDraftStatusMessage(draft.getDraftStatusMessage());
    if (Boolean.TRUE.equals(templateExtension.getEnableProduct())) {
      dto.setRequisitionLineItems(buildRequisitionLineItems(draft, dto));
    }
    if (Boolean.TRUE.equals(templateExtension.getEnableKitUsage())) {
      dto.setKitUsageLineItems(KitUsageLineItemDraft.from(draft.getKitUsageLineItems()));
    }
    if (Boolean.TRUE.equals(templateExtension.getEnableUsageInformation())) {
      dto.setUsageInformationLineItems(
          UsageInformationLineItemDraft.getLineItemDto(draft.getUsageInformationLineItemDrafts()));
    }
    if (Boolean.TRUE.equals(templateExtension.getEnableRapidTestConsumption())) {
      dto.setTestConsumptionLineItems(
          TestConsumptionLineItemDraft.getLineItemDto(draft.getTestConsumptionLineItemDrafts()));
    }
    if (Boolean.TRUE.equals(templateExtension.getEnablePatientLineItem())) {
      dto.setPatientLineItems(PatientLineItemDraft.getLineItemDto(draft.getPatientLineItemDrafts()));
    }
    if (Boolean.TRUE.equals(templateExtension.getEnableConsultationNumber())) {
      dto.setConsultationNumberLineItems(ConsultationNumberLineItemDraft
          .getLineItemDto(draft.getConsultationNumberLineItemDrafts())
      );
    }

    if (Boolean.TRUE.equals(templateExtension.getEnableRegimen())) {
      regimenDataProcessor.setCustomRegimen(dto);
      dto.setRegimenLineItems(RegimenLineItemDraft.getRegimenLineDtos(
          draft.getRegimenLineItemDrafts(), regimenDataProcessor.getRegimenDtoMap()));
      dto.setRegimenSummaryLineItems(RegimenSummaryLineItemDraft.getRegimenSummaryLineDtos(
          draft.getRegimenSummaryLineItemDrafts()));
    }
  }

  private List<RequisitionLineItemV2Dto> buildRequisitionLineItems(RequisitionDraft draft, SiglusRequisitionDto dto) {
    List<RequisitionLineItemV2Dto> draftLineItems = draft.getLineItems().stream()
        .map(RequisitionLineItemDraft::getLineItemDto)
        .collect(toList());
    Set<UUID> draftLineItemIds = draftLineItems.stream()
        .map(BaseDto::getId)
        .filter(Objects::nonNull)
        .collect(toSet());
    List<RequisitionLineItemV2Dto> lineItems = dto.getLineItems().stream()
        .filter(lineItem -> !draftLineItemIds.contains(lineItem.getId()))
        .map(this::from)
        .collect(toList());
    lineItems.addAll(draftLineItems);
    return lineItems;
  }

  private void initiateRequisitionNumber(SiglusRequisitionDto siglusRequisitionDto) {
    RequisitionExtension requisitionExtension = siglusRequisitionExtensionService.createRequisitionExtension(
        siglusRequisitionDto);
    siglusRequisitionDto.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisitionExtension));
  }

  private void initiateSuggestedQuantity(List<BaseRequisitionLineItemDto> lineItems,
      UUID facilityId, UUID processingPeriodId, UUID programId,
      BasicRequisitionTemplateDto template) {
    Map<String, BasicRequisitionTemplateColumnDto> columnsMap = template.getColumnsMap();
    if (null == columnsMap) {
      return;
    }
    BasicRequisitionTemplateColumnDto column = columnsMap.get(SUGGESTED_QUANTITY_COLUMN);
    if (null == column) {
      return;
    }
    if (Boolean.FALSE.equals(column.getIsDisplayed()) || null == column.getOption()) {
      return;
    }
    if ("cp".equals(column.getOption().getOptionName())) {
      fcCmmCpService.initiateSuggestedQuantityByCp(lineItems, facilityId, processingPeriodId, programId);
    } else if ("cmm".equals(column.getOption().getOptionName())) {
      fcCmmCpService.initiateSuggestedQuantityByCmm(lineItems, facilityId, processingPeriodId);
    }
  }

  @Transactional
  public BasicRequisitionDto createClientRequisition(
      UUID facilityId,
      SiglusRequisitionDto requisitionDto,
      HttpServletRequest request,
      HttpServletResponse response) {
    Requisition existRequisition = siglusRequisitionRepository.findOneByFacilityIdAndProgramIdAndProcessingPeriodId(
        requisitionDto.getFacilityId(),
        requisitionDto.getProgramId(),
        requisitionDto.getProcessingPeriodId());
    if (existRequisition != null) {
      throw new ValidationMessageException("requisition.error.validation.dateModifiedMismatch",
          existRequisition.getId());
    }

    if (RequisitionStatus.INITIATED.equals(requisitionDto.getStatus())) {
      SiglusRequisitionDto initiateRequisition = initiateForClient(requisitionDto.getProgramId(), facilityId,
          requisitionDto.getProcessingPeriodId(), false, null, request, response);
      UUID requisitionId = initiateRequisition.getId();
      requisitionDto.setId(requisitionId);
    }

    SiglusRequisitionDto siglusRequisitionDto = updateRequisition(requisitionDto.getId(), requisitionDto, request,
        response);
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(
        requisitionDto.getId());
    if (requisitionExtension == null) {
      requisitionExtension = new RequisitionExtension();
    }
    activateArchivedProducts(requisitionDto.getId(), facilityId);
    BasicRequisitionDto approveRequisitionDto = buildBasicRequisitionDto(siglusRequisitionDto);
    requisitionExtension.setIsApprovedByInternal(true);
    requisitionExtensionRepository.save(requisitionExtension);
    notificationService.postApprove(approveRequisitionDto);

    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    if (facilityExtension != null && facilityExtension.getIsAndroid()) {
      saveSyncUpHash(siglusRequisitionDto);
    }

    return approveRequisitionDto;
  }

  private void saveSyncUpHash(SiglusRequisitionDto siglusRequisitionDto) {
    org.siglus.siglusapi.dto.UserDto user = authHelper.getCurrentUser();
    String syncUpHash = HashEncoder.hash(
        siglusRequisitionDto.getActualStartDate().toString()
            + siglusRequisitionDto.getActualEndDate()
            + Instant.now()
            + user.getId()
            + user.getHomeFacilityId()
            + programRepository.findOne(siglusRequisitionDto.getProgramId()).getCode()
            + siglusRequisitionDto.getEmergency());
    SyncUpHash syncUpHashDomain = SyncUpHash.builder()
        .hash(syncUpHash)
        .type("Requisition")
        .referenceId(siglusRequisitionDto.getId())
        .build();
    syncUpHashRepository.save(syncUpHashDomain);
  }

  private BasicRequisitionDto buildBasicRequisitionDto(SiglusRequisitionDto siglusRequisitionDto) {
    BasicRequisitionDto basicRequisitionDto = new BasicRequisitionDto();
    basicRequisitionDto.setId(siglusRequisitionDto.getId());
    basicRequisitionDto.setStatus(siglusRequisitionDto.getStatus());
    MinimalFacilityDto minimalFacilityDto = new MinimalFacilityDto();
    minimalFacilityDto.setId(siglusRequisitionDto.getFacilityId());
    basicRequisitionDto.setFacility(minimalFacilityDto);
    BasicProgramDto basicProgramDto = new BasicProgramDto();
    basicProgramDto.setId(siglusRequisitionDto.getProgramId());
    basicRequisitionDto.setProgram(basicProgramDto);
    basicRequisitionDto.setEmergency(siglusRequisitionDto.getEmergency());
    BasicProcessingPeriodDto basicProcessingPeriodDto = new BasicProcessingPeriodDto();
    basicProcessingPeriodDto.setId(siglusRequisitionDto.getProcessingPeriodId());
    basicRequisitionDto.setProcessingPeriod(basicProcessingPeriodDto);
    basicRequisitionDto.setStatusChanges(siglusRequisitionDto.getStatusChanges());
    return basicRequisitionDto;
  }
}
