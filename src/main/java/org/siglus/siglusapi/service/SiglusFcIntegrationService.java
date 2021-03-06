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
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.domain.referencedata.Facility;
import org.siglus.common.domain.referencedata.SupervisoryNode;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.ProgramOrderablesExtension;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.FcProofOfDeliveryDto;
import org.siglus.siglusapi.dto.FcProofOfDeliveryProductDto;
import org.siglus.siglusapi.dto.FcRequisitionDto;
import org.siglus.siglusapi.dto.FcRequisitionLineItemDto;
import org.siglus.siglusapi.dto.ProofOfDeliverParameter;
import org.siglus.siglusapi.dto.RealProgramDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusFcIntegrationService {

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Autowired
  private ProgramRealProgramRepository programRealProgramRepository;

  @Autowired
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Autowired
  private RequisitionLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Autowired
  private SiglusUsageReportService siglusUsageReportService;

  @Autowired
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Autowired
  private SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  @Autowired
  private SiglusLotReferenceDataService siglusLotReferenceDataService;

  @Autowired
  private StockCardLineItemReasonRepository stockCardLineItemReasonRepository;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  @Autowired
  private SiglusDateHelper dateHelper;

  @Autowired
  private PodExtensionRepository podExtensionRepository;

  @Value("${dpm.facilityTypeId}")
  private UUID dpmFacilityTypeId;

  @Value("${dpm.facilityTypeCode}")
  private String dpmFacilityTypeCode;

  @Value("${fc.facilityTypeId}")
  private UUID fcFacilityTypeId;

  private final Map<String, String> fcMaps = ImmutableMap.of("Farmácia Comunitária",
      "comunitaryPharmacy", "Total Pacientes", "patientsOnTreatment");

  public Page<FcRequisitionDto> searchRequisitions(LocalDate date, Pageable pageable) {
    Set<UUID> dpmSupervisoryNodeIds = supervisoryNodeRepository
        .findAllByFacilityTypeId(dpmFacilityTypeId).stream().map(SupervisoryNode::getId)
        .collect(toSet());
    Set<UUID> fcSupervisoryNodeIds = supervisoryNodeRepository
        .findAllByFacilityTypeId(fcFacilityTypeId).stream().map(SupervisoryNode::getId)
        .collect(toSet());
    Page<Requisition> requisitions;
    String today = dateHelper.getTodayDateStr();
    if (fcSupervisoryNodeIds.isEmpty()) {
      requisitions = siglusRequisitionRepository.searchForFc(date, today, dpmSupervisoryNodeIds,
          pageable);
    } else {
      requisitions = siglusRequisitionRepository.searchForFc(date, today, dpmSupervisoryNodeIds,
          fcSupervisoryNodeIds, pageable);
    }
    List<FcRequisitionDto> fcRequisitionDtos = newArrayList();
    Map<UUID, ProgramRealProgram> realProgramIdToEntityMap = programRealProgramRepository.findAll()
        .stream().collect(Collectors.toMap(ProgramRealProgram::getId, Function.identity()));
    requisitions.getContent().forEach(requisition -> fcRequisitionDtos.add(buildDto(requisition,
        fcSupervisoryNodeIds, realProgramIdToEntityMap)));
    return Pagination.getPage(fcRequisitionDtos, pageable, requisitions.getTotalElements());
  }

  public Page<FcProofOfDeliveryDto> searchProofOfDelivery(LocalDate date, Pageable pageable) {
    List<FacilityDto> facilityDtos = siglusFacilityReferenceDataService.findAll();
    Set<UUID> dpmRequestingFacilityIds = facilityDtos
        .stream()
        .filter(facilityDto -> dpmFacilityTypeCode.equals(facilityDto.getType().getCode()))
        .map(FacilityDto::getId)
        .collect(toSet());
    Map<UUID, String> facilityIdTofacilityCodeMap = facilityDtos
        .stream()
        .collect(toMap(FacilityDto::getId, FacilityDto::getCode));

    Page<ProofOfDelivery> page = siglusProofOfDeliveryRepository
        .search(date, dateHelper.getTodayDateStr(), dpmRequestingFacilityIds, pageable);

    Set<UUID> shipmentIds = page.getContent().stream()
        .map(ProofOfDelivery::getShipment).map(Shipment::getId).collect(toSet());
    Map<UUID, PodExtension> shipmenIdToPodExtensionMap =
        podExtensionRepository.findByShipmentIdIn(shipmentIds).stream()
            .collect(toMap(PodExtension::getShipmentId, Function.identity()));

    Set<UUID> externalIds = page.getContent()
        .stream()
        .map(pod -> pod.getShipment().getOrder().getExternalId())
        .collect(toSet());

    List<OrderExternal> externals = orderExternalRepository.findByIdIn(externalIds);
    Set<UUID> requisitionIds = new HashSet<>();

    // podId: requisitionId
    Map<UUID, UUID> podIdToRequisitionIdMap = page.getContent().stream().collect(toMap(
        ProofOfDelivery::getId,
        p -> {
          OrderExternal orderExternal = externals.stream()
              .filter(external ->
                  external.getId().equals(p.getShipment().getOrder().getExternalId()))
              .findAny().orElse(null);

          UUID requisitionId;

          requisitionId = orderExternal == null
              ? p.getShipment().getOrder().getExternalId() :
              orderExternal.getRequisitionId();
          requisitionIds.add(requisitionId);
          return requisitionId;
        }
    ));

    // requisitionId: requisitionNumber
    Map<UUID, String> requisitionIdToRequisitionNumberMap =
        siglusRequisitionExtensionService.getRequisitionNumbers(requisitionIds);

    Set<UUID> orderableIds = page.getContent()
        .stream()
        .flatMap(pod -> pod.getLineItems()
            .stream().map(lineItem -> lineItem.getOrderable().getId()))
        .collect(toSet());
    Map<UUID, OrderableDto> orderableIdToOrderableMap = orderableReferenceDataService
        .findByIds(orderableIds)
        .stream()
        .collect(toMap(OrderableDto::getId, Function.identity()));
    Map<UUID, ProgramOrderablesExtension> orderableIdToProgramMap =
        programOrderablesExtensionRepository.findAllByOrderableIdIn(orderableIds)
            .stream()
            .collect(toMap(ProgramOrderablesExtension::getOrderableId, Function.identity()));
    Map<UUID, LotDto> lotIdToLotMap = siglusLotReferenceDataService.findAll()
        .stream().collect(toMap(LotDto::getId, Function.identity()));
    Map<UUID, String> reasonIdToReasonMap = stockCardLineItemReasonRepository
        .findByReasonTypeIn(newArrayList(ReasonType.DEBIT))
        .stream().collect(toMap(StockCardLineItemReason::getId, StockCardLineItemReason::getName));

    ProofOfDeliverParameter proofOfDeliverParameter = ProofOfDeliverParameter.builder()
        .requisitionIdToRequisitionNumberMap(requisitionIdToRequisitionNumberMap)
        .orderableIdToOrderableMap(orderableIdToOrderableMap)
        .orderableIdToProgramMap(orderableIdToProgramMap)
        .lotIdToLotMap(lotIdToLotMap)
        .reasonIdToReasonMap(reasonIdToReasonMap)
        .podIdToRequisitionIdMap(podIdToRequisitionIdMap)
        .facilityIdTofacilityCodeMap(facilityIdTofacilityCodeMap)
        .shipmenIdToPodExtensionMap(shipmenIdToPodExtensionMap)
        .build();

    List<FcProofOfDeliveryDto> pods = page.getContent()
        .stream()
        .map(pod -> buildProofOfDeliveryDto(pod, proofOfDeliverParameter))
        .collect(Collectors.toList());

    return Pagination.getPage(pods, pageable, page.getTotalElements());
  }

  private FcProofOfDeliveryDto buildProofOfDeliveryDto(ProofOfDelivery pod,
      ProofOfDeliverParameter proofOfDeliverParameter) {

    String requisitionNumber = proofOfDeliverParameter.getRequisitionIdToRequisitionNumberMap()
        .get(proofOfDeliverParameter.getPodIdToRequisitionIdMap().get(pod.getId()));

    PodExtension podExtension = proofOfDeliverParameter.getShipmenIdToPodExtensionMap()
        .get(pod.getShipment().getId());

    List<FcProofOfDeliveryProductDto> products = pod.getLineItems()
        .stream()
        .map(lineItem -> buildProductDto(lineItem,
            proofOfDeliverParameter.getOrderableIdToOrderableMap(),
            proofOfDeliverParameter.getOrderableIdToProgramMap(),
            proofOfDeliverParameter.getLotIdToLotMap(),
            proofOfDeliverParameter.getReasonIdToReasonMap()))
        .collect(Collectors.toList());

    return FcProofOfDeliveryDto.builder()
        .orderNumber(pod.getShipment().getOrder().getOrderCode())
        .facilityCode(proofOfDeliverParameter.getFacilityIdTofacilityCodeMap()
            .get(pod.getShipment().getOrder().getRequestingFacilityId()))
        .issueVoucherNumber(podExtension == null ? null : podExtension.getIssueVoucherNumber())
        .requisitionNumber(requisitionNumber)
        .deliveredBy(pod.getDeliveredBy())
        .receivedBy(pod.getReceivedBy())
        .receivedDate(pod.getReceivedDate())
        .products(products)
        .build();
  }

  private FcProofOfDeliveryProductDto buildProductDto(ProofOfDeliveryLineItem lineItem,
      Map<UUID, OrderableDto> orderableMap,
      Map<UUID, ProgramOrderablesExtension> programMap,
      Map<UUID, LotDto> lotMap,
      Map<UUID, String> reasonMap) {
    OrderableDto orderableDto = orderableMap.get(lineItem.getOrderable().getId());
    ProgramOrderablesExtension extension = programMap.get(orderableDto.getId());
    RealProgramDto program =
        new RealProgramDto(extension.getRealProgramCode(), extension.getRealProgramName());

    return FcProofOfDeliveryProductDto.builder()
        .productCode(orderableDto.getProductCode())
        .productName(orderableDto.getFullProductName())
        .productDescription(orderableDto.getDescription())
        .realPorgrams(newArrayList(program))
        .lotCode(lotMap.get(lineItem.getLotId()).getLotCode())
        .acceptedQuantity(lineItem.getQuantityAccepted())
        .rejectedQuantity(lineItem.getQuantityRejected())
        .rejectedReason(reasonMap.get(lineItem.getRejectionReasonId()))
        .notes(lineItem.getNotes())
        .build();

  }


  private FcRequisitionDto buildDto(Requisition requisition, Set<UUID> fcSupervisoryNodeIds,
      Map<UUID, ProgramRealProgram> realProgramMap) {
    FcRequisitionDto fcRequisitionDto = new FcRequisitionDto();
    BeanUtils.copyProperties(requisition, fcRequisitionDto);
    fcRequisitionDto.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisition.getId()));
    setFacilityInfo(requisition, fcSupervisoryNodeIds, fcRequisitionDto);
    setProgramInfo(requisition.getProgramId(), fcRequisitionDto);
    setPeriodInfo(requisition.getProcessingPeriodId(), fcRequisitionDto);
    setProductInfo(requisition.getNonSkippedRequisitionLineItems(), fcRequisitionDto);
    setRegimenInfo(requisition, fcRequisitionDto, realProgramMap);
    return fcRequisitionDto;
  }

  private void setRegimenInfo(Requisition requisition, FcRequisitionDto fcRequisitionDto,
      Map<UUID, ProgramRealProgram> realProgramMap) {
    RequisitionTemplateExtension templateExtension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisition.getTemplate().getId());
    if (Boolean.FALSE.equals(templateExtension.getEnableRegimen())) {
      return;
    }
    RequisitionV2Dto requisitionDto = siglusRequisitionRequisitionService
        .searchRequisition(requisition.getId());
    SiglusRequisitionDto siglusRequisitionDto = siglusUsageReportService
        .searchUsageReport(requisitionDto);
    List<RegimenLineDto> regimenLineItems = siglusRequisitionDto.getRegimenLineItems();
    Map<String, List<UsageTemplateColumnDto>> usageTemplateMap = siglusRequisitionDto
        .getUsageTemplate()
        .getRegimen()
        .stream()
        .collect(Collectors.toMap(UsageTemplateSectionDto::getName,
            UsageTemplateSectionDto::getColumns));
    Map<String, String> regimenLabelMap = usageTemplateMap.get("regimen").stream().collect(
        Collectors.toMap(UsageTemplateColumnDto::getName, UsageTemplateColumnDto::getLabel));
    List<Map<String, Object>> regimens = newArrayList();
    regimenLineItems.stream()
        .filter(lineItem -> lineItem.getRegimen() != null)
        .forEach(regimenLineItem -> {
          Map<String, Object> regimenMap = newHashMap();
          regimens.add(regimenMap);
          RegimenDto regimenDto = regimenLineItem.getRegimen();
          ProgramRealProgram realProgram = realProgramMap.get(regimenDto.getRealProgramId());
          regimenMap.put("code", regimenDto.getCode());
          regimenMap.put("name", regimenDto.getFullProductName());
          regimenMap.put("programCode", realProgram.getRealProgramCode());
          regimenMap.put("programName", realProgram.getRealProgramName());
          regimenLineItem.getColumns().forEach((key, value) -> {
            String regimenLabel = regimenLabelMap.get(key);
            String regimenFcLabel = fcMaps.getOrDefault(regimenLabel, regimenLabel);
            regimenMap.put(regimenFcLabel, value.getValue());
          });
        });
    fcRequisitionDto.setRegimens(regimens);
  }

  private void setProductInfo(List<RequisitionLineItem> lineItems,
      FcRequisitionDto fcRequisitionDto) {
    Set<UUID> lineItemIds = lineItems.stream().map(RequisitionLineItem::getId)
        .collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(lineItemIds)) {
      List<FcRequisitionLineItemDto> products = newArrayList();
      Map<UUID, Integer> authorizedQuantityMap = lineItemExtensionRepository
          .findLineItems(lineItemIds).stream().collect(toMap(
              RequisitionLineItemExtension::getRequisitionLineItemId, lineItemExtension ->
                  Optional.ofNullable(lineItemExtension.getAuthorizedQuantity()).orElse(0)));
      boolean duringApproval = fcRequisitionDto.getStatus().duringApproval();
      lineItems.forEach(lineItem -> {
        if (duringApproval) {
          lineItem.setApprovedQuantity(null);
        }
        products.add(buildLineItemDto(lineItem, authorizedQuantityMap));
      });
      fcRequisitionDto.setProducts(products);
    }
  }

  private void setPeriodInfo(UUID processingPeriodId, FcRequisitionDto fcRequisitionDto) {
    ProcessingPeriodDto processingPeriodDto = processingPeriodReferenceDataService
        .findOne(processingPeriodId);
    fcRequisitionDto.setPeriodStartDate(processingPeriodDto.getStartDate());
    fcRequisitionDto.setPeriodEndDate(processingPeriodDto.getEndDate());
  }

  private void setProgramInfo(UUID programId, FcRequisitionDto fcRequisitionDto) {
    ProgramDto programDto = programReferenceDataService.findOne(programId);
    fcRequisitionDto.setProgramCode(programDto.getCode());
    fcRequisitionDto.setProgramName(programDto.getName());
  }

  private void setFacilityInfo(Requisition requisition, Set<UUID> fcSupervisoryNodeIds,
      FcRequisitionDto fcRequisitionDto) {
    FacilityDto requestingFacility = siglusFacilityReferenceDataService
        .findOne(requisition.getFacilityId());
    fcRequisitionDto.setRequestingFacilityCode(requestingFacility.getCode());
    fcRequisitionDto.setRequestingFacilityName(requestingFacility.getName());
    fcRequisitionDto.setRequestingFacilityDescription(requestingFacility.getDescription());
    if (fcSupervisoryNodeIds.contains(requisition.getSupervisoryNodeId())) {
      fcRequisitionDto.setFacilityCode(requestingFacility.getCode());
      fcRequisitionDto.setFacilityName(requestingFacility.getName());
      fcRequisitionDto.setFacilityDescription(requestingFacility.getDescription());
    } else {
      Facility supervisoryNodeFacility = requisition.getSupervisoryNode().getFacility();
      fcRequisitionDto.setFacilityCode(supervisoryNodeFacility.getCode());
      fcRequisitionDto.setFacilityName(supervisoryNodeFacility.getName());
      fcRequisitionDto.setFacilityDescription(supervisoryNodeFacility.getDescription());
    }
  }

  private FcRequisitionLineItemDto buildLineItemDto(RequisitionLineItem lineItem,
      Map<UUID, Integer> authorizedQuantityMap) {
    FcRequisitionLineItemDto fcLineItem = new FcRequisitionLineItemDto();
    BeanUtils.copyProperties(lineItem, fcLineItem);
    OrderableDto orderable = orderableReferenceDataService.findOne(lineItem.getOrderable().getId());
    fcLineItem.setProductCode(orderable.getProductCode());
    fcLineItem.setProductName(orderable.getFullProductName());
    fcLineItem.setProductDescription(orderable.getDescription());
    List<RealProgramDto> realPrograms = newArrayList();
    programOrderablesExtensionRepository.findAllByOrderableId(lineItem.getOrderable().getId())
        .forEach(orderablesExtension ->
            realPrograms.add(new RealProgramDto(orderablesExtension.getRealProgramCode(),
                orderablesExtension.getRealProgramName())));
    fcLineItem.setRealPrograms(realPrograms);
    fcLineItem.setAuthorizedQuantity(authorizedQuantityMap.get(lineItem.getId()));
    return fcLineItem;
  }
}
