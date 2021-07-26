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
import static org.siglus.common.constant.KitConstants.CHW_KIT_CODE;
import static org.siglus.common.constant.KitConstants.HF_KIT_CODE;
import static org.siglus.siglusapi.constant.AndroidConstants.SCHEDULE_CODE;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.COLUMN_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.GROUP_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.COLLECTION_KIT_OPENED;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.COLLECTION_KIT_RECEIVED;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.SERVICE_CHW;
import static org.siglus.siglusapi.constant.UsageSectionConstants.KitUsageLineItems.SERVICE_HF;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_COLUMN_4;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_0;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_1;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_2;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_3;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.NEW_SECTION_4;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.PATIENT_TYPE;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TOTAL;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_COMMUNITY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_PATIENT;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.android.request.PatientLineItemColumnRequest;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.RegimenLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RegimenSummaryLineItemRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class AndroidRequisitionService {

  @Value("${android.via.templateId}")
  private UUID androidViaTemplateId;

  @Value("${android.mmia.templateId}")
  private UUID androidMmiaTemplateId;

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
  private final RegimenLineItemRepository regimenLineItemRepository;
  private final RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;
  private final RegimenRepository regimenRepository;
  private final PatientLineItemRepository patientLineItemRepository;
  private final PatientLineItemMapper patientLineItemMapper;

  @RequiredArgsConstructor
  @Getter
  public enum RegimenSummaryCode {
    key_regime_3lines_1("1stLinhas"),
    key_regime_3lines_2("newColumn0"),
    key_regime_3lines_3("newColumn1");
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum PatientLineItemName {
    table_arvt_key(PATIENT_TYPE),
    table_patients_key(NEW_SECTION_0),
    table_prophylaxy_key(NEW_SECTION_1),
    table_dispensed_ds_key(NEW_SECTION_2),
    table_dispensed_dt_key(NEW_SECTION_3),
    table_dispensed_dm_key(NEW_SECTION_4);
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum PatientType {
    table_trav_label_new_key(NEW_COLUMN),
    table_trav_label_maintenance_key(NEW_COLUMN_0),
    table_trav_label_transit_key(NEW_COLUMN_1),
    table_trav_label_transfers_key(NEW_COLUMN_2),
    table_trav_label_alteration_key(NEW_COLUMN_3);
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum NewSection0 {
    table_patients_adults_key(NEW_COLUMN),
    table_patients_0to4_key(NEW_COLUMN_0),
    table_patients_5to9_key(NEW_COLUMN_1),
    table_patients_10to14_key(NEW_COLUMN_2);
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum NewSection1 {
    table_prophylaxis_ppe_key(NEW_COLUMN),
    table_prophylaxis_prep_key(NEW_COLUMN_0),
    table_prophylaxis_child_key(NEW_COLUMN_1),
    table_prophylaxis_value_key(TOTAL);
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum NewSection2 {
    dispensed_ds5(NEW_COLUMN),
    dispensed_ds4(NEW_COLUMN_0),
    dispensed_ds3(NEW_COLUMN_1),
    dispensed_ds2(NEW_COLUMN_2),
    dispensed_ds1(NEW_COLUMN_3),
    dispensed_ds(NEW_COLUMN_4);
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum NewSection3 {
    dispensed_dt2(NEW_COLUMN),
    dispensed_dt1(NEW_COLUMN_0),
    dispensed_dt(NEW_COLUMN_1);
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum NewSection4 {
    dispensed_dm(NEW_COLUMN);
    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @Transactional
  public void create(RequisitionCreateRequest request) {
    UserDto user = authHelper.getCurrentUser();
    UUID authorId = user.getId();
    UUID programId = siglusProgramService.getProgramIdByCode(request.getProgramCode());
    Requisition requisition = initiateRequisition(request, user.getHomeFacilityId(), programId, authorId);
    requisition = submitRequisition(requisition, authorId);
    requisition = authorizeRequisition(requisition, authorId);
    internalApproveRequisition(requisition, authorId);
  }

  public RequisitionResponse getRequisitionResponseByFacilityIdAndDate(UUID facilityId, String startDate,
      Map<UUID, String> orderableIdToCode) {
    List<RequisitionExtension> requisitionExtensions = requisitionExtensionRepository
        .searchRequisitionIdByFacilityAndDate(facilityId, startDate);

    Set<UUID> requisitionIdSet = requisitionExtensions.stream().map(RequisitionExtension::getRequisitionId)
        .collect(Collectors.toSet());

    Map<UUID, List<RegimenLineItemRequest>> idToRegimenLines = buildIdToRegimenLineRequestsMap(requisitionIdSet);
    Map<UUID, List<RegimenSummaryLineItemRequest>> idToRegimenSummaryLines = buildIdToRegimenSummaryLineRequestsMap(
        requisitionIdSet);
    Map<UUID, List<PatientLineItemsRequest>> idToPatientLines = buildIdToPatientLineRequestsMap(requisitionIdSet);

    List<RequisitionCreateRequest> requisitionCreateRequests = new ArrayList<>();

    requisitionExtensions.forEach(
        extension -> {
          RequisitionV2Dto requisitionV2Dto = siglusRequisitionRequisitionService
              .searchRequisition(extension.getRequisitionId());
          if (!isAndroidTemplate(requisitionV2Dto.getTemplate().getId())
              || requisitionV2Dto.getStatus().equals(RequisitionStatus.INITIATED)) {
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

  private List<RegimenSummaryLineItemRequest> getRegimenSummaryLineItems(RequisitionV2Dto requisitionV2Dto,
      Map<UUID, List<RegimenSummaryLineItemRequest>> idToRegimenSummaryLineRequests) {
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
    Set<UUID> androidTemplateSet = Stream.of(androidViaTemplateId, androidMmiaTemplateId)
        .collect(Collectors.toCollection(HashSet::new));
    return androidTemplateSet.contains(programTemplatedId);
  }

  private Map<UUID, List<RegimenLineItemRequest>> buildIdToRegimenLineRequestsMap(Set<UUID> requisitionIdSet) {
    List<RegimenLineItem> regimenLineItems = regimenLineItemRepository.findByRequisitionIdIn(requisitionIdSet);
    Set<UUID> regimenIds = regimenLineItems.stream()
        .map(RegimenLineItem::getRegimenId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    List<Regimen> regimens = regimenRepository.findByIdIn(regimenIds);

    Map<UUID, RegimenDto> idToRegimenDto = regimens.stream()
        .map(RegimenDto::from)
        .collect(Collectors.toMap(RegimenDto::getId, Function.identity()));

    Map<UUID, List<RegimenLineItem>> idToRegimenLineItem = regimenLineItems.stream()
        .collect(Collectors.groupingBy(RegimenLineItem::getRequisitionId));

    Map<UUID, List<RegimenLineItemRequest>> idToRegimenLineRequest = new HashMap<>();

    idToRegimenLineItem.forEach((requisitionId, lineItems) -> {
      List<RegimenLineDto> regimenLineDtos = RegimenLineDto.from(lineItems, idToRegimenDto);
      List<RegimenLineItemRequest> regimenLineItemRequests = regimenLineDtos.stream()
          .filter(regimenLineDto -> StringUtils.isEmpty(regimenLineDto.getName()))
          .map(regimenLineDto -> RegimenLineItemRequest
              .builder()
              .name(regimenLineDto.getRegimen().getFullProductName())
              .code(regimenLineDto.getRegimen().getCode())
              .comunitaryPharmacy(regimenLineDto.getColumns().get(COLUMN_NAME_COMMUNITY).getValue())
              .patientsOnTreatment(regimenLineDto.getColumns().get(COLUMN_NAME_PATIENT).getValue())
              .build())
          .collect(Collectors.toList());
      idToRegimenLineRequest.put(requisitionId, regimenLineItemRequests);
    });
    return idToRegimenLineRequest;
  }

  private Map<UUID, List<RegimenSummaryLineItemRequest>> buildIdToRegimenSummaryLineRequestsMap(
      Set<UUID> requisitionIdSet) {
    List<RegimenSummaryLineItem> regimenSummaryLineItems = regimenSummaryLineItemRepository
        .findByRequisitionIds(requisitionIdSet);
    Map<UUID, List<RegimenSummaryLineItem>> idToRegimenSummaryLineItem = regimenSummaryLineItems.stream()
        .collect(Collectors.groupingBy(RegimenSummaryLineItem::getRequisitionId));
    Map<UUID, List<RegimenSummaryLineItemRequest>> idToRegimenSummaryLineRequests = new HashMap<>();

    idToRegimenSummaryLineItem.forEach((requisitionId, lineItems) -> {
      List<RegimenSummaryLineDto> regimenSummaryLineDtos = RegimenSummaryLineDto.from(lineItems);
      List<RegimenSummaryLineItemRequest> regimenLineItemRequests = regimenSummaryLineDtos.stream()
          .filter(regimenSummaryLineDto -> !regimenSummaryLineDto.getName().equals(FieldConstants.TOTAL))
          .map(
              regimenSummaryLineDto -> RegimenSummaryLineItemRequest.builder()
                  .code(RegimenSummaryCode.findByValue(regimenSummaryLineDto.getName()))
                  .comunitaryPharmacy(regimenSummaryLineDto.getColumns().get(COLUMN_NAME_COMMUNITY).getValue())
                  .patientsOnTreatment(regimenSummaryLineDto.getColumns().get(COLUMN_NAME_PATIENT).getValue())
                  .build()
          ).collect(Collectors.toList());
      idToRegimenSummaryLineRequests.put(requisitionId, regimenLineItemRequests);
    });
    return idToRegimenSummaryLineRequests;
  }

  private Map<UUID, List<PatientLineItemsRequest>> buildIdToPatientLineRequestsMap(Set<UUID> requisitionIdSet) {
    List<PatientLineItem> patientLineItems = patientLineItemRepository.findByRequisitionIdIn(requisitionIdSet);
    Map<UUID, List<PatientLineItem>> idToPatientLines = patientLineItems.stream()
        .collect(Collectors.groupingBy(PatientLineItem::getRequisitionId));
    Map<UUID, List<PatientLineItemsRequest>> idToPatienLineRequests = new HashMap<>();
    idToPatientLines.forEach((requisitionId, lineItems) -> {
      List<PatientGroupDto> patientGroupDtos = patientLineItemMapper.from(lineItems);
      List<PatientLineItemsRequest> list = patientGroupDtos.stream()
          .filter(t -> !StringUtils.isEmpty(PatientLineItemName.findByValue(t.getName())))
          .map(this::buildPatientLineItemRequest)
          .collect(Collectors.toList());
      idToPatienLineRequests.put(requisitionId, dealDispensedPatientLineRequest(list));
    });
    return idToPatienLineRequests;
  }

  private PatientLineItemsRequest buildPatientLineItemRequest(PatientGroupDto patientGroupDto) {
    List<PatientLineItemColumnRequest> columns = newArrayList();
    String name = PatientLineItemName.findByValue(patientGroupDto.getName());
    Map<String, PatientColumnDto> map = patientGroupDto.getColumns();
    map.forEach((colName, patientColumnDto) -> {
      if (colName.equals(FieldConstants.TOTAL)  && !name.equals(PatientLineItemName.table_prophylaxy_key.name())) {
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

  private List<PatientLineItemsRequest> dealDispensedPatientLineRequest(List<PatientLineItemsRequest> list) {
    List<PatientLineItemsRequest> dealList = list.stream().filter(t -> !t.getName().contains(TABLE_DISPENSED))
        .collect(Collectors.toList());
    dealList.add(mergeDispensedColumns(list));
    return dealList;
  }

  private PatientLineItemsRequest mergeDispensedColumns(List<PatientLineItemsRequest> list) {
    List<PatientLineItemColumnRequest> dispensedColumns = list.stream()
        .filter(t -> t.getName().contains(TABLE_DISPENSED))
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
    if (tableName.equals(PatientLineItemName.table_arvt_key.name())) {
      return PatientType.findByValue(columnName);
    } else if (tableName.equals(PatientLineItemName.table_patients_key.name())) {
      return NewSection0.findByValue(columnName);
    } else if (tableName.equals(PatientLineItemName.table_prophylaxy_key.name())) {
      return NewSection1.findByValue(columnName);
    } else if (tableName.equals(PatientLineItemName.table_dispensed_ds_key.name())) {
      return NewSection2.findByValue(columnName);
    } else if (tableName.equals(PatientLineItemName.table_dispensed_dt_key.name())) {
      return NewSection3.findByValue(columnName);
    } else if (tableName.equals(PatientLineItemName.table_dispensed_dm_key.name())) {
      return NewSection4.findByValue(columnName);
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
          .build();
      requisitionLineItemRequestList.add(lineItemRequest);

    });
    return requisitionLineItemRequestList;
  }

  private Requisition initiateRequisition(RequisitionCreateRequest request, UUID homeFacilityId, UUID programId,
      UUID authorId) {
    checkPermission(() -> permissionService.canInitRequisition(programId, homeFacilityId));
    Requisition newRequisition = RequisitionBuilder.newRequisition(homeFacilityId, programId, request.getEmergency());
    newRequisition.setTemplate(getRequisitionTemplate());
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newRequisition.setProcessingPeriodId(getPeriodId(request));
    newRequisition.setReportOnly(false);
    newRequisition.setNumberOfMonthsInPeriod(1);
    buildStatusChanges(newRequisition, authorId);
    buildRequisitionApprovedProduct(newRequisition, homeFacilityId, programId);
    buildRequisitionExtraData(newRequisition, request);
    buildRequisitionLineItems(newRequisition, request);
    log.info("initiate android requisition: {}", newRequisition);
    Requisition requisition = requisitionRepository.save(newRequisition);
    buildRequisitionExtension(requisition, request);
    buildRequisitionLineItemsExtension(requisition, request);
    buildRequisitionUsageSections(requisition, request);
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

  private RequisitionTemplate getRequisitionTemplate() {
    RequisitionTemplate requisitionTemplate = requisitionTemplateService.findTemplateById(androidViaTemplateId);
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

  private void buildRequisitionApprovedProduct(Requisition requisition, UUID homeFacilityId, UUID programId) {
    ApproveProductsAggregator approvedProductsContainKit = requisitionService
        .getApproveProduct(homeFacilityId, programId, false);
    List<ApprovedProductDto> approvedProductDtos = approvedProductsContainKit.getFullSupplyProducts();
    ApproveProductsAggregator approvedProducts = new ApproveProductsAggregator(approvedProductDtos, programId);
    Set<ApprovedProductReference> availableProductIdentities = approvedProducts.getApprovedProductReferences();
    requisition.setAvailableProducts(availableProductIdentities);
  }

  private void buildRequisitionUsageSections(Requisition requisition, RequisitionCreateRequest request) {
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
    siglusUsageReportService.saveUsageReport(requisitionDto, dto);
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
        .filter(product -> CHW_KIT_CODE.equals(product.getProductCode()))
        .mapToInt(RequisitionLineItemRequest::getTotalReceivedQuantity)
        .sum();
    int kitReceivedHf = request.getProducts().stream()
        .filter(product -> HF_KIT_CODE.equals(product.getProductCode()))
        .mapToInt(RequisitionLineItemRequest::getTotalReceivedQuantity)
        .sum();
    int kitOpenedChw = request.getProducts().stream()
        .filter(product -> CHW_KIT_CODE.equals(product.getProductCode()))
        .mapToInt(RequisitionLineItemRequest::getTotalConsumedQuantity)
        .sum();
    int kitOpenedHf = request.getProducts().stream()
        .filter(product -> HF_KIT_CODE.equals(product.getProductCode()))
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
