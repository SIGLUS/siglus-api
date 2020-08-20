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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.emptyCollection;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.IN_APPROVAL;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED_WITHOUT_ORDER;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.dto.OrderStatus.SHIPPED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_ID_MISMATCH;
import static org.openlmis.requisition.service.notification.NotificationChannelDto.EMAIL;
import static org.siglus.siglusapi.constant.PaginationConstants.UNPAGED;
import static org.siglus.siglusapi.i18n.SimamMessageKeys.REQUISITION_EMAIL_CONTENT_PRE;
import static org.siglus.siglusapi.i18n.SimamMessageKeys.REQUISITION_EMAIL_SUBJECT;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.dto.ShipmentDto;
import org.openlmis.requisition.dto.ShipmentLineItemDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.ProofOfDeliveryService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ShipmentFulfillmentService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.ApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.IdealStockAmountReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.RequisitionGroupReferenceDataService;
import org.openlmis.requisition.service.referencedata.RightReferenceDataService;
import org.openlmis.requisition.service.referencedata.RoleReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisingUsersReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.requisition.service.stockmanagement.StockOnHandRetrieverBuilderFactory;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.openlmis.requisition.web.QueryRequisitionSearchParams;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.requisition.web.RequisitionV2Controller;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.exception.NotFoundException;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.SimulateAuthenticationHelper;
import org.siglus.siglusapi.domain.ConsultationNumberLineItemDraft;
import org.siglus.siglusapi.domain.KitUsageLineItemDraft;
import org.siglus.siglusapi.domain.PatientLineItemDraft;
import org.siglus.siglusapi.domain.RegimenLineItemDraft;
import org.siglus.siglusapi.domain.RegimenSummaryLineItemDraft;
import org.siglus.siglusapi.domain.RequisitionDraft;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemDraft;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.TestConsumptionLineItemDraft;
import org.siglus.siglusapi.domain.UsageInformationLineItemDraft;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.dto.simam.EmailAttachmentDto;
import org.siglus.siglusapi.dto.simam.MessageSimamDto;
import org.siglus.siglusapi.dto.simam.NotificationSimamDto;
import org.siglus.siglusapi.i18n.MessageService;
import org.siglus.siglusapi.repository.RequisitionDraftRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusNotificationNotificationService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusRequisitionService {

  public static final String SIMAM = "simam";

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
  private PeriodService periodService;

  @Autowired
  private StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Autowired
  private StockOnHandRetrieverBuilderFactory stockOnHandRetrieverBuilderFactory;

  @Autowired
  private ProofOfDeliveryService proofOfDeliveryService;

  @Autowired
  private IdealStockAmountReferenceDataService idealStockAmountReferenceDataService;

  @Autowired
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Autowired
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private FacilityTypeApprovedProductReferenceDataService
      facilityTypeApprovedProductReferenceDataService;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  RequisitionAuthenticationHelper authenticationHelper;

  @Autowired
  ApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Autowired
  private SimulateAuthenticationHelper simulateAuthenticationHelper;

  @Autowired
  private SiglusUsageReportService siglusUsageReportService;

  @Autowired
  private OrderFulfillmentService orderFulfillmentService;

  @Autowired
  private ShipmentFulfillmentService shipmentFulfillmentService;

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
  private SupervisingUsersReferenceDataService supervisingUsersReferenceDataService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private RequisitionSimamEmailService requisitionSimamEmailService;

  @Autowired
  private MessageService messageService;

  @Autowired
  private SiglusNotificationNotificationService siglusNotificationNotificationService;

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
  private SiglusApprovedProductReferenceDataService siglusApprovedReferenceDataService;

  @Value("${service.url}")
  private String serviceUrl;

  @Transactional
  public SiglusRequisitionDto updateRequisition(UUID requisitionId,
      SiglusRequisitionDto requisitionDto, HttpServletRequest request,
      HttpServletResponse response) {
    if (null != requisitionDto.getId()
        && !Objects.equals(requisitionDto.getId(), requisitionId)) {
      throw new ValidationMessageException(ERROR_ID_MISMATCH);
    }
    if (operatePermissionService.canSubmit(requisitionDto)) {
      return saveRequisitionWithoutValidation(requisitionId, requisitionDto, request, response);
    } else {
      return saveRequisitionDraft(requisitionDto);
    }
  }

  @Transactional
  public SiglusRequisitionDto initiate(UUID programId, UUID facilityId,
      UUID suggestedPeriod,
      boolean emergency,
      String physicalInventoryDateStr,
      HttpServletRequest request, HttpServletResponse response) {
    RequisitionV2Dto v2Dto = requisitionV2Controller
        .initiate(programId, facilityId, suggestedPeriod, emergency,
            physicalInventoryDateStr, request, response);
    SiglusRequisitionDto siglusRequisitionDto = siglusUsageReportService.initiateUsageReport(v2Dto);
    initiateRequisitionNumber(siglusRequisitionDto);
    return siglusRequisitionDto;
  }

  @Transactional
  public List<SiglusRequisitionLineItemDto> createRequisitionLineItem(
      UUID requisitionId,
      List<UUID> orderableIds) {

    Profiler profiler = requisitionController
        .getProfiler("ADD_NEW_REQUISITION_LINE_ITEM_FOR_SPEC_ORDERABLE");

    Requisition existedRequisition = requisitionController.findRequisition(requisitionId, profiler);

    for (UUID orderableId : orderableIds) {
      boolean alreadyHaveCurrentOrderable = existedRequisition.getRequisitionLineItems().stream()
          .anyMatch(
              requisitionLineItem -> requisitionLineItem.getOrderable().getId().equals(orderableId)
          );
      if (alreadyHaveCurrentOrderable) {
        throw new ValidationMessageException(
            new Message(MessageKeys.ERROR_ORDERABLE_ALREADY_IN_GIVEN_REQUISITION));
      }
    }

    UUID programId = existedRequisition.getProgramId();
    ProgramDto program = requisitionController.findProgram(programId, profiler);

    UUID facilityId = existedRequisition.getFacilityId();
    FacilityDto facility = requisitionController.findFacility(facilityId, profiler);

    permissionService.canInitOrAuthorizeRequisition(programId, facilityId);

    UserDto userDto = authenticationHelper.getCurrentUser();
    FacilityDto userFacility = requisitionController.findFacility(
        userDto.getHomeFacilityId(), profiler);

    List<RequisitionLineItem> lineItemList = constructLineItem(
        existedRequisition, program, facility, orderableIds, userFacility);

    boolean isApprove = requisitionService
        .validateCanApproveRequisition(existedRequisition, userDto.getId()).isSuccess();
    boolean isInternalFacility = userDto.getHomeFacilityId()
        .equals(existedRequisition.getFacilityId());
    boolean isExternalApprove = isApprove && !isInternalFacility;

    return buildSiglusLineItem(lineItemList, isExternalApprove);
  }

  public SiglusRequisitionDto searchRequisition(UUID requisitionId) {
    // call origin OpenLMIS API
    // reason: 1. set template extension
    //         1. 2. set line item authorized quality extension
    RequisitionV2Dto requisitionDto =
        siglusRequisitionRequisitionService.searchRequisition(requisitionId);
    setLineItemExtension(requisitionDto);
    RequisitionTemplateExtension extension = setTemplateExtension(requisitionDto);

    filterProductsIfEmergency(requisitionDto);
    SiglusRequisitionDto siglusRequisitionDto = getSiglusRequisitionDto(requisitionId,
        extension, requisitionDto);
    // set available products in approve page
    setAvailableProductsForApprovePage(siglusRequisitionDto);
    siglusRequisitionDto.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId));
    return setIsFinalApproval(siglusRequisitionDto);
  }

  @Transactional
  public BasicRequisitionDto submitRequisition(UUID requisitionId, HttpServletRequest request,
      HttpServletResponse response) {
    saveRequisitionWithValidation(requisitionId, request, response);
    BasicRequisitionDto basicRequisitionDto = requisitionController
        .submitRequisition(requisitionId, request, response);
    notificationService.postSubmit(basicRequisitionDto);
    activateArchivedProducts(requisitionId, basicRequisitionDto.getFacility().getId());
    return basicRequisitionDto;
  }

  @Transactional
  public BasicRequisitionDto authorizeRequisition(UUID requisitionId, HttpServletRequest request,
      HttpServletResponse response) {
    SiglusRequisitionDto siglusRequisitionDto = saveRequisitionWithValidation(requisitionId,
        request, response);
    BasicRequisitionDto basicRequisitionDto = requisitionController
        .authorizeRequisition(requisitionId, request, response);
    notificationService.postAuthorize(basicRequisitionDto);
    UUID facilityId = basicRequisitionDto.getFacility().getId();
    activateArchivedProducts(requisitionId, facilityId);
    UUID programId = basicRequisitionDto.getProgram().getId();
    notifySimamWhenAuthorize(siglusRequisitionDto, facilityId, programId);
    return basicRequisitionDto;
  }

  private void notifySimamWhenAuthorize(SiglusRequisitionDto siglusRequisitionDto, UUID facilityId,
      UUID programId) {
    SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeReferenceDataService.findSupervisoryNode(
        programId, facilityId);
    Collection<UserDto> approvers = getApprovers(supervisoryNodeDto.getId(), programId);
    if (approvers.stream().noneMatch(approver -> facilityId.equals(approver.getHomeFacilityId()))) {
      notifySimam(siglusRequisitionDto, approvers);
    }
  }

  @Transactional
  public BasicRequisitionDto approveRequisition(UUID requisitionId, HttpServletRequest request,
      HttpServletResponse response) {
    SiglusRequisitionDto siglusRequisitionDto = saveRequisitionWithValidation(requisitionId,
        request, response);
    BasicRequisitionDto basicRequisitionDto = requisitionController
        .approveRequisition(requisitionId, request, response);
    notificationService.postApprove(basicRequisitionDto);
    UUID facilityId = basicRequisitionDto.getFacility().getId();
    UUID programId = basicRequisitionDto.getProgram().getId();
    if (checkIsInternal(facilityId, authenticationHelper.getCurrentUser())) {
      activateArchivedProducts(requisitionId, facilityId);
      notifySimamWhenApprove(siglusRequisitionDto, facilityId, programId);
    }
    return basicRequisitionDto;
  }

  private void notifySimamWhenApprove(SiglusRequisitionDto siglusRequisitionDto, UUID facilityId,
      UUID programId) {
    SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeReferenceDataService
        .findSupervisoryNode(programId, facilityId);
    Collection<UserDto> approvers = getApprovers(supervisoryNodeDto.getParentNodeId(), programId);
    notifySimam(siglusRequisitionDto, approvers);
  }

  private Collection<UserDto> getApprovers(UUID supervisoryNodeId, UUID programId) {
    if (supervisoryNodeId == null) {
      return emptyCollection();
    }
    RightDto right = rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE);
    return supervisingUsersReferenceDataService.findAll(supervisoryNodeId, right.getId(),
        programId);
  }

  private void notifySimam(SiglusRequisitionDto siglusRequisitionDto,
      Collection<UserDto> approvers) {
    log.info("start to send email to simam of the requisition id {}", siglusRequisitionDto.getId());
    ProgramDto program = programReferenceDataService
        .findOne(siglusRequisitionDto.getProgram().getId());
    setLineItemExtension(siglusRequisitionDto);
    siglusUsageReportService.setUsageTemplateDto(siglusRequisitionDto.getTemplate().getId(),
        siglusRequisitionDto);
    List<EmailAttachmentDto> emailAttachments = requisitionSimamEmailService
        .prepareEmailAttachmentsForSimam(siglusRequisitionDto, program);
    String subject = messageService
        .localize(new org.siglus.common.util.Message(REQUISITION_EMAIL_SUBJECT,
            siglusRequisitionDto.getId().toString())).getMessage();
    String emailContent = messageService
        .localize(new org.siglus.common.util.Message(REQUISITION_EMAIL_CONTENT_PRE
            + program.getCode().toLowerCase())).getMessage();
    approvers.forEach(approve -> {
      log.info("send simam email to user {}", approve.getUsername());
      Map<String, MessageSimamDto> messages = new HashMap<>();
      messages.put(EMAIL.toString(), new MessageSimamDto(
          subject, emailContent, SIMAM, emailAttachments));
      siglusNotificationNotificationService
          .sendNotification(new NotificationSimamDto(approve.getId(), messages));
    });
  }

  private SiglusRequisitionDto getSiglusRequisitionDto(UUID requisitionId,
      RequisitionTemplateExtension extension, RequisitionV2Dto requisitionDto) {
    SiglusRequisitionDto siglusRequisitionDto;
    if (operatePermissionService.isEditable(requisitionDto)) {
      UserDto user = authenticationHelper.getCurrentUser();
      RequisitionDraft draft = draftRepository
          .findRequisitionDraftByRequisitionIdAndFacilityId(requisitionId,
              user.getHomeFacilityId());
      if (draft != null) {
        siglusRequisitionDto = SiglusRequisitionDto.from(requisitionDto);
        siglusUsageReportService.setUsageTemplateDto(requisitionDto.getTemplate().getId(),
            siglusRequisitionDto);
        fillRequisitionDraft(draft, extension, siglusRequisitionDto);
        return siglusRequisitionDto;
      }
    }
    return siglusUsageReportService.searchUsageReport(requisitionDto);
  }

  public Page<BasicRequisitionDto> searchRequisitions(MultiValueMap<String, String> queryParams,
      Pageable pageable) {
    Set<RequisitionStatus> requisitionStatusDisplayInRequisitionHistory =
        getRequisitionStatusDisplayInRequisitionHistory(
            UUID.fromString(queryParams.getFirst(QueryRequisitionSearchParams.FACILITY)),
            UUID.fromString(queryParams.getFirst(QueryRequisitionSearchParams.PROGRAM)));
    requisitionStatusDisplayInRequisitionHistory.forEach(requisitionStatus -> queryParams
        .add(QueryRequisitionSearchParams.REQUISITION_STATUS, requisitionStatus.toString()));
    RequisitionSearchParams params = new QueryRequisitionSearchParams(queryParams);
    return siglusRequisitionRequisitionService.searchRequisitions(params, pageable);
  }

  public List<FacilityDto> searchFacilitiesForApproval() {
    RightDto right = rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE);
    List<RoleDto> roleDtos = roleReferenceDataService.search(right.getId());
    Set<UUID> roleIds = roleDtos.stream().map(RoleDto::getId).collect(toSet());
    UserDto userDto = authenticationHelper.getCurrentUser();

    // id : supervisory node
    Map<UUID, SupervisoryNodeDto> nodeDtoMap =
        supervisoryNodeReferenceDataService.findAllSupervisoryNodes()
            .stream()
            .collect(toMap(SupervisoryNodeDto::getId, node -> node));

    // id : requisition group
    Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap =
        requisitionGroupReferenceDataService.findAll()
            .stream()
            .collect(toMap(RequisitionGroupDto::getId, groupDto -> groupDto));

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
            getAllFacilityDtos(homeId, supervisoryNodeDto, requisitionGroupDtoMap,
                facilityDtoMap, nodeDtoMap).stream())
        .collect(toSet());
    return convertToList(facilityDtos, facilityDtoMap.get(homeId));
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

  private List<FacilityDto> sortFacility(Set<FacilityDto> set) {
    List<FacilityDto> list = new ArrayList<>(set);
    list.sort((FacilityDto f1, FacilityDto f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
    return list;
  }

  // check if internal only && not final approve
  private boolean isInternalApproveOnly(UUID userHomeFacilityId,
      SupervisoryNodeDto supervisoryNodeDto,
      Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap) {
    UUID requisitionGroupId = supervisoryNodeDto.getRequisitionGroupId();
    if (requisitionGroupId == null) {
      return false;
    }
    Set<UUID> idsToApprove = requisitionGroupDtoMap.get(requisitionGroupId)
        .getMemberFacilities()
        .stream()
        .map(FacilityDto::getId)
        .collect(toSet());

    return idsToApprove.contains(userHomeFacilityId) && supervisoryNodeDto.getParentNode() != null;
  }

  // get all facilities of this supervisoryNode
  private Set<FacilityDto> getAllFacilityDtos(UUID userHomeFacilityId,
      SupervisoryNodeDto supervisoryNodeDto,
      Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap,
      Map<UUID, FacilityDto> facilityDtoMap,
      Map<UUID, SupervisoryNodeDto> nodeDtoMap) {

    if (isInternalApproveOnly(userHomeFacilityId, supervisoryNodeDto,
        requisitionGroupDtoMap)) {
      return Sets.newHashSet(facilityDtoMap.get(userHomeFacilityId));
    }

    return getFacilityDtosByOwnAndChildSupervisoryNode(supervisoryNodeDto,
        requisitionGroupDtoMap, nodeDtoMap);

  }

  // get facilities to approve of its own node
  private Set<FacilityDto> getFacilityDtosBySupervisoryNode(
      SupervisoryNodeDto supervisoryNodeDto,
      Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap) {
    if (supervisoryNodeDto.getRequisitionGroupId() != null) {
      return requisitionGroupDtoMap.get(supervisoryNodeDto.getRequisitionGroupId())
          .getMemberFacilities();
    }
    return Collections.emptySet();
  }

  // get facilities to approve of its own and all child supervisory node
  private Set<FacilityDto> getFacilityDtosByOwnAndChildSupervisoryNode(
      SupervisoryNodeDto supervisoryNodeDto,
      Map<UUID, RequisitionGroupDto> requisitionGroupDtoMap,
      Map<UUID, SupervisoryNodeDto> nodeDtoMap) {
    if (supervisoryNodeDto.getChildNodes().isEmpty()) {
      return getFacilityDtosBySupervisoryNode(supervisoryNodeDto, requisitionGroupDtoMap);
    } else {
      return supervisoryNodeDto.getChildNodes()
          .stream()
          .flatMap(child -> {
            // needs optimize
            SupervisoryNodeDto childNode = nodeDtoMap.get(child.getId());
            Set<FacilityDto> childSet = getFacilityDtosByOwnAndChildSupervisoryNode(childNode,
                requisitionGroupDtoMap, nodeDtoMap);
            Set<FacilityDto> allSet =
                getFacilityDtosBySupervisoryNode(supervisoryNodeDto, requisitionGroupDtoMap);

            return mergeSets(allSet, childSet).stream();
          }).collect(toSet());
    }
  }

  private Set<FacilityDto> mergeSets(Set<FacilityDto> set1, Set<FacilityDto> set2) {
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
    RequisitionTemplate template = requisitionRepository.findOne(requisitionDto.getId())
        .getTemplate();
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(template.getId());
    template.setTemplateExtension(templateExtension);
    RequisitionDraft requisitionDraft = RequisitionDraft
        .from(requisitionDto, template, (draft == null ? null : draft.getId()), user);
    log.info("save requisition draft extension: {}", requisitionDraft);
    draft = draftRepository.save(requisitionDraft);

    fillRequisitionDraft(draft, template.getTemplateExtension(), requisitionDto);
    return requisitionDto;
  }

  private SiglusRequisitionDto saveRequisitionWithoutValidation(UUID requisitionId,
      SiglusRequisitionDto requisitionDto, HttpServletRequest request,
      HttpServletResponse response) {
    return saveRequisition(requisitionId, requisitionDto, request, response, false);
  }

  private SiglusRequisitionDto saveRequisitionWithValidation(UUID requisitionId,
      HttpServletRequest request, HttpServletResponse response) {
    return saveRequisition(requisitionId, null, request, response, true);
  }

  private SiglusRequisitionDto saveRequisition(UUID requisitionId,
      SiglusRequisitionDto requisitionDto, HttpServletRequest request,
      HttpServletResponse response, boolean validate) {
    // call modify OpenLMIS API
    RequisitionDraft draft = null;
    if (requisitionDto == null) {
      RequisitionV2Dto dto =
          siglusRequisitionRequisitionService.searchRequisition(requisitionId);
      requisitionDto = SiglusRequisitionDto.from(dto);
      draft = draftRepository.findByRequisitionId(requisitionDto.getId());
      if (draft != null) {
        RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
            .findByRequisitionTemplateId(dto.getTemplate().getId());
        fillRequisitionDraft(draft, templateExtension, requisitionDto);
      } else {
        setLineItemExtension(requisitionDto);
        RequisitionTemplateExtension extension = setTemplateExtension(requisitionDto);
        requisitionDto = getSiglusRequisitionDto(requisitionId, extension, requisitionDto);
      }
    }
    filterApprovedQualityForPreAuthorize(requisitionDto);
    RequisitionV2Dto updateRequisitionDto = requisitionV2Controller
        .updateRequisition(requisitionId, requisitionDto, request, response);
    if (draft != null) {
      draftRepository.delete(draft.getId());
    }

    saveLineItemExtension(requisitionDto, updateRequisitionDto);
    if (validate) {
      return siglusUsageReportService
          .saveUsageReportWithValidation(requisitionDto, updateRequisitionDto);
    }
    return siglusUsageReportService.saveUsageReport(requisitionDto, updateRequisitionDto);
  }

  private void filterApprovedQualityForPreAuthorize(SiglusRequisitionDto requisitionDto) {
    if (requisitionDto.getStatus().isPreAuthorize()) {
      List<RequisitionLineItem.Importer> lineItems = requisitionDto.getRequisitionLineItems();
      lineItems.forEach(lineItem -> {
        RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
        lineItemV2Dto.setApprovedQuantity(null);
      });
    }
  }

  private void saveLineItemExtension(RequisitionV2Dto toUpdatedDto, RequisitionV2Dto updatedDto) {
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
        RequisitionLineItemExtension requisitionLineItemExtension =
            findLineItemExtension(extensions, dto);
        if (requisitionLineItemExtension != null) {
          requisitionLineItemExtension.setAuthorizedQuantity(dto.getAuthorizedQuantity());
          updateExtension.add(requisitionLineItemExtension);
        } else if (dto != null && dto.getAuthorizedQuantity() != null) {
          RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
          extension.setRequisitionLineItemId(lineItem.getId());
          extension.setAuthorizedQuantity(dto.getAuthorizedQuantity());
          updateExtension.add(extension);
        }
      });
      log.info("lineItem Extension Repository {}", updateExtension);
      lineItemExtensionRepository.save(updateExtension);
    }
  }

  private RequisitionLineItemV2Dto findDto(RequisitionLineItem.Importer lineItem,
      RequisitionV2Dto dto) {
    for (RequisitionLineItem.Importer lineItemV2Dto : dto.getRequisitionLineItems()) {
      if (lineItemV2Dto.getOrderableIdentity().getId()
          .equals(lineItem.getOrderableIdentity().getId())) {
        return (RequisitionLineItemV2Dto) lineItemV2Dto;
      }
    }
    return null;
  }

  private List<RequisitionLineItem> constructLineItem(Requisition requisition, ProgramDto program,
      FacilityDto facility, List<UUID> orderableIds, FacilityDto userFacility) {
    RequisitionTemplate requisitionTemplate = requisition.getTemplate();
    Integer numberOfPreviousPeriodsToAverage = decrementOrZero(requisitionTemplate
        .getNumberOfPeriodsToAverage());
    List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos = null;
    List<StockCardRangeSummaryDto> stockCardRangeSummariesToAverage = null;
    List<ProcessingPeriodDto> periods = null;
    List<Requisition> previousRequisitions = requisition.getPreviousRequisitions();

    ProcessingPeriodDto period = periodService.getPeriod(requisition.getProcessingPeriodId());
    List<ApprovedProductDto> approvedProducts = siglusApprovedReferenceDataService
        .getApprovedProducts(
            userFacility.getId(), program.getId(), orderableIds,
            period.isReportOnly() && Boolean.FALSE.equals(requisition.getEmergency()));
    if (requisitionTemplate.isPopulateStockOnHandFromStockCards() && requisition.getEmergency()) {
      stockCardRangeSummaryDtos = getStockCardRangeSummaryDtos(facility, program.getId(),
          approvedProducts, requisition.getActualStartDate(), requisition.getActualEndDate());

      LocalDate startDateForCalculateAvg;
      LocalDate endDateForCalculateAvg = requisition.getActualEndDate();
      if (!CollectionUtils.isEmpty(previousRequisitions)) {
        Set<UUID> periodIds = previousRequisitions.stream()
            .map(Requisition::getProcessingPeriodId)
            .collect(toSet());
        periods = periodService.getPeriods(periodIds);
        periods.add(period);

        startDateForCalculateAvg = previousRequisitions.stream()
            .min(Comparator.comparing(Requisition::getActualStartDate))
            .orElseThrow(() -> new NotFoundException("Earlier Rquisition Not Found"))
            .getActualStartDate();
        if (Boolean.TRUE.equals(requisition.getEmergency())) {
          List<Requisition> requisitions =
              requisitionService.searchAfterAuthorizedRequisitions(requisition.getFacilityId(),
                  requisition.getProgramId(),
                  period.getId(), false);
          endDateForCalculateAvg = requisitions.get(0).getActualEndDate();
        }
      } else {
        startDateForCalculateAvg = period.getStartDate();
        periods = Lists.newArrayList(period);
      }

      stockCardRangeSummariesToAverage = getStockCardRangeSummaryDtos(facility, program.getId(),
          approvedProducts, startDateForCalculateAvg, endDateForCalculateAvg);

    } else if (numberOfPreviousPeriodsToAverage > previousRequisitions.size()) {
      numberOfPreviousPeriodsToAverage = previousRequisitions.size();
    }

    OAuth2Authentication originAuth = simulateAuthenticationHelper.simulateCrossServiceAuth();
    Map<UUID, Integer> orderableSoh = getOrderableSohMap(requisitionTemplate,
        facility.getId(), requisition.getActualEndDate(), RequisitionLineItem.STOCK_ON_HAND,
        program.getId(), approvedProducts);
    Map<UUID, Integer> orderableBeginning = getOrderableSohMap(requisitionTemplate,
        facility.getId(), requisition.getActualStartDate().minusDays(1),
        RequisitionLineItem.BEGINNING_BALANCE, program.getId(), approvedProducts);
    simulateAuthenticationHelper.recoveryAuth(originAuth);

    ProofOfDeliveryDto pod = null;
    if (!isEmpty(previousRequisitions)) {
      pod = proofOfDeliveryService.get(previousRequisitions.get(0));
    }

    final Map<UUID, Integer> idealStockAmounts = idealStockAmountReferenceDataService
        .search(requisition.getFacilityId(), requisition.getProcessingPeriodId())
        .stream()
        .collect(toMap(isa -> isa.getCommodityType().getId(), IdealStockAmountDto::getAmount));

    List<RequisitionLineItem> lineItemList = new ArrayList<>();
    for (ApprovedProductDto approvedProductDto : approvedProducts) {
      UUID orderableId = approvedProductDto.getOrderable().getId();
      Integer stockOnHand = orderableSoh.get(orderableId);
      Integer beginningBalances = orderableBeginning.get(orderableId);

      lineItemList.add(requisition.constructLineItem(requisitionTemplate, stockOnHand,
          beginningBalances, approvedProductDto, numberOfPreviousPeriodsToAverage,
          idealStockAmounts, stockCardRangeSummaryDtos, stockCardRangeSummariesToAverage,
          periods, pod, approvedProducts));
    }
    return lineItemList;
  }

  @Transactional
  public void deleteRequisition(UUID requisitionId) {
    deleteExtensionForRequisition(requisitionId);
    deleteSiglusDraft(requisitionId);
    siglusUsageReportService.deleteUsageReport(requisitionId);
    siglusRequisitionExtensionService.deleteRequisitionExtension(requisitionId);
    siglusRequisitionRequisitionService.deleteRequisition(requisitionId);
    notificationService.postDelete(requisitionId);
  }

  private void deleteExtensionForRequisition(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    List<UUID> ids = findLineItemIds(requisition);
    List<RequisitionLineItemExtension> extensions = ids.isEmpty() ? new ArrayList<>() :
        lineItemExtensionRepository.findLineItems(ids);
    if (!extensions.isEmpty()) {
      log.info("delete line item extension: {}", extensions);
      lineItemExtensionRepository.delete(extensions);
    }
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

  private List<StockCardRangeSummaryDto> getStockCardRangeSummaryDtos(FacilityDto facility,
      UUID programId, List<ApprovedProductDto> approveProduct, LocalDate startDate,
      LocalDate endDate) {
    Set<VersionIdentityDto> orderableIdentities = approveProduct
        .stream()
        .map(product -> product.getIdentity())
        .collect(Collectors.toSet());
    return stockCardRangeSummaryStockManagementService
        .search(programId, facility.getId(),
            orderableIdentities, null,
            startDate, endDate);
  }

  private Map<UUID, Integer> getOrderableSohMap(RequisitionTemplate requisitionTemplate,
      UUID facilityId, LocalDate date, String columnName, UUID programId,
      List<ApprovedProductDto> approveProduct) {
    return stockOnHandRetrieverBuilderFactory
        .getInstance(requisitionTemplate, columnName)
        .forProgram(programId)
        .forFacility(facilityId)
        .forProducts(new ApproveProductsAggregator(approveProduct,
            programId))
        .asOfDate(date)
        .build()
        .get();
  }

  private Integer decrementOrZero(Integer numberOfPreviousPeriodsToAverage) {
    // numberOfPeriodsToAverage is always >= 2 or null
    if (numberOfPreviousPeriodsToAverage == null) {
      numberOfPreviousPeriodsToAverage = 0;
    } else {
      numberOfPreviousPeriodsToAverage--;
    }
    return numberOfPreviousPeriodsToAverage;
  }

  private List<OrderableExpirationDateDto> findOrderableIds(
      List<RequisitionLineItem> requisitionLineItems) {
    Set<UUID> orderableIds = requisitionLineItems
        .stream()
        .map(item -> item.getOrderable().getId())
        .collect(toSet());
    return orderableIds.isEmpty() ? new ArrayList<>() :
        siglusOrderableService.getOrderableExpirationDate(orderableIds);
  }

  private List<SiglusRequisitionLineItemDto> buildSiglusLineItem(
      List<RequisitionLineItem> lineItemList, boolean isExternalApprove) {
    List<OrderableExpirationDateDto> expirationDateDtos = findOrderableIds(lineItemList);

    Set<VersionEntityReference> references = lineItemList.stream()
        .map(line -> {
          VersionEntityReference reference = line.getFacilityTypeApprovedProduct();
          return new VersionEntityReference(reference.getId(), reference.getVersionNumber());
        }).collect(toSet());

    List<ApprovedProductDto> list = facilityTypeApprovedProductReferenceDataService
        .findByIdentities(references);

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
              new MetadataDto(line.getFacilityTypeApprovedProduct().getVersionNumber(),
                  null));
          UUID approvedProductId = line.getFacilityTypeApprovedProduct().getId();
          approvedProduct.setId(approvedProductId);

          RequisitionLineItemV2Dto lineDto = new RequisitionLineItemV2Dto();
          lineDto.setServiceUrl(serviceUrl);
          line.export(lineDto, orderable, approvedProduct);
          setOrderableExpirationDate(expirationDateDtos, orderable, lineDto);
          if (isExternalApprove) {
            lineDto.setRequestedQuantity(0);
            lineDto.setAuthorizedQuantity(0);
            lineDto.setRequestedQuantityExplanation("0");
            lineDto.setAdditionalQuantityRequired(0);
          }

          SiglusRequisitionLineItemDto siglusRequisitionLineItemDto =
              new SiglusRequisitionLineItemDto();
          siglusRequisitionLineItemDto.setLineItem(lineDto);
          siglusRequisitionLineItemDto
              .setApprovedProduct(approvedProductDtoMap.get(approvedProductId));

          return siglusRequisitionLineItemDto;
        })
        .collect(Collectors.toList());
  }

  private void setOrderableExpirationDate(List<OrderableExpirationDateDto> expirationDateDtos,
      OrderableDto orderable, RequisitionLineItemV2Dto lineDto) {
    OrderableExpirationDateDto expirationDate = expirationDateDtos
        .stream()
        .filter(expirationDateDto ->
            expirationDateDto.getOrderableId().equals(orderable.getId()))
        .findFirst()
        .orElse(null);
    if (null != expirationDate) {
      lineDto.setExpirationDate(expirationDate.getExpirationDate());
    } else {
      lineDto.setExpirationDate(null);
    }
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

  private List<RequisitionV2Dto> getPreviousEmergencyRequisition(BaseRequisitionDto requisition) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.set(QueryRequisitionSearchParams.EMERGENCY, Boolean.TRUE.toString());
    String periodId = requisition.getProcessingPeriod().getId().toString();
    queryParams.set(QueryRequisitionSearchParams.PROCESSING_PERIOD, periodId);
    String facilityId = requisition.getFacility().getId().toString();
    queryParams.set(QueryRequisitionSearchParams.FACILITY, facilityId);
    return siglusRequisitionRequisitionService
        .searchRequisitions(new QueryRequisitionSearchParams(queryParams), UNPAGED)
        .getContent().stream()
        .filter(req -> !req.getId().equals(requisition.getId()))
        .map(BaseDto::getId)
        .map(siglusRequisitionRequisitionService::searchRequisition)
        .collect(toList());
  }

  private void filterInProgressProducts(List<RequisitionV2Dto> previousEmergencyReqs,
      RequisitionV2Dto requisition) {
    Set<UUID> productIdsInProgress = previousEmergencyReqs.stream()
        .filter(req -> req.getStatus().isInProgress())
        .map(RequisitionV2Dto::getLineItems)
        .flatMap(Collection::stream)
        .map(lineItem -> lineItem.getOrderableIdentity().getId())
        .collect(toSet());
    filterProductsInRequisition(requisition, productIdsInProgress);
  }

  private void filterNotFullyShippedProducts(List<RequisitionV2Dto> previousEmergencyReqs,
      RequisitionV2Dto requisition) {
    Set<UUID> productIdsNotFullyShipped = previousEmergencyReqs.stream()
        .filter(req -> req.getStatus() == RELEASED)
        .map(this::mapToNotFullyShippedProductIds)
        .flatMap(Collection::stream)
        .collect(toSet());
    filterProductsInRequisition(requisition, productIdsNotFullyShipped);
  }

  private void filterProductsInRequisition(RequisitionV2Dto requisition,
      Set<UUID> productIdsToBeFiltered) {
    requisition.getAvailableProducts()
        .removeIf(product -> productIdsToBeFiltered.contains(product.getId()));
  }

  private Set<UUID> mapToNotFullyShippedProductIds(RequisitionV2Dto requisition) {
    Map<UUID, Long> reqProductQuantityMap = requisition.getLineItems().stream()
        .filter(lineItem -> !lineItem.getSkipped() && lineItem.getApprovedQuantity() > 0)
        .collect(groupingBy(lineItem -> lineItem.getOrderableIdentity().getId(),
            reducing(0L, req -> req.getApprovedQuantity().longValue(), Long::sum)));
    Map<UUID, Long> shipmentProductQuantityMap = searchOrders(requisition).stream()
        .filter(order -> order.getStatus() == SHIPPED)
        .map(BaseDto::getId)
        .map(shipmentFulfillmentService::getShipments)
        .flatMap(Collection::stream)
        .map(ShipmentDto::getLineItems)
        .flatMap(Collection::stream)
        .collect(groupingBy(lineItem -> lineItem.getOrderable().getId(),
            reducing(0L, ShipmentLineItemDto::getQuantityShipped, Long::sum)));
    return reqProductQuantityMap.entrySet().stream()
        .filter(entry -> !shipmentProductQuantityMap.containsKey(entry.getKey())
            || entry.getValue() > shipmentProductQuantityMap.get(entry.getKey()))
        .map(Entry::getKey)
        .collect(Collectors.toSet());
  }

  private List<OrderDto> searchOrders(BaseRequisitionDto requisition) {
    return orderFulfillmentService
        .search(requisition.getSupplyingFacility(), requisition.getFacilityId(),
            requisition.getProgramId(), requisition.getProcessingPeriodId(), null/*ignore status*/)
        .stream()
        .filter(order -> requisition.getId().equals(order.getExternalId()))
        .collect(Collectors.toList());
  }

  private void setAvailableProductsForApprovePage(SiglusRequisitionDto siglusRequisitionDto) {
    UUID requisitionId = siglusRequisitionDto.getId();
    Profiler profiler = requisitionController
        .getProfiler("GET_REQUISITION_TO_APPROVE", requisitionId);
    Requisition requisition = requisitionController
        .findRequisition(requisitionId, profiler);
    UserDto userDto = authenticationHelper.getCurrentUser();
    if (requisitionService
        .validateCanApproveRequisition(requisition, userDto.getId()).isSuccess()) {

      siglusRequisitionDto.setIsExternalApproval(
          !checkIsInternal(siglusRequisitionDto.getFacility().getId(), userDto));

      Set<VersionObjectReferenceDto> availableProducts =
          siglusRequisitionDto.getAvailableProducts();

      Set<UUID> approverMainProgramAndAdditionalProgramApprovedProducts
          = Optional
          .ofNullable(requisitionService
              .getApproveProduct(userDto.getHomeFacilityId(), requisition.getProgramId(),
                  siglusRequisitionDto.getReportOnly())
              .getApprovedProductReferences())
          .orElse(Collections.emptySet())
          .stream()
          .map(ApprovedProductReference::getOrderable)
          .map(VersionEntityReference::getId)
          .collect(toSet());

      // keep only products in approver facility main & associate programs
      // toggle no/full-supply will update the version
      // version mismatch in VersionObjectReferenceDto is not needed here
      siglusRequisitionDto.setAvailableProducts(availableProducts.stream()
          .filter(product ->
              approverMainProgramAndAdditionalProgramApprovedProducts.contains(product.getId()))
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
      List<RequisitionLineItemExtension> lineItemExtension =
          lineItemExtensionRepository.findLineItems(lineItemsId);
      lineItems.forEach(lineItem -> {
        RequisitionLineItemExtension itemExtension = findLineItemExtension(lineItemExtension,
            (RequisitionLineItemV2Dto) lineItem);
        if (itemExtension != null) {
          RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
          lineItemV2Dto.setAuthorizedQuantity(itemExtension.getAuthorizedQuantity());
        }
      });
    }
  }

  private SiglusRequisitionDto setIsFinalApproval(SiglusRequisitionDto siglusRequisitionDto) {
    if (siglusRequisitionDto.getStatus().duringApproval()) {
      UUID nodeId = siglusRequisitionDto.getSupervisoryNode();
      SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeService
          .findOne(nodeId);

      if (supervisoryNodeDto != null && supervisoryNodeDto.getParentNode() == null) {
        siglusRequisitionDto.setIsFinalApproval(Boolean.TRUE);
        return siglusRequisitionDto;
      }
    }

    siglusRequisitionDto.setIsFinalApproval(Boolean.FALSE);
    return siglusRequisitionDto;
  }

  private RequisitionLineItemExtension findLineItemExtension(
      List<RequisitionLineItemExtension> extensions,
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
    archiveProductService.activateArchivedProducts(orderableIds, facilityId);
  }

  private Set<UUID> findLineItemOrderableIds(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    List<RequisitionLineItem> lineItems = requisition.getRequisitionLineItems();
    return lineItems.stream()
        .map(lineItem -> lineItem.getOrderable().getId())
        .collect(Collectors.toSet());
  }

  private Set<RequisitionStatus> getRequisitionStatusDisplayInRequisitionHistory(UUID facilityId,
      UUID programId) {
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
      canSeeRequisitionStatus.addAll(Arrays
          .asList(SUBMITTED, AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER));
      return canSeeRequisitionStatus;
    }

    return canSeeRequisitionStatus;
  }

  private void fillRequisitionDraft(RequisitionDraft draft,
      RequisitionTemplateExtension templateExtension, SiglusRequisitionDto dto) {
    dto.setExtraData(draft.getExtraData().getExtraData());
    dto.setDraftStatusMessage(draft.getDraftStatusMessage());
    if (Boolean.TRUE.equals(templateExtension.getEnableProduct())) {
      dto.setRequisitionLineItems(draft.getLineItems()
          .stream()
          .map(RequisitionLineItemDraft::getLineItemDto)
          .collect(toList()));
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
          TestConsumptionLineItemDraft.getLineItemDto(draft.getTestConsumptionLineItemDrafts())
      );
    }
    if (Boolean.TRUE.equals(templateExtension.getEnablePatientLineItem())) {
      dto.setPatientLineItems(PatientLineItemDraft.getLineItemDto(draft.getPatientLineItemDrafts())
      );
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
      dto.setRegimenDispatchLineItems(RegimenSummaryLineItemDraft.getRegimenSummaryLineDtos(
          draft.getRegimenSummaryLineItemDrafts(),
          regimenDataProcessor.getRegimenDispatchLineDtoMap()));
    }

  }

  private void initiateRequisitionNumber(SiglusRequisitionDto siglusRequisitionDto) {
    RequisitionExtension requisitionExtension = siglusRequisitionExtensionService
        .createRequisitionExtension(siglusRequisitionDto.getId(),
            siglusRequisitionDto.getEmergency(), siglusRequisitionDto.getFacilityId());
    siglusRequisitionDto.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisitionExtension));
  }

}
