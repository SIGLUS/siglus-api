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
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ExtraDataConstants.APPROVE;
import static org.siglus.common.constant.ExtraDataConstants.AUTHORIZE;
import static org.siglus.common.constant.ExtraDataConstants.CLIENT_SUBMITTED_TIME;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;
import static org.siglus.common.constant.ExtraDataConstants.SUBMIT;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.COLUMN_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.GROUP_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_ARVT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DM_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_DT_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_DISPENSED_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_PATIENTS_KEY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.PatientLineItems.TABLE_PROPHYLAXY_KEY;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem.Importer;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.siglus.siglusapi.config.AndroidTemplateConfigProperties;
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
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.android.androidenum.NewSection0;
import org.siglus.siglusapi.dto.android.androidenum.NewSection1;
import org.siglus.siglusapi.dto.android.androidenum.NewSection2;
import org.siglus.siglusapi.dto.android.androidenum.NewSection3;
import org.siglus.siglusapi.dto.android.androidenum.NewSection4;
import org.siglus.siglusapi.dto.android.androidenum.PatientLineItemName;
import org.siglus.siglusapi.dto.android.androidenum.PatientType;
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
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.PatientLineItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class AndroidGetRequisitionService {
  private final SiglusProgramService siglusProgramService;
  private final SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;
  private final RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final ConsultationNumberDataProcessor consultationNumberDataProcessor;
  private final AndroidTemplateConfigProperties androidTemplateConfigProperties;
  private final RegimenLineItemRepository regimenLineItemRepository;
  private final RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;
  private final RegimenRepository regimenRepository;
  private final PatientLineItemRepository patientLineItemRepository;
  private final PatientLineItemMapper patientLineItemMapper;

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
    Set<UUID> androidTemplateSet = androidTemplateConfigProperties.getAndroidTemplateIds();
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
}

