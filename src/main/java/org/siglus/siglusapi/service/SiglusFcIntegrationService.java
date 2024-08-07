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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.openlmis.requisition.domain.requisition.Requisition.AI;
import static org.openlmis.requisition.domain.requisition.Requisition.DPM;
import static org.openlmis.requisition.domain.requisition.Requisition.HC;
import static org.openlmis.requisition.domain.requisition.Requisition.ODF;
import static org.siglus.common.constant.ProgramConstants.MTB_PROGRAM_CODE;
import static org.siglus.common.constant.ProgramConstants.RAPIDTEST_PROGRAM_CODE;
import static org.siglus.common.constant.ProgramConstants.TARV_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.FieldConstants.VALUE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.TOTAL;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.BaseEntity;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.constant.KitConstants;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.constant.FacilityTypeConstants;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.ShipmentsExtension;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.FcProofOfDeliveryDto;
import org.siglus.siglusapi.dto.FcProofOfDeliveryLineItem;
import org.siglus.siglusapi.dto.FcRequisitionDto;
import org.siglus.siglusapi.dto.FcRequisitionLineItemDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.ProofOfDeliverParameter;
import org.siglus.siglusapi.dto.RealProgramDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.enumeration.MmiaPatientTableColumnKeyValue;
import org.siglus.siglusapi.dto.android.enumeration.MmiaPatientTableKeyValue;
import org.siglus.siglusapi.dto.android.enumeration.MmtbPatientTableColumnKeyValue;
import org.siglus.siglusapi.dto.android.enumeration.MmtbPatientTableKeyValue;
import org.siglus.siglusapi.dto.android.enumeration.TestProject;
import org.siglus.siglusapi.dto.android.enumeration.TestService;
import org.siglus.siglusapi.dto.fc.FacilityStockMovementResponse;
import org.siglus.siglusapi.dto.fc.FacilityStockOnHandResponse;
import org.siglus.siglusapi.dto.fc.ProductStockOnHandResponse;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusProgramOrderableRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.siglus.siglusapi.repository.dto.ProgramOrderableDto;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapper;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.LotOnHandMapper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusFcIntegrationService {

  private final SupervisoryNodeRepository supervisoryNodeRepository;
  private final SiglusRequisitionRepository siglusRequisitionRepository;
  private final ProgramRealProgramRepository programRealProgramRepository;
  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final ProgramReferenceDataService programReferenceDataService;
  private final SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;
  private final SiglusOrderableReferenceDataService orderableReferenceDataService;
  private final ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;
  private final RequisitionLineItemExtensionRepository lineItemExtensionRepository;
  private final SiglusProgramOrderableRepository siglusProgramOrderableRepository;
  private final OrderLineItemExtensionRepository orderLineItemExtensionRepository;
  private final RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;
  private final SiglusUsageReportService siglusUsageReportService;
  private final SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;
  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;
  private final SiglusLotReferenceDataService siglusLotReferenceDataService;
  private final StockCardLineItemReasonRepository stockCardLineItemReasonRepository;
  private final OrderExternalRepository orderExternalRepository;
  private final SiglusDateHelper dateHelper;
  private final ShipmentsExtensionRepository shipmentsExtensionRepository;
  private final FacilityNativeRepository facilityNativeRepository;
  private final SiglusFacilityTypeReferenceDataService facilityTypeDataService;
  private final StockManagementRepository stockManagementRepository;
  private final ProductMovementMapper productMovementMapper;
  private final LotOnHandMapper lotOnHandMapper;

  private final UsageInformationLineItemRepository usageInformationLineItemRepository;

  private final TestConsumptionLineItemRepository testConsumptionLineItemRepository;

  private final PatientLineItemRepository patientLineItemRepository;
  private final SiglusOrderableService siglusOrderableService;

  private final AgeGroupLineItemRepository ageGroupLineItemRepository;

  @Value("${dpm.facilityTypeId}")
  private UUID dpmFacilityTypeId;
  @Value("${fc.facilityTypeId}")
  private UUID fcFacilityTypeId;

  private final Map<String, String> fcMaps = ImmutableMap.of("Farmácia Comunitária",
      "comunitaryPharmacy", "Total doentes", "patientsOnTreatment");

  private Map<UUID, Map<String, String>> orderableIdToInfoMap;

  public Page<FcRequisitionDto> searchRequisitions(LocalDate date, Pageable pageable) {
    Page<Requisition> requisitions;
    requisitions = siglusRequisitionRepository.searchAllForFc(date, pageable);
    List<FcRequisitionDto> fcRequisitionDtos = newArrayList();
    Map<UUID, ProgramRealProgram> realProgramIdToEntityMap = programRealProgramRepository.findAll()
        .stream().collect(Collectors.toMap(ProgramRealProgram::getId, Function.identity()));
    orderableIdToInfoMap = siglusOrderableService.getAllOrderableInfoForFc();
    Set<UUID> fcSupervisoryNodeIds = supervisoryNodeRepository
        .findAllByFacilityTypeId(fcFacilityTypeId).stream().map(SupervisoryNode::getId)
        .collect(toSet());
    requisitions.getContent().forEach(requisition -> fcRequisitionDtos.add(buildDto(requisition,
        fcSupervisoryNodeIds, realProgramIdToEntityMap)));
    return Pagination.getPage(fcRequisitionDtos, pageable, requisitions.getTotalElements());
  }

  public Page<FcRequisitionDto> searchNeedApprovalRequisitions(LocalDate date, Pageable pageable) {
    Page<Requisition> requisitions;
    Set<UUID> fcSupervisoryNodeIds = supervisoryNodeRepository
        .findAllByFacilityTypeId(fcFacilityTypeId).stream().map(SupervisoryNode::getId)
        .collect(toSet());
    requisitions = siglusRequisitionRepository.searchNeedApprovalForFc(date, pageable, fcSupervisoryNodeIds);
    List<FcRequisitionDto> fcRequisitionDtos = newArrayList();
    Map<UUID, ProgramRealProgram> realProgramIdToEntityMap = programRealProgramRepository.findAll()
        .stream().collect(Collectors.toMap(ProgramRealProgram::getId, Function.identity()));
    orderableIdToInfoMap = siglusOrderableService.getAllOrderableInfoForFc();

    requisitions.getContent().forEach(requisition -> fcRequisitionDtos.add(buildDto(requisition,
        fcSupervisoryNodeIds, realProgramIdToEntityMap)));
    return Pagination.getPage(fcRequisitionDtos, pageable, requisitions.getTotalElements());
  }

  public Page<FacilityStockMovementResponse> searchStockMovements(LocalDate since, Pageable pageable) {
    List<UUID> excludedTypeIds = findFacilityTypes(FacilityTypeConstants.getVirtualFacilityTypes()).stream()
        .map(FacilityTypeDto::getId).collect(toList());
    return facilityNativeRepository.findAllForStockMovements(excludedTypeIds, since, pageable)
        .map(f -> toMovementResponse(f, since));
  }

  public Page<FacilityStockOnHandResponse> searchStockOnHand(LocalDate at, Pageable pageable) {
    List<UUID> excludedTypeIds = findFacilityTypes(FacilityTypeConstants.getVirtualFacilityTypes()).stream()
        .map(FacilityTypeDto::getId).collect(toList());
    return facilityNativeRepository.findAllForStockOnHand(excludedTypeIds, at, pageable)
        .map(f -> toStockOnHandResponse(f, at));
  }

  private FacilityStockMovementResponse toMovementResponse(org.siglus.siglusapi.dto.android.db.Facility facility,
      LocalDate since) {
    FacilityStockMovementResponse response = new FacilityStockMovementResponse();
    response.setCode(facility.getCode());
    response.setName(facility.getName());
    PeriodOfProductMovements period = stockManagementRepository.getAllProductMovementsForSync(facility.getId(), since);
    response.setProductMovements(productMovementMapper.toResponses(period));
    return response;
  }

  private FacilityStockOnHandResponse toStockOnHandResponse(org.siglus.siglusapi.dto.android.db.Facility facility,
      LocalDate at) {
    FacilityStockOnHandResponse response = new FacilityStockOnHandResponse();
    response.setCode(facility.getCode());
    response.setName(facility.getName());
    log.info("toStockOnHandResponse facility id: {}", facility.getId());
    StocksOnHand stocksOnHand = stockManagementRepository.getStockOnHand(facility.getId(), at);
    List<ProductStockOnHandResponse> products = lotOnHandMapper.toResponses(stocksOnHand).stream()
        .filter(resp -> !resp.getLots().isEmpty() || KitConstants.isKit(resp.getProductCode()))
        .collect(toList());
    response.setProducts(products);
    return response;
  }

  public Page<FcProofOfDeliveryDto> searchProofOfDelivery(LocalDate date, Pageable pageable) {
    List<FacilityDto> facilityDtos = siglusFacilityReferenceDataService.findAll();
    Map<UUID, String> facilityIdToFacilityCodeMap = facilityDtos
        .stream()
        .collect(toMap(FacilityDto::getId, FacilityDto::getCode));

    Page<ProofOfDelivery> page = siglusProofOfDeliveryRepository
        .search(date, pageable);

    Set<UUID> shipmentIds = page.getContent().stream()
        .map(ProofOfDelivery::getShipment).map(Shipment::getId).collect(toSet());
    Map<UUID, ShipmentsExtension> shipmenIdToShipmentsExtensionMap =
        shipmentsExtensionRepository.findByShipmentIdIn(shipmentIds).stream()
            .collect(toMap(ShipmentsExtension::getShipmentId, Function.identity()));

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

    Set<UUID> lotIds = page.getContent()
        .stream()
        .flatMap(pod -> pod.getLineItems()
            .stream().map(ProofOfDeliveryLineItem::getLotId))
        .collect(toSet());
    log.info("lotIds size: {}", lotIds.size());
    Map<UUID, LotDto> lotIdToLotMap = siglusLotReferenceDataService.findByIds(lotIds)
        .stream().collect(toMap(LotDto::getId, Function.identity()));
    Map<UUID, String> reasonIdToReasonMap = stockCardLineItemReasonRepository
        .findByReasonTypeIn(newArrayList(ReasonType.DEBIT))
        .stream().collect(toMap(StockCardLineItemReason::getId, StockCardLineItemReason::getName));
    Map<UUID, BigDecimal> productIdToPriceMap = siglusProgramOrderableRepository.findAllMaxVersionProgramOrderableDtos()
        .stream()
        .filter(programOrderableDto -> programOrderableDto.getPrice() != null)
        .collect(toMap(ProgramOrderableDto::getOrderableId, ProgramOrderableDto::getPrice));

    ProofOfDeliverParameter proofOfDeliverParameter = ProofOfDeliverParameter.builder()
        .requisitionIdToRequisitionNumberMap(requisitionIdToRequisitionNumberMap)
        .orderableIdToOrderableMap(orderableIdToOrderableMap)
        .lotIdToLotMap(lotIdToLotMap)
        .reasonIdToReasonMap(reasonIdToReasonMap)
        .podIdToRequisitionIdMap(podIdToRequisitionIdMap)
        .facilityIdTofacilityCodeMap(facilityIdToFacilityCodeMap)
        .shipmenIdToShipmentsExtensionMap(shipmenIdToShipmentsExtensionMap)
        .productIdToPriceMap(productIdToPriceMap)
        .build();

    List<FcProofOfDeliveryDto> pods = page.getContent()
        .stream()
        .map(pod -> buildProofOfDeliveryDto(pod, proofOfDeliverParameter))
        .collect(toList());

    return Pagination.getPage(pods, pageable, page.getTotalElements());
  }

  private FcProofOfDeliveryDto buildProofOfDeliveryDto(ProofOfDelivery pod,
      ProofOfDeliverParameter proofOfDeliverParameter) {

    Map<UUID, Long> productIdToOrderedQuantityMap = newHashMap();

    pod.getShipment().getOrder().getOrderLineItems()
        .forEach(orderLineItem -> productIdToOrderedQuantityMap.put(orderLineItem.getOrderable().getId(),
            orderLineItem.getOrderedQuantity()));

    Set<UUID> orderLineItemIds = pod.getShipment().getOrder().getOrderLineItems()
        .stream()
        .map(BaseEntity::getId)
        .collect(toSet());

    Map<UUID, Long> orderLineItemIdToPartialFulfilledMap = orderLineItemExtensionRepository.findByOrderLineItemIdIn(
        orderLineItemIds).stream().collect(
        toMap(OrderLineItemExtension::getOrderLineItemId, OrderLineItemExtension::getPartialFulfilledQuantity));

    Map<UUID, Long> productIdToPartialFulfilledMap = newHashMap();
    pod.getShipment().getOrder().getOrderLineItems()
        .forEach(orderLineItem -> productIdToPartialFulfilledMap.put(orderLineItem.getOrderable().getId(),
            orderLineItemIdToPartialFulfilledMap.get(orderLineItem.getId())));

    String requisitionNumber = proofOfDeliverParameter.getRequisitionIdToRequisitionNumberMap()
        .get(proofOfDeliverParameter.getPodIdToRequisitionIdMap().get(pod.getId()));

    ShipmentsExtension shipmentsExtension = proofOfDeliverParameter.getShipmenIdToShipmentsExtensionMap()
        .get(pod.getShipment().getId());

    List<FcProofOfDeliveryLineItem> products = pod.getLineItems()
        .stream()
        .map(lineItem -> buildProductDto(lineItem,
            proofOfDeliverParameter.getOrderableIdToOrderableMap(),
            proofOfDeliverParameter.getLotIdToLotMap(),
            proofOfDeliverParameter.getReasonIdToReasonMap(),
            productIdToOrderedQuantityMap,
            productIdToPartialFulfilledMap,
            proofOfDeliverParameter.getProductIdToPriceMap()))
        .collect(toList());

    return FcProofOfDeliveryDto.builder()
        .orderNumber(pod.getShipment().getOrder().getOrderCode())
        .facilityCode(proofOfDeliverParameter.getFacilityIdTofacilityCodeMap()
            .get(pod.getShipment().getOrder().getRequestingFacilityId()))
        .issueVoucherNumber(shipmentsExtension == null ? null : shipmentsExtension.getIssueVoucherNumber())
        .requisitionNumber(requisitionNumber)
        .deliveredBy(pod.getDeliveredBy())
        .receivedBy(pod.getReceivedBy())
        .receivedDate(pod.getReceivedDate())
        .podLineItems(products)
        .build();
  }

  private FcProofOfDeliveryLineItem buildProductDto(ProofOfDeliveryLineItem lineItem,
      Map<UUID, OrderableDto> orderableMap,
      Map<UUID, LotDto> lotMap,
      Map<UUID, String> reasonMap,
      Map<UUID, Long> productIdToOrderedQuantityMap,
      Map<UUID, Long> productIdToPartialFulfilledMap,
      Map<UUID, BigDecimal> productIdToPriceMap) {
    OrderableDto orderableDto = orderableMap.get(lineItem.getOrderable().getId());

    return FcProofOfDeliveryLineItem.builder()
        .productCode(orderableDto.getProductCode())
        .productName(orderableDto.getFullProductName())
        .productDescription(orderableDto.getDescription())
        .lotCode(lotMap.get(lineItem.getLotId()).getLotCode())
        .expiringDate(lotMap.get(lineItem.getLotId()).getExpirationDate())
        .orderedQuantity(productIdToOrderedQuantityMap.get(orderableDto.getId()))
        .partialFulfilled(productIdToPartialFulfilledMap.get(orderableDto.getId()))
        .suppliedQuantity(lineItem.getQuantityAccepted() + lineItem.getQuantityRejected())
        .price(getPrice(orderableDto, productIdToPriceMap))
        .value(calculateValue(lineItem, getPrice(orderableDto, productIdToPriceMap)))
        .receivedQuantity(lineItem.getQuantityAccepted())
        .difference(lineItem.getQuantityRejected())
        .adjustmentReason(reasonMap.get(lineItem.getRejectionReasonId()))
        .notes(lineItem.getNotes())
        .build();

  }

  private Double getPrice(OrderableDto orderableDto, Map<UUID, BigDecimal> productIdToPriceMap) {
    return productIdToPriceMap.get(orderableDto.getId()) == null ? null
        : productIdToPriceMap.get(orderableDto.getId()).doubleValue();
  }

  private Double calculateValue(ProofOfDeliveryLineItem lineItem, Double price) {
    return price == null ? null : (lineItem.getQuantityAccepted() + lineItem.getQuantityRejected()) * price;
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
    setUsageInformation(requisition, fcRequisitionDto);
    setTestConsumption(requisition, fcRequisitionDto);
    setPatientInfo(requisition, fcRequisitionDto);
    return fcRequisitionDto;
  }


  private void setPatientInfo(Requisition requisition, FcRequisitionDto fcRequisitionDto) {
    if (ProgramConstants.TARV_PROGRAM_CODE.equals(fcRequisitionDto.getProgramCode())) {
      List<PatientLineItem> lineItems = patientLineItemRepository.findByRequisitionId(requisition.getId());
      List<Map<String, Object>> patientLineItems = newArrayList();
      lineItems.stream()
          .filter(lineItem -> !StringUtils.isEmpty(MmiaPatientTableKeyValue.findKeyByValue(lineItem.getGroup()))
              && !StringUtils.isEmpty(MmiaPatientTableColumnKeyValue.valueOf(lineItem.getGroup().toUpperCase())
              .findKeyByValue(lineItem.getColumn())))
          .forEach(lineItem -> {
            Map<String, Object> patientInfoMap = newHashMap();
            patientInfoMap.put("groupName",
                removePrefixAndSuffix(MmiaPatientTableKeyValue.findKeyByValue(lineItem.getGroup())));
            patientInfoMap.put("columnName", removePrefixAndSuffix(
                MmiaPatientTableColumnKeyValue.valueOf(lineItem.getGroup().toUpperCase())
                    .findKeyByValue(lineItem.getColumn())));
            patientInfoMap.put(VALUE, lineItem.getValue());
            patientLineItems.add(patientInfoMap);
          });
      fcRequisitionDto.setPatientLineItems(patientLineItems);
    } else if (ProgramConstants.MTB_PROGRAM_CODE.equals(fcRequisitionDto.getProgramCode())) {

      List<PatientLineItem> lineItems = patientLineItemRepository.findByRequisitionId(requisition.getId());
      List<Map<String, Object>> patientLineItems = newArrayList();
      lineItems.stream()
          .filter(lineItem -> !StringUtils.isEmpty(MmtbPatientTableKeyValue.findKeyByValue(lineItem.getGroup()))
              && !StringUtils.isEmpty(MmtbPatientTableColumnKeyValue.valueOf(lineItem.getGroup().toUpperCase())
              .findKeyByValue(lineItem.getColumn())))
          .forEach(lineItem -> {
            Map<String, Object> patientInfoMap = newHashMap();
            patientInfoMap.put("groupName",
                removePrefixAndSuffix(MmtbPatientTableKeyValue.findKeyByValue(lineItem.getGroup())));
            patientInfoMap.put("columnName", removePrefixAndSuffix(
                MmtbPatientTableColumnKeyValue.valueOf(lineItem.getGroup().toUpperCase())
                    .findKeyByValue(lineItem.getColumn())));
            patientInfoMap.put(VALUE, lineItem.getValue());
            patientLineItems.add(patientInfoMap);
          });

      List<AgeGroupLineItem> ageGroupLineItems = ageGroupLineItemRepository.findByRequisitionId(
          requisition.getId());
      ageGroupLineItems.forEach(lineItem -> {
        Map<String, Object> patientInfoMap = newHashMap();
        patientInfoMap.put("groupName", "age_" + lineItem.getGroup());
        patientInfoMap.put("columnName", lineItem.getService());
        patientInfoMap.put(VALUE, lineItem.getValue());
        patientLineItems.add(patientInfoMap);
        fcRequisitionDto.setPatientLineItems(patientLineItems);
      });
    }
  }

  private void setTestConsumption(Requisition requisition, FcRequisitionDto fcRequisitionDto) {
    if (ProgramConstants.RAPIDTEST_PROGRAM_CODE.equals(fcRequisitionDto.getProgramCode())) {
      List<TestConsumptionLineItem> lineItems = testConsumptionLineItemRepository.findByRequisitionId(
          requisition.getId());
      List<Map<String, Object>> testConsumptionLineItems = newArrayList();
      lineItems.forEach(lineItem -> {
        Map<String, Object> testConsumptionMap = newHashMap();
        testConsumptionMap.put("project", TestProject.findByValue(lineItem.getProject()));
        testConsumptionMap.put("outcome", lineItem.getOutcome().toUpperCase());
        testConsumptionMap.put("service", TOTAL.equals(lineItem.getService()) ? "TOTAL" :
            TestService.findByValue(lineItem.getService()));
        testConsumptionMap.put(VALUE, lineItem.getValue());
        testConsumptionLineItems.add(testConsumptionMap);
      });
      fcRequisitionDto.setTestConsumptionLineItems(testConsumptionLineItems);
    }
  }

  private void setUsageInformation(Requisition requisition, FcRequisitionDto fcRequisitionDto) {
    if (ProgramConstants.MALARIA_PROGRAM_CODE.equals(fcRequisitionDto.getProgramCode())) {
      List<UsageInformationLineItem> lineItems = usageInformationLineItemRepository.findByRequisitionId(
          requisition.getId());
      List<Map<String, Object>> usageInformationLineItems = newArrayList();
      lineItems.forEach(lineItem -> {
        Map<String, Object> usageInformationMap = newHashMap();
        usageInformationMap.put("information", lineItem.getInformation());
        usageInformationMap.put("productCode", orderableIdToInfoMap.get(lineItem.getOrderableId())
            .get("code"));
        usageInformationMap.put("service",
            "newColumn0".equals(lineItem.getService()) ? "CHW" : lineItem.getService());
        usageInformationMap.put(VALUE, lineItem.getValue());
        usageInformationLineItems.add(usageInformationMap);
      });
      fcRequisitionDto.setUsageInformationLineItems(usageInformationLineItems);
    }
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

  private boolean shouldSwapRequestedQuantity(FcRequisitionDto fcRequisitionDto) {
    String facilityTypeCode = fcRequisitionDto.getRequestingFacilityType();
    String programCode = fcRequisitionDto.getProgramCode();
    return Arrays.asList(DPM, AI, HC, ODF).contains(facilityTypeCode)
        && Arrays.asList(MTB_PROGRAM_CODE, TARV_PROGRAM_CODE, RAPIDTEST_PROGRAM_CODE).contains(programCode);
  }

  private void setProductInfo(List<RequisitionLineItem> lineItems,
      FcRequisitionDto fcRequisitionDto) {
    Set<UUID> lineItemIds = lineItems.stream().map(RequisitionLineItem::getId)
        .collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(lineItemIds)) {
      List<FcRequisitionLineItemDto> products = newArrayList();
      List<RequisitionLineItemExtension> extensions = lineItemExtensionRepository.findLineItems(lineItemIds);
      Map<UUID, Integer> authorizedQuantityMap = extensions.stream().collect(toMap(
              RequisitionLineItemExtension::getRequisitionLineItemId, lineItemExtension ->
                  Optional.ofNullable(lineItemExtension.getAuthorizedQuantity()).orElse(0)));
      Map<UUID, Integer> requestedQuantityMap = extensions.stream().collect(toMap(
          RequisitionLineItemExtension::getRequisitionLineItemId, lineItemExtension ->
              Optional.ofNullable(lineItemExtension.getSuggestedQuantity()).orElse(0)));
      boolean duringApproval = fcRequisitionDto.getStatus().duringApproval();
      lineItems.forEach(lineItem -> {
        if (duringApproval) {
          lineItem.setApprovedQuantity(null);
        }
        products.add(buildLineItemDto(lineItem, authorizedQuantityMap, requestedQuantityMap, fcRequisitionDto));
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
    fcRequisitionDto.setRequestingFacilityType(requestingFacility.getType().getCode());
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
      Map<UUID, Integer> authorizedQuantityMap,
      Map<UUID, Integer> requestedQuantityMap,
      FcRequisitionDto fcRequisitionDto) {
    FcRequisitionLineItemDto fcLineItem = new FcRequisitionLineItemDto();
    BeanUtils.copyProperties(lineItem, fcLineItem);
    if (lineItem.getTotalLossesAndAdjustments() == null) {
      fcLineItem.setTotalLossesAndAdjustments(0);
    }
    fcLineItem.setProductCode(orderableIdToInfoMap.get(lineItem.getOrderable().getId()).get("code"));
    fcLineItem.setProductName(orderableIdToInfoMap.get(lineItem.getOrderable().getId()).get("name"));
    fcLineItem.setProductDescription(orderableIdToInfoMap.get(lineItem.getOrderable().getId()).get("description"));
    List<RealProgramDto> realPrograms = newArrayList();
    programOrderablesExtensionRepository.findAllByOrderableId(lineItem.getOrderable().getId())
        .forEach(orderablesExtension ->
            realPrograms.add(new RealProgramDto(orderablesExtension.getRealProgramCode(),
                orderablesExtension.getRealProgramName())));
    fcLineItem.setRealPrograms(realPrograms);
    fcLineItem.setAuthorizedQuantity(authorizedQuantityMap.get(lineItem.getId()));
    if (shouldSwapRequestedQuantity(fcRequisitionDto)) {
      Integer requestedQuantity = requestedQuantityMap.get(lineItem.getId());
      fcLineItem.setRequestedQuantity(requestedQuantity == null ? 0 : requestedQuantity);
    } else {
      fcLineItem.setRequestedQuantity(fcLineItem.getRequestedQuantity() == null
          ? 0 : fcLineItem.getRequestedQuantity());
    }
    return fcLineItem;
  }

  private List<FacilityTypeDto> findFacilityTypes(Collection<String> typeNames) {
    return facilityTypeDataService.getPage(RequestParameters.init()).getContent().stream()
        .filter(t -> typeNames.contains(t.getCode())).collect(toList());
  }

  private String removePrefixAndSuffix(String s) {
    String temp = removeStart(s, "table_");
    return removeEnd(temp, "_key");
  }
}
