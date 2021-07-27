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

import static com.google.common.collect.Lists.newArrayList;
import static org.openlmis.requisition.web.ResourceNames.PROCESSING_PERIODS;
import static org.openlmis.requisition.web.ResourceNames.PROGRAMS;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ExtraDataConstants.APPROVE;
import static org.siglus.common.constant.ExtraDataConstants.AUTHORIZE;
import static org.siglus.common.constant.ExtraDataConstants.CLIENT_SUBMITTED_TIME;
import static org.siglus.common.constant.ExtraDataConstants.IS_SAVED;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;
import static org.siglus.common.constant.ExtraDataConstants.SUBMIT;
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
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_ARVT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DM_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_PATIENTS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_PROPHYLAXY_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TOTAL_COLUMN;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_COMMUNITY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_PATIENT;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem.Importer;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
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
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.ExtraDataSignatureDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.android.androidenum.NewSection0;
import org.siglus.siglusapi.dto.android.androidenum.NewSection1;
import org.siglus.siglusapi.dto.android.androidenum.NewSection2;
import org.siglus.siglusapi.dto.android.androidenum.NewSection3;
import org.siglus.siglusapi.dto.android.androidenum.NewSection4;
import org.siglus.siglusapi.dto.android.androidenum.PatientLineItemName;
import org.siglus.siglusapi.dto.android.androidenum.PatientType;
import org.siglus.siglusapi.dto.android.androidenum.RegimenSummaryCode;
import org.siglus.siglusapi.dto.android.request.AndroidTemplateConfig;
import org.siglus.siglusapi.dto.android.request.PatientLineItemColumnRequest;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.RegimenLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.ConsultationNumberDataProcessor;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.PatientLineItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class AndroidRequisitionService {

  private final SiglusAuthenticationHelper authHelper;
  private final RequisitionService requisitionService;
  private final RequisitionTemplateService requisitionTemplateService;
  private final SiglusProgramService siglusProgramService;
  private final SiglusOrderableService siglusOrderableService;
  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  private final SupervisoryNodeReferenceDataService supervisoryNodeService;
  private final SiglusUsageReportService siglusUsageReportService;
  private final PermissionService permissionService;
  private final SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;
  private final RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  private final RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;
  private final RequisitionRepository requisitionRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final ConsultationNumberDataProcessor consultationNumberDataProcessor;
  private final AndroidTemplateConfig androidTemplateConfig;
  private final RegimenLineItemRepository regimenLineItemRepository;
  private final RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;
  private final RegimenRepository regimenRepository;
  private final PatientLineItemRepository patientLineItemRepository;
  private final PatientLineItemMapper patientLineItemMapper;

  @Transactional
  public void create(RequisitionCreateRequest request) {
    UserDto user = authHelper.getCurrentUser();
    UUID authorId = user.getId();
    UUID programId = siglusProgramService.getProgramIdByCode(request.getProgramCode());
    Requisition requisition = initiateRequisition(request, user.getHomeFacilityId(), programId, authorId,
        request.getProgramCode());
    requisition = submitRequisition(requisition, authorId);
    requisition = authorizeRequisition(requisition, authorId);
    internalApproveRequisition(requisition, authorId);
  }

  public RequisitionResponse getRequisitionResponseByFacilityIdAndDate(UUID facilityId, String startDate,
      Map<UUID, String> orderableIdToCode) {
    List<RequisitionExtension> requisitionExtensions = requisitionExtensionRepository
        .searchRequisitionIdByFacilityAndDate(facilityId, startDate);
    Set<UUID> requisitionIds = requisitionExtensions.stream().map(RequisitionExtension::getRequisitionId)
        .collect(Collectors.toSet());
    Map<UUID, List<RegimenLineItemRequest>> idToRegimenLines = buildIdToRegimenLineRequestsMap(requisitionIds);
    Map<UUID, List<RegimenLineItemRequest>> idToRegimenSummaryLines = buildIdToRegimenSummaryLineRequestsMap(
        requisitionIds);
    Map<UUID, List<PatientLineItemsRequest>> idToPatientLines = buildIdToPatientLineRequestsMap(requisitionIds);
    List<RequisitionCreateRequest> requisitionCreateRequests = new ArrayList<>();

    requisitionExtensions.forEach(
        extension -> {
          RequisitionV2Dto requisitionV2Dto = siglusRequisitionRequisitionService
              .searchRequisition(extension.getRequisitionId());
          if (!isAndroidTemplate(requisitionV2Dto.getTemplate().getId())
              || !requisitionV2Dto.getStatus().isAuthorized()) {
            return;
          }
          RequisitionCreateRequest requisitionCreateRequest = RequisitionCreateRequest.builder()
              .programCode(getProgramCode(requisitionV2Dto.getProgram().getId()))
              .emergency(requisitionV2Dto.getEmergency())
              .consultationNumber(getConsultationNumber(requisitionV2Dto.getId()))
              .products(getProducts(requisitionV2Dto, orderableIdToCode))
              .regimenLineItems(getRegimenLineItems(requisitionV2Dto, idToRegimenLines))
              .regimenSummaryLineItems(getRegimenSummaryLineItems(requisitionV2Dto, idToRegimenSummaryLines))
              .patientLineItems(getPatientLineItems(requisitionV2Dto, idToPatientLines))
              .comments(requisitionV2Dto.getDraftStatusMessage())
              .build();
          setTimeAndSignature(requisitionCreateRequest, requisitionV2Dto);
          requisitionCreateRequests.add(requisitionCreateRequest);
        }
    );
    return RequisitionResponse.builder().requisitionResponseList(requisitionCreateRequests).build();
  }

  private List<RegimenLineItemRequest> getRegimenLineItems(RequisitionV2Dto requisitionV2Dto,
      Map<UUID, List<RegimenLineItemRequest>> idToRegimenLineRequests) {
    if (idToRegimenLineRequests.get(requisitionV2Dto.getId()) == null) {
      return Collections.emptyList();
    }
    return idToRegimenLineRequests.get(requisitionV2Dto.getId());
  }

  private List<RegimenLineItemRequest> getRegimenSummaryLineItems(RequisitionV2Dto requisitionV2Dto,
      Map<UUID, List<RegimenLineItemRequest>> idToRegimenSummaryLineRequests) {
    if (idToRegimenSummaryLineRequests.get(requisitionV2Dto.getId()) == null) {
      return Collections.emptyList();
    }
    return idToRegimenSummaryLineRequests.get(requisitionV2Dto.getId());
  }

  private List<PatientLineItemsRequest> getPatientLineItems(RequisitionV2Dto requisitionV2Dto,
      Map<UUID, List<PatientLineItemsRequest>> idToPatientLineRequests) {
    if (idToPatientLineRequests.get(requisitionV2Dto.getId()) == null) {
      return Collections.emptyList();
    }
    return idToPatientLineRequests.get(requisitionV2Dto.getId());
  }

  private boolean isAndroidTemplate(UUID programTemplatedId) {
    Set<UUID> androidTemplateSet = androidTemplateConfig.getAndroidTemplateIds();
    return androidTemplateSet.contains(programTemplatedId);
  }

  private Map<UUID, List<RegimenLineItemRequest>> buildIdToRegimenLineRequestsMap(Set<UUID> requisitionIds) {
    List<RegimenLineItem> regimenLineItems = regimenLineItemRepository.findByRequisitionIdIn(requisitionIds);
    Set<UUID> regimenIds = regimenLineItems.stream().map(RegimenLineItem::getRegimenId).filter(Objects::nonNull)
        .collect(Collectors.toSet());
    List<Regimen> regimens = regimenRepository.findByIdIn(regimenIds);
    Map<UUID, RegimenDto> idToRegimenDto = regimens.stream().map(RegimenDto::from)
        .collect(Collectors.toMap(RegimenDto::getId, Function.identity()));
    Map<UUID, List<RegimenLineItem>> idToRegimenLineItem = regimenLineItems.stream()
        .collect(Collectors.groupingBy(RegimenLineItem::getRequisitionId));
    return idToRegimenLineItem.entrySet().stream()
        .collect(Collectors
            .toMap(Map.Entry::getKey, entry -> RegimenLineItemRequest.from(entry.getValue(), idToRegimenDto)));
  }

  private Map<UUID, List<RegimenLineItemRequest>> buildIdToRegimenSummaryLineRequestsMap(
      Set<UUID> requisitionIds) {
    List<RegimenSummaryLineItem> regimenSummaryLineItems = regimenSummaryLineItemRepository
        .findByRequisitionIdIn(requisitionIds);
    Map<UUID, List<RegimenSummaryLineItem>> idToRegimenSummaryLineItem = regimenSummaryLineItems.stream()
        .collect(Collectors.groupingBy(RegimenSummaryLineItem::getRequisitionId));
    return idToRegimenSummaryLineItem.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> RegimenLineItemRequest.from(entry.getValue())));
  }

  private Map<UUID, List<PatientLineItemsRequest>> buildIdToPatientLineRequestsMap(Set<UUID> requisitionIds) {
    List<PatientLineItem> patientLineItems = patientLineItemRepository.findByRequisitionIdIn(requisitionIds);
    Map<UUID, List<PatientLineItem>> idToPatientLines = patientLineItems.stream()
        .collect(Collectors.groupingBy(PatientLineItem::getRequisitionId));
    return idToPatientLines.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> buildPatientLineItemRequestList(entry.getValue())));
  }

  private List<PatientLineItemsRequest> buildPatientLineItemRequestList(List<PatientLineItem> patientLineItems) {
    List<PatientGroupDto> patientGroupDtos = patientLineItemMapper.from(patientLineItems);
    List<PatientLineItemsRequest> list = patientGroupDtos.stream()
        .filter(t -> !StringUtils.isEmpty(PatientLineItemName.findKeyByValue(t.getName())))
        .map(this::buildPatientLineItemRequest)
        .collect(Collectors.toList());
    return reBuildPatientLineList(list);
  }

  private PatientLineItemsRequest buildPatientLineItemRequest(PatientGroupDto patientGroupDto) {
    List<PatientLineItemColumnRequest> columns = newArrayList();
    String name = PatientLineItemName.findKeyByValue(patientGroupDto.getName());
    Map<String, PatientColumnDto> map = patientGroupDto.getColumns();
    map.forEach((colName, patientColumnDto) -> {
      if (FieldConstants.TOTAL.equals(colName) && !TABLE_PROPHYLAXY_KEY.equals(name)) {
        return;
      }
      columns.add(PatientLineItemColumnRequest.builder()
          .tableName(name)
          .name(getRealColumnName(name, colName))
          .value(patientColumnDto.getValue())
          .build());
    });
    return PatientLineItemsRequest.builder()
        .name(name)
        .columns(columns)
        .build();
  }

  private List<PatientLineItemsRequest> reBuildPatientLineList(List<PatientLineItemsRequest> patientLineItemList) {
    List<PatientLineItemsRequest> despenseList = patientLineItemList.stream()
        .filter(t -> t.getName().contains(TABLE_DISPENSED))
        .collect(Collectors.toList());
    List<PatientLineItemsRequest> arvtList = patientLineItemList.stream()
        .filter(t -> !t.getName().contains(TABLE_DISPENSED)).collect(Collectors.toList());
    arvtList.add(buildDespensePatientLine(despenseList));
    return arvtList;
  }

  private PatientLineItemsRequest buildDespensePatientLine(List<PatientLineItemsRequest> despenseList) {
    List<PatientLineItemColumnRequest> dispensedColumns = despenseList.stream()
        .flatMap(m -> m.getColumns().stream())
        .map(n -> PatientLineItemColumnRequest.builder()
            .name(n.getName())
            .tableName(TABLE_DISPENSED_KEY)
            .value(n.getValue())
            .build())
        .collect(Collectors.toList());
    return PatientLineItemsRequest.builder().name(TABLE_DISPENSED_KEY).columns(dispensedColumns).build();
  }

  private String getRealColumnName(String tableName, String columnName) {
    if (tableName.equals(TABLE_ARVT_KEY)) {
      return PatientType.findKeyByValue(columnName);
    } else if (tableName.equals(TABLE_PATIENTS_KEY)) {
      return NewSection0.findKeyByValue(columnName);
    } else if (tableName.equals(TABLE_PROPHYLAXY_KEY)) {
      return NewSection1.findKeyByValue(columnName);
    } else if (tableName.equals(TABLE_DISPENSED_DS_KEY)) {
      return NewSection2.findKeyByValue(columnName);
    } else if (tableName.equals(TABLE_DISPENSED_DT_KEY)) {
      return NewSection3.findKeyByValue(columnName);
    } else if (tableName.equals(TABLE_DISPENSED_DM_KEY)) {
      return NewSection4.findKeyByValue(columnName);
    } else {
      return "";
    }
  }

  private Integer getConsultationNumber(UUID requisitionId) {
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setId(requisitionId);
    consultationNumberDataProcessor.get(siglusRequisitionDto);
    List<ConsultationNumberGroupDto> consultationNumberGroupDtos = siglusRequisitionDto
        .getConsultationNumberLineItems();
    if (consultationNumberGroupDtos == null) {
      return null;
    }
    Map<String, Map<String, ConsultationNumberColumnDto>> mapColumnDtoByName = consultationNumberGroupDtos.stream()
        .collect(Collectors.toMap(ConsultationNumberGroupDto::getName, ConsultationNumberGroupDto::getColumns));
    Map<String, ConsultationNumberColumnDto> columnDtoMap = mapColumnDtoByName.get(GROUP_NAME);
    if (columnDtoMap == null || columnDtoMap.get(COLUMN_NAME) == null) {
      return null;
    }
    return columnDtoMap.get(COLUMN_NAME).getValue();
  }

  private String getProgramCode(UUID programId) {
    String programCode = null;
    ProgramDto programDto = siglusProgramService.getProgram(programId);
    if (programDto != null) {
      programCode = programDto.getCode();
    }
    return programCode;
  }

  private void setTimeAndSignature(RequisitionCreateRequest requisitionCreateRequest,
      RequisitionV2Dto requisitionV2Dto) {
    Map<String, Object> extraData = requisitionV2Dto.getExtraData();
    List<RequisitionSignatureRequest> signatures = new ArrayList<>();
    if (extraData.get(SIGNATURE) != null) {
      ObjectMapper objectMapper = new ObjectMapper();
      ExtraDataSignatureDto signatureDto = objectMapper
          .convertValue(extraData.get(SIGNATURE), ExtraDataSignatureDto.class);
      if (signatureDto.getSubmit() != null) {
        signatures.add(RequisitionSignatureRequest.builder().type(SUBMIT).name(signatureDto.getSubmit()).build());
      }
      if (signatureDto.getAuthorize() != null) {
        signatures.add(RequisitionSignatureRequest.builder().type(AUTHORIZE).name(signatureDto.getAuthorize()).build());
      }
      String[] approves = signatureDto.getApprove();
      if (approves != null && approves.length > 0) {
        signatures.add(RequisitionSignatureRequest.builder().type(APPROVE).name(approves[0]).build());
      }
      requisitionCreateRequest.setSignatures(signatures);
    }
    if (extraData.get(CLIENT_SUBMITTED_TIME) != null) {
      requisitionCreateRequest
          .setClientSubmittedTime(Instant.parse(String.valueOf(extraData.get(CLIENT_SUBMITTED_TIME))));
    }

    if (extraData.get(ACTUAL_START_DATE) != null) {
      requisitionCreateRequest.setActualStartDate(LocalDate.parse(String.valueOf(extraData.get(ACTUAL_START_DATE))));
    }

    if (extraData.get(ACTUAL_END_DATE) != null) {
      requisitionCreateRequest.setActualEndDate(LocalDate.parse(String.valueOf(extraData.get(ACTUAL_END_DATE))));
    }
  }

  private List<RequisitionLineItemRequest> getProducts(RequisitionV2Dto requisitionDto,
      Map<UUID, String> orderableIdToCode) {
    List<RequisitionLineItem.Importer> lineItems = requisitionDto.getRequisitionLineItems();
    if (lineItems.isEmpty()) {
      return Collections.emptyList();
    }
    List<RequisitionLineItemRequest> requisitionLineItemRequestList = new ArrayList<>();
    List<UUID> lineItemIdList = lineItems.stream()
        .map(Importer::getId)
        .collect(Collectors.toList());
    Map<UUID, RequisitionLineItemExtension> requisitionLineItemExtensionMap =
        requisitionLineItemExtensionRepository.findLineItems(lineItemIdList).stream()
            .collect(Collectors.toMap(RequisitionLineItemExtension::getRequisitionLineItemId, Function.identity(),
                (key1, key2) -> key2));
    lineItems.forEach(lineItem -> {
      RequisitionLineItemExtension itemExtension = requisitionLineItemExtensionMap.get(lineItem.getId());
      RequisitionLineItemRequest lineItemRequest = RequisitionLineItemRequest.builder()
          .beginningBalance(lineItem.getBeginningBalance())
          .totalReceivedQuantity(lineItem.getTotalReceivedQuantity())
          .totalConsumedQuantity(lineItem.getTotalConsumedQuantity())
          .stockOnHand(lineItem.getStockOnHand())
          .requestedQuantity(lineItem.getRequestedQuantity())
          .authorizedQuantity(itemExtension == null ? null : itemExtension.getAuthorizedQuantity())
          .productCode(lineItem.getOrderableIdentity() == null ? null
              : orderableIdToCode.get(lineItem.getOrderableIdentity().getId()))
          .expirationDate(itemExtension.getExpirationDate())
          .totalLossesAndAdjustments(lineItem.getTotalLossesAndAdjustments())
          .build();
      requisitionLineItemRequestList.add(lineItemRequest);

    });
    return requisitionLineItemRequestList;
  }

  private Requisition initiateRequisition(RequisitionCreateRequest request, UUID homeFacilityId, UUID programId,
      UUID authorId, String programCode) {
    checkPermission(() -> permissionService.canInitRequisition(programId, homeFacilityId));
    Requisition newRequisition = RequisitionBuilder.newRequisition(homeFacilityId, programId, request.getEmergency());
    newRequisition.setTemplate(getRequisitionTemplate(programCode));
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newRequisition.setProcessingPeriodId(getPeriodId(request));
    newRequisition.setReportOnly(false);
    newRequisition.setNumberOfMonthsInPeriod(1);
    newRequisition.setDraftStatusMessage(request.getComments());
    buildStatusChanges(newRequisition, authorId);
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
        .findTemplateById(getAndroidTemplateId(programCode));
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisitionTemplate.getId());
    requisitionTemplate.setTemplateExtension(templateExtension);
    return requisitionTemplate;
  }

  private UUID getAndroidTemplateId(String programCode) {
    if ("VC".equals(programCode)) {
      return androidTemplateConfig.getAndroidViaTemplateId();
    } else if ("T".equals(programCode)) {
      return androidTemplateConfig.getAndroidMmiaTemplateId();
    } else if ("ML".equals(programCode)) {
      return androidTemplateConfig.getAndroidMalariaTemplateId();
    } else {
      return null;
    }
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
        .getApproveProduct(homeFacilityId, programId, false);
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
    SiglusRequisitionDto requisitionDto = siglusUsageReportService.initiateUsageReport(dto);
    buildConsultationNumber(requisitionDto, request);
    buildRequisitionKitUsage(requisitionDto, request);
    updateRegimenLineItems(requisitionDto, programId, request);
    updateRegimenSummaryLineItems(requisitionDto, request);
    updatePatientLineItems(requisitionDto, request);
    siglusUsageReportService.saveUsageReport(requisitionDto, dto);
  }

  private void updatePatientLineItems(SiglusRequisitionDto requisitionDto, RequisitionCreateRequest request) {
    if (requisitionDto.getPatientLineItems() == null || requisitionDto.getPatientLineItems().isEmpty()) {
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
    caculatePatientDispensedTotal(patientNameToPatientGroupDto);
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

  private void caculatePatientDispensedTotal(Map<String, PatientGroupDto> patientNameToPatientGroupDto) {
    PatientGroupDto patientGroupDtoSection5 = patientNameToPatientGroupDto.get(NEW_SECTION_5);
    PatientColumnDto section5TotalDto = patientGroupDtoSection5.getColumns().get(TOTAL_COLUMN);
    if (section5TotalDto.getValue() == null) {
      section5TotalDto.setValue(0);
    }
    caculatePatientDispensedTotalBySection(patientNameToPatientGroupDto.get(NEW_SECTION_2), patientGroupDtoSection5,
        NEW_SECTION_2);
    caculatePatientDispensedTotalBySection(patientNameToPatientGroupDto.get(NEW_SECTION_3), patientGroupDtoSection5,
        NEW_SECTION_3);
    caculatePatientDispensedTotalBySection(patientNameToPatientGroupDto.get(NEW_SECTION_4), patientGroupDtoSection5,
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

  private void caculatePatientDispensedTotalBySection(PatientGroupDto patientGroupDtoSection,
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
    if (requisitionDto.getRegimenSummaryLineItems() == null || requisitionDto.getRegimenSummaryLineItems().isEmpty()) {
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
    if (requisitionDto.getRegimenLineItems() == null || requisitionDto.getRegimenLineItems().isEmpty()) {
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
}

